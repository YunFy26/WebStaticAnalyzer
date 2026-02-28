// File: SRFAAnalysis.java
package org.example.taint;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.spring.analysis.RouterAnalysis;
import org.example.spring.analysis.router.ControllerClass;
import pascal.taie.World;
import pascal.taie.analysis.ProgramAnalysis;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.config.AnalysisOptions;

import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SRFAAnalysis extends ProgramAnalysis {

    private static final Logger logger = LogManager.getLogger(SRFAAnalysis.class);

    public static final String ID = "srfaAnalysis";

    public SRFAAnalysis(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Object analyze() {
        LocalDateTime startTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        logger.info("SRFA Analysis started at: {}", startTime.format(formatter));

        AnalysisOptions options = getOptions();

        // -------------------------------------------------------
        // Step 0: Retrieve RouterAnalysis results from Chapter 3
        // -------------------------------------------------------
        List<ControllerClass> controllerClasses;
        try {
            @SuppressWarnings("unchecked")
            List<ControllerClass> result =
                (List<ControllerClass>) World.get().getResult(RouterAnalysis.ID);
            controllerClasses = result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.warn("Could not retrieve RouterAnalysis result", e);
            controllerClasses = Collections.emptyList();
        }
        logger.info("Loaded {} controller classes from RouterAnalysis",
            controllerClasses.size());

        // -------------------------------------------------------
        // Step 1: Determine mapper search paths
        // -------------------------------------------------------
        String mapperPathsOption = options.getString("mapper-paths");
        List<String> mapperPaths;
        if (mapperPathsOption == null || mapperPathsOption.isBlank()) {
            mapperPaths = discoverMapperPathsFromClassPath();
        } else {
            mapperPaths = parseCommaSeparated(mapperPathsOption);
        }

        if (mapperPaths.isEmpty()) {
            logger.warn("No MyBatis mapper paths found. Sink config will be empty.");
        } else {
            logger.info("Using {} mapper search path(s):", mapperPaths.size());
            mapperPaths.forEach(p -> logger.info("  - {}", p));
        }

        // -------------------------------------------------------
        // Step 2: Parse MyBatis XML mapper files
        // -------------------------------------------------------
        MyBatisXMLParser xmlParser = new MyBatisXMLParser();
        List<MyBatisXMLParser.MapperParseResult> mapperResults =
            xmlParser.parseMapperFiles(mapperPaths);
        logger.info("Parsed {} MyBatis mapper files", mapperResults.size());

        // 诊断日志：打印所有解析到的mapper及其操作
        for (MyBatisXMLParser.MapperParseResult r : mapperResults) {
            logger.info("  Mapper namespace: {}", r.namespace);
            for (MyBatisXMLParser.SqlOperation op : r.operations) {
                boolean hasDollar = op.variables.stream().anyMatch(v -> v.isDollar);
                logger.info("    Operation: id={}, type={}, hasDollar={}",
                    op.id, op.operationType, hasDollar);
                if (hasDollar) {
                    op.variables.stream()
                        .filter(v -> v.isDollar)
                        .forEach(v -> logger.info("      Risky var: ${{{}}}", v.rawRef));
                }
            }
        }

        // -------------------------------------------------------
        // Step 3: Build F_write
        // -------------------------------------------------------
        Set<String> writeFields = xmlParser.collectWriteFieldNames(mapperResults);

        // -------------------------------------------------------
        // Step 4: Extract Source metadata (Algorithm 4-1)
        // -------------------------------------------------------
        SourceMetadataExtractor sourceExtractor = new SourceMetadataExtractor();
        List<SourceMetadata> sourceMetas = sourceExtractor.extract(controllerClasses);

        // -------------------------------------------------------
        // Step 5: Extract Sink metadata (Algorithm 4-2)
        // -------------------------------------------------------
        SinkMetadataExtractor sinkExtractor = new SinkMetadataExtractor();
        List<SinkMetadata> sinkMetas = sinkExtractor.extract(mapperResults);

        // -------------------------------------------------------
        // Step 6: Generate taint config
        // -------------------------------------------------------
        TaintConfigGenerator generator = new TaintConfigGenerator();
        TaintAnalysisConfig taintConfig =
            generator.generateConfig(sourceMetas, sinkMetas, writeFields);

        // -------------------------------------------------------
        // Step 7: Write YAML
        // -------------------------------------------------------
        String outputPath = options.getString("output-path");
        if (outputPath == null || outputPath.isBlank()) {
            outputPath = "output/taint-config-generated.yml";
        }
        TaintConfigYamlWriter writer = new TaintConfigYamlWriter();
        try {
            writer.write(taintConfig, outputPath);
        } catch (Exception e) {
            logger.error("Failed to write taint config to: {}", outputPath, e);
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        logger.info("SRFA Analysis completed at: {} (Duration: {}ms)",
            endTime.format(formatter), duration);

        return taintConfig;
    }

    /**
     * Auto-discover MyBatis mapper XML paths from appClassPath.
     *
     * Handles the following project structures:
     *
     * 1. Standard Maven (resources dir):
     *    src/main/resources/mapper/*.xml
     *
     * 2. Non-standard: XML mixed with Java source (VBlog, vhr style):
     *    src/main/java/com/example/mapper/*.xml
     *
     * 3. Multi-module Maven:
     *    module/target/classes → module/src/main/resources
     *                          → module/src/main/java
     *
     * 4. Non-standard classpath directory:
     *    tries up to 3 ancestor levels to find src/
     */
    private List<String> discoverMapperPathsFromClassPath() {
        Set<String> result = new LinkedHashSet<>();

        List<String> appClassPaths = World.get().getOptions().getAppClassPath();
        if (appClassPaths == null || appClassPaths.isEmpty()) {
            logger.warn("appClassPath is empty, cannot auto-discover mapper paths");
            return new ArrayList<>();
        }

        logger.info("Discovering mapper paths from {} appClassPath entries", appClassPaths.size());

        for (String classPathEntry : appClassPaths) {
            Path classPath = Paths.get(classPathEntry).toAbsolutePath().normalize();
            if (!Files.exists(classPath)) {
                logger.debug("ClassPath entry does not exist: {}", classPath);
                continue;
            }

            String pathStr = classPath.toString();
            logger.debug("Processing classPath entry: {}", pathStr);

            // -------------------------------------------------------
            // Spring Boot fat jar: extract XML files to temp directory
            // -------------------------------------------------------
            if (pathStr.endsWith(".jar")) {
                try {
                    extractXmlFromFatJar(classPath, result);
                } catch (Exception e) {
                    logger.warn("Failed to process jar: {}", classPath, e);
                }
                continue;
            }

            // Case 1: .../target/classes → infer module root
            if (pathStr.contains("target" + File.separator + "classes")
                || pathStr.endsWith("target/classes")) {
                Path moduleRoot = classPath.getParent().getParent();
                logger.debug("Inferred module root: {}", moduleRoot);
                boolean found = scanModuleSourceDirs(moduleRoot, result);
                if (!found) {
                    logger.debug("No src dirs found, scanning target/classes: {}", classPath);
                    collectXmlDirs(classPath, result);
                }
            }
            // Case 2: other directory
            else if (Files.isDirectory(classPath)) {
                boolean found = false;
                for (int levels = 1; levels <= 3; levels++) {
                    Path ancestor = goUp(classPath, levels);
                    if (ancestor == null) break;
                    if (scanModuleSourceDirs(ancestor, result)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    collectXmlDirs(classPath, result);
                }
            }
        }

        logger.info("Auto-discovered {} mapper path(s)", result.size());
        result.forEach(p -> logger.info("  - {}", p));
        return new ArrayList<>(result);
    }

    /**
     * Extract MyBatis XML mapper files from a Spring Boot fat jar.
     *
     * Spring Boot fat jar structure:
     *   BOOT-INF/classes/mapper/*.xml          ← most common
     *   BOOT-INF/classes/mapper/system/*.xml   ← multi-level
     *
     * Extracts all XML files under BOOT-INF/classes/ to a temp directory,
     * then collects directories containing XML files.
     */
    private void extractXmlFromFatJar(Path jarPath, Set<String> result) throws Exception {
        String jarName = jarPath.getFileName().toString()
            .replace(".jar", "").replace(".", "_");

        // Use a stable temp dir based on jar name + last modified time
        // so we don't re-extract every run
        long lastModified = Files.getLastModifiedTime(jarPath).toMillis();
        Path tempBase = Paths.get(System.getProperty("java.io.tmpdir"),
            "srfa-mapper-cache", jarName + "_" + lastModified);

        if (Files.exists(tempBase)) {
            logger.debug("Using cached extraction for jar: {}", jarPath.getFileName());
            collectXmlDirs(tempBase, result);
            return;
        }

        Files.createDirectories(tempBase);
        logger.info("Extracting XML from fat jar: {}", jarPath.getFileName());

        int extracted = 0;
        try (java.util.zip.ZipInputStream zis =
                 new java.util.zip.ZipInputStream(Files.newInputStream(jarPath))) {

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Only extract XML files from BOOT-INF/classes/ (business code)
                // Skip BOOT-INF/lib/ (dependency jars)
                if (!entry.isDirectory()
                    && entryName.endsWith(".xml")
                    && entryName.startsWith("BOOT-INF/classes/")) {

                    // Strip "BOOT-INF/classes/" prefix
                    String relativePath = entryName.substring("BOOT-INF/classes/".length());
                    Path targetFile = tempBase.resolve(relativePath);
                    Files.createDirectories(targetFile.getParent());

                    try (OutputStream os = Files.newOutputStream(targetFile)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            os.write(buf, 0, len);
                        }
                    }
                    extracted++;
                    logger.debug("Extracted: {}", relativePath);
                }
                zis.closeEntry();
            }
        }

        logger.info("Extracted {} XML files from {}", extracted, jarPath.getFileName());

        if (extracted > 0) {
            collectXmlDirs(tempBase, result);
        } else {
            logger.debug("No XML files found in BOOT-INF/classes/ of jar: {}",
                jarPath.getFileName());
            // Try root-level XMLs (non-Spring-Boot fat jar)
            extractXmlFromPlainJar(jarPath, tempBase, result);
        }
    }

    /**
     * Fallback: extract XMLs from root-level or any path in a plain jar.
     */
    private void extractXmlFromPlainJar(Path jarPath, Path tempBase,
                                        Set<String> result) throws Exception {
        int extracted = 0;
        try (java.util.zip.ZipInputStream zis =
                 new java.util.zip.ZipInputStream(Files.newInputStream(jarPath))) {

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entry.isDirectory() && entryName.endsWith(".xml")) {
                    Path targetFile = tempBase.resolve(
                        entryName.replace("/", File.separator));
                    Files.createDirectories(targetFile.getParent());

                    try (OutputStream os = Files.newOutputStream(targetFile)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            os.write(buf, 0, len);
                        }
                    }
                    extracted++;
                }
                zis.closeEntry();
            }
        }

        if (extracted > 0) {
            logger.info("Extracted {} XML files (plain jar) from {}",
                extracted, jarPath.getFileName());
            collectXmlDirs(tempBase, result);
        }
    }

    /**
     * Scan all possible source/resource directories under a module root for XML files.
     *
     * Covers:
     *   src/main/resources/          → standard Maven resources
     *   src/main/java/               → non-standard: XML mixed with Java (VBlog/vhr style)
     *   resources/                   → non-standard flat layout
     *
     * Returns true if at least one candidate directory exists under moduleRoot.
     */
    private boolean scanModuleSourceDirs(Path moduleRoot, Set<String> result) {
        boolean anyFound = false;

        // 1. Standard: src/main/resources (highest priority, most common)
        Path resourcesDir = moduleRoot.resolve("src/main/resources");
        if (Files.exists(resourcesDir) && Files.isDirectory(resourcesDir)) {
            int before = result.size();
            collectXmlDirs(resourcesDir, result);
            logger.debug("Scanned src/main/resources: {} (+{} xml dirs)",
                resourcesDir, result.size() - before);
            anyFound = true;
        }

        // 2. Non-standard: src/main/java (XML mixed with Java source, e.g. VBlog, vhr)
        Path javaDir = moduleRoot.resolve("src/main/java");
        if (Files.exists(javaDir) && Files.isDirectory(javaDir)) {
            int before = result.size();
            collectXmlDirs(javaDir, result);
            int added = result.size() - before;
            if (added > 0) {
                logger.debug("Scanned src/main/java: {} (+{} xml dirs, non-standard layout)",
                    javaDir, added);
                anyFound = true;
            }
        }

        // 3. Alternative flat resources dir at module root
        Path altResources = moduleRoot.resolve("resources");
        if (Files.exists(altResources) && Files.isDirectory(altResources)) {
            int before = result.size();
            collectXmlDirs(altResources, result);
            int added = result.size() - before;
            if (added > 0) {
                logger.debug("Scanned resources/: {} (+{} xml dirs)", altResources, added);
                anyFound = true;
            }
        }

        return anyFound;
    }

    /**
     * Recursively collect all directories that directly contain at least one .xml file.
     *
     * Covers structures like:
     *   src/main/resources/mapper/           ← xml files here
     *   src/main/resources/mapper/system/    ← xml files here
     *   src/main/java/com/example/mapper/    ← xml files here (non-standard)
     */
    private void collectXmlDirs(Path dir, Set<String> result) {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return;
        try {
            Files.walk(dir)
                .filter(Files::isDirectory)
                .forEach(subDir -> {
                    File[] xmlFiles = subDir.toFile().listFiles(
                        f -> f.isFile() && f.getName().endsWith(".xml")
                    );
                    if (xmlFiles != null && xmlFiles.length > 0) {
                        String absPath = subDir.toAbsolutePath().normalize().toString();
                        if (result.add(absPath)) {
                            logger.debug("Found XML dir: {} ({} xml files)",
                                absPath, xmlFiles.length);
                        }
                    }
                });
        } catch (Exception e) {
            logger.debug("Error scanning directory: {}", dir, e);
        }
    }

    /**
     * Walk up 'levels' directory levels. Returns null if root is exceeded.
     */
    private Path goUp(Path path, int levels) {
        Path current = path;
        for (int i = 0; i < levels; i++) {
            current = current.getParent();
            if (current == null) return null;
        }
        return current;
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}