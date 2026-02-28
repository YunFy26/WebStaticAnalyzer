// File: MyBatisXMLParser.java
package org.example.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Parses MyBatis XML mapper files to extract:
 * 1. Sink metadata (SQL operations with variable references)
 * 2. Write operation field names for POJO source filtering (F_write)
 */
public class MyBatisXMLParser {

    private static final Logger logger = LogManager.getLogger(MyBatisXMLParser.class);

    private static final Pattern VAR_PATTERN =
        Pattern.compile("([#$])\\{([^}]+?)\\}");

    private static final Set<String> DML_TAGS =
        Set.of("select", "insert", "update", "delete");
    private static final Set<String> WRITE_TAGS =
        Set.of("insert", "update");

    public static class MapperParseResult {
        public final String namespace;
        public final List<SqlOperation> operations;

        public MapperParseResult(String namespace, List<SqlOperation> operations) {
            this.namespace = namespace;
            this.operations = operations;
        }
    }

    public static class SqlOperation {
        public final String id;
        public final String operationType;
        public final String sqlFragment;
        public final List<VarReference> variables;

        public SqlOperation(String id, String operationType,
                            String sqlFragment, List<VarReference> variables) {
            this.id = id;
            this.operationType = operationType;
            this.sqlFragment = sqlFragment;
            this.variables = variables;
        }

        public boolean isWriteOperation() {
            return WRITE_TAGS.contains(operationType.toLowerCase());
        }
    }

    public static class VarReference {
        public final String varName;   // root variable name
        public final boolean isDollar; // true = ${...}, false = #{...}
        public final String rawRef;    // full inner content

        public VarReference(String varName, boolean isDollar, String rawRef) {
            this.varName = varName;
            this.isDollar = isDollar;
            this.rawRef = rawRef;
        }
    }

    public List<MapperParseResult> parseMapperFiles(List<String> searchPaths) {
        List<MapperParseResult> results = new ArrayList<>();

        for (String searchPath : searchPaths) {
            Path rootPath = Paths.get(searchPath);
            if (!Files.exists(rootPath)) {
                logger.debug("Search path does not exist: {}", searchPath);
                continue;
            }
            try {
                Files.walk(rootPath)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .forEach(xmlFile -> {
                        try {
                            MapperParseResult result = parseMapperFile(xmlFile.toFile());
                            if (result != null) {
                                results.add(result);
                                logger.debug("Parsed mapper: {} (namespace={})",
                                    xmlFile.getFileName(), result.namespace);
                            }
                        } catch (Exception e) {
                            logger.debug("Failed to parse: {}", xmlFile, e);
                        }
                    });
            } catch (IOException e) {
                logger.warn("Failed to walk path: {}", searchPath, e);
            }
        }

        logger.info("Parsed {} MyBatis mapper files", results.size());
        return results;
    }

    public MapperParseResult parseMapperFile(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(
            "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature(
            "http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature(
            "http://xml.org/sax/features/external-parameter-entities", false);
        factory.setValidating(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        if (!"mapper".equals(root.getTagName())) return null;

        String namespace = root.getAttribute("namespace");
        if (namespace == null || namespace.isEmpty()) return null;

        List<SqlOperation> operations = new ArrayList<>();

        for (String tag : DML_TAGS) {
            NodeList nodes = root.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element elem = (Element) node;

                String id = elem.getAttribute("id");
                if (id == null || id.isEmpty()) continue;

                String sqlText = getFullTextContent(elem);
                List<VarReference> vars = extractVariables(sqlText);

                if (!vars.isEmpty()) {
                    operations.add(new SqlOperation(id, tag, sqlText, vars));
                }
            }
        }

        if (operations.isEmpty()) return null;
        return new MapperParseResult(namespace, operations);
    }

    private String getFullTextContent(Element elem) {
        StringBuilder sb = new StringBuilder();
        collectTextContent(elem, sb);
        return sb.toString();
    }

    private void collectTextContent(Node node, StringBuilder sb) {
        if (node.getNodeType() == Node.TEXT_NODE
            || node.getNodeType() == Node.CDATA_SECTION_NODE) {
            sb.append(node.getNodeValue());
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectTextContent(children.item(i), sb);
        }
    }

    private List<VarReference> extractVariables(String sqlText) {
        List<VarReference> vars = new ArrayList<>();
        if (sqlText == null || sqlText.isEmpty()) return vars;

        Matcher matcher = VAR_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String inner  = matcher.group(2).trim();
            boolean isDollar = "$".equals(prefix);

            String varName;
            if (inner.contains(".")) {
                varName = inner.substring(0, inner.indexOf('.'));
            } else {
                varName = inner.contains(",")
                    ? inner.substring(0, inner.indexOf(','))
                    : inner;
            }
            varName = varName.trim();

            if (!varName.isEmpty()) {
                vars.add(new VarReference(varName, isDollar, inner));
            }
        }
        return vars;
    }

    public Set<String> collectWriteFieldNames(List<MapperParseResult> mapperResults) {
        Set<String> writeFields = new HashSet<>();

        for (MapperParseResult result : mapperResults) {
            for (SqlOperation op : result.operations) {
                if (op.isWriteOperation()) {
                    for (VarReference var : op.variables) {
                        writeFields.add(var.varName);
                        if (var.rawRef.contains(".")) {
                            String leaf = var.rawRef
                                .substring(var.rawRef.lastIndexOf('.') + 1);
                            if (leaf.contains(",")) {
                                leaf = leaf.substring(0, leaf.indexOf(','));
                            }
                            writeFields.add(leaf.trim());
                        }
                    }
                }
            }
        }

        logger.info("Collected {} write field names from INSERT/UPDATE operations",
            writeFields.size());
        return writeFields;
    }
}