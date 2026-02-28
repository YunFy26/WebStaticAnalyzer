package org.example.taint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Writes the generated TaintAnalysisConfig to a Tai-e 0.5.1 compatible YAML file.
 *
 * Tai-e 0.5.1 source format:
 *   param source: { kind: param, method: "...", index: N, type: "..." }
 *   call  source: { method: "...", type: "..." }   ← no kind/index fields in 0.5.1
 *
 * Output sections:
 *   sources   - SRFA param sources + universal framework call sources
 *   sinks     - SRFA risky ${...} MyBatis mapper sinks + common predefined sinks
 *   transfers - Standard Java/JEE/security-framework propagation rules
 */
public class TaintConfigYamlWriter {

    private static final Logger logger = LogManager.getLogger(TaintConfigYamlWriter.class);

    public void write(TaintAnalysisConfig config, String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Auto-generated taint analysis configuration by SRFA\n");
        sb.append("# Chapter 4: Fine-grained Taint Source and Sink Detection\n\n");

        // ── Sources ──────────────────────────────────────────────────────────
        List<TaintAnalysisConfig.SourceConfig> sources = config.getSources();
        sb.append("sources:\n");
        for (TaintAnalysisConfig.SourceConfig source : sources) {
            sb.append("  - { kind: ").append(source.getKind())
                .append(", method: \"").append(escapeYaml(source.getMethod())).append("\"")
                .append(", index: ").append(source.getIndex())
                .append(", type: \"").append(source.getType()).append("\"")
                .append(" }\n");
        }
        // Universal framework-injected call sources (Tai-e 0.5.1 format: method + type only)
        sb.append(FRAMEWORK_INJECTED_SOURCES);

        sb.append("\n");

        // ── Sinks ─────────────────────────────────────────────────────────────
        List<TaintAnalysisConfig.SinkConfig> sinks = config.getSinks();
        sb.append("sinks:\n");
        if (!sinks.isEmpty()) {
            for (TaintAnalysisConfig.SinkConfig sink : sinks) {
                sb.append("  - { method: \"").append(escapeYaml(sink.getMethod())).append("\"")
                    .append(", index: ").append(sink.getIndex())
                    .append(" }\n");
            }
        }
        // Common predefined sinks (always included)
        sb.append(COMMON_SINKS);

        sb.append("\n");

        // ── Transfers ─────────────────────────────────────────────────────────
        sb.append("transfers:\n");
        sb.append(STANDARD_TRANSFERS);

        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);

        logger.info("Written taint config to: {}", filePath);
        logger.info("  Sources: {} (SRFA param) + framework call sources", sources.size());
        logger.info("  Sinks: {} (SRFA) + common predefined sinks", sinks.size());
        logger.info("  Transfers: standard set included");
    }

    private String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // =========================================================================
    // Framework-injected taint sources (Tai-e 0.5.1 call source format)
    //
    // In Tai-e 0.5.1, a "call source" is declared with only:
    //   { method: "SIGNATURE", type: "TYPE" }
    // The return value of the method becomes the taint source.
    // Do NOT add kind or index fields — they are not supported in 0.5.1
    // and will cause a NullPointerException in YamlTaintConfigProvider.
    //
    // Covers: Apache Shiro / Spring Security / Servlet Session / ThreadLocal
    // =========================================================================
    private static final String FRAMEWORK_INJECTED_SOURCES = """
  # ── Framework-injected taint sources (universal, Tai-e 0.5.1 format) ─────

  # --- Apache Shiro ---
  - { method: "<org.apache.shiro.SecurityUtils: org.apache.shiro.subject.Subject getSubject()>", type: "org.apache.shiro.subject.Subject" }
  - { method: "<org.apache.shiro.subject.Subject: java.lang.Object getPrincipal()>", type: "java.lang.Object" }
  - { method: "<org.apache.shiro.subject.Subject: org.apache.shiro.subject.PrincipalCollection getPrincipals()>", type: "org.apache.shiro.subject.PrincipalCollection" }
  - { method: "<org.apache.shiro.subject.PrincipalCollection: java.lang.Object getPrimaryPrincipal()>", type: "java.lang.Object" }

  # --- Spring Security ---
  - { method: "<org.springframework.security.core.context.SecurityContextHolder: org.springframework.security.core.context.SecurityContext getContext()>", type: "org.springframework.security.core.context.SecurityContext" }
  - { method: "<org.springframework.security.core.context.SecurityContext: org.springframework.security.core.Authentication getAuthentication()>", type: "org.springframework.security.core.Authentication" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.Object getPrincipal()>", type: "java.lang.Object" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.Object getDetails()>", type: "java.lang.Object" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.String getName()>", type: "java.lang.String" }
  - { method: "<org.springframework.security.core.Authentication: java.util.Collection getAuthorities()>", type: "java.util.Collection" }
  - { method: "<org.springframework.security.core.userdetails.UserDetails: java.lang.String getUsername()>", type: "java.lang.String" }

  # --- Servlet HttpSession (framework-agnostic) ---
  - { method: "<javax.servlet.http.HttpSession: java.lang.Object getAttribute(java.lang.String)>", type: "java.lang.Object" }
  - { method: "<javax.servlet.http.HttpSession: java.lang.Object getValue(java.lang.String)>", type: "java.lang.Object" }
  - { method: "<javax.servlet.http.HttpServletRequest: javax.servlet.http.HttpSession getSession()>", type: "javax.servlet.http.HttpSession" }
  - { method: "<javax.servlet.http.HttpServletRequest: javax.servlet.http.HttpSession getSession(boolean)>", type: "javax.servlet.http.HttpSession" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.security.Principal getUserPrincipal()>", type: "java.security.Principal" }
  - { method: "<java.security.Principal: java.lang.String getName()>", type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>", type: "java.lang.String" }

  # --- Request Attribute (JWT / custom filter pattern) ---
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.Object getAttribute(java.lang.String)>", type: "java.lang.Object" }
  - { method: "<javax.servlet.ServletRequest: java.lang.Object getAttribute(java.lang.String)>", type: "java.lang.Object" }

  # --- ThreadLocal user context (common custom pattern) ---
  - { method: "<java.lang.ThreadLocal: java.lang.Object get()>", type: "java.lang.Object" }
  - { method: "<java.lang.InheritableThreadLocal: java.lang.Object get()>", type: "java.lang.Object" }
""";

    // =========================================================================
    // Common predefined taint sinks (universal, framework-agnostic)
    //
    // Tai-e 0.5.1 sink format:
    //   { method: "SIGNATURE", index: N }
    // where index is the parameter position that receives tainted data.
    //
    // Covers: SQL Injection, Command Injection, Path Traversal, XSS,
    //         SSRF, LDAP Injection, XPath Injection, Expression Language
    //         Injection, Logging Injection, URL Redirect, Deserialization
    // =========================================================================
    private static final String COMMON_SINKS = """
  # ── SQL Injection ─────────────────────────────────────────────────────────
  # --- JDBC ---
  - { method: "<java.sql.Statement: java.sql.ResultSet executeQuery(java.lang.String)>", index: 0 }
  - { method: "<java.sql.Statement: int executeUpdate(java.lang.String)>", index: 0 }
  - { method: "<java.sql.Statement: boolean execute(java.lang.String)>", index: 0 }
  - { method: "<java.sql.Statement: int[] executeBatch()>", index: 0 }
  - { method: "<java.sql.Connection: java.sql.PreparedStatement prepareStatement(java.lang.String)>", index: 0 }
  - { method: "<java.sql.Connection: java.sql.CallableStatement prepareCall(java.lang.String)>", index: 0 }
  # --- Spring JdbcTemplate ---
  - { method: "<org.springframework.jdbc.core.JdbcTemplate: java.util.List query(java.lang.String,org.springframework.jdbc.core.RowMapper)>", index: 0 }
  - { method: "<org.springframework.jdbc.core.JdbcTemplate: java.lang.Object queryForObject(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.springframework.jdbc.core.JdbcTemplate: java.util.List queryForList(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.springframework.jdbc.core.JdbcTemplate: int update(java.lang.String)>", index: 0 }
  - { method: "<org.springframework.jdbc.core.JdbcTemplate: int[] batchUpdate(java.lang.String[])>", index: 0 }
  # --- JPA / Hibernate / EntityManager ---
  - { method: "<javax.persistence.EntityManager: javax.persistence.Query createQuery(java.lang.String)>", index: 0 }
  - { method: "<javax.persistence.EntityManager: javax.persistence.Query createNativeQuery(java.lang.String)>", index: 0 }
  - { method: "<javax.persistence.EntityManager: javax.persistence.Query createNativeQuery(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.hibernate.Session: org.hibernate.Query createQuery(java.lang.String)>", index: 0 }
  - { method: "<org.hibernate.Session: org.hibernate.SQLQuery createSQLQuery(java.lang.String)>", index: 0 }

  # ── Command Injection (OS Command) ────────────────────────────────────────
  - { method: "<java.lang.Runtime: java.lang.Process exec(java.lang.String)>", index: 0 }
  - { method: "<java.lang.Runtime: java.lang.Process exec(java.lang.String[])>", index: 0 }
  - { method: "<java.lang.Runtime: java.lang.Process exec(java.lang.String,java.lang.String[])>", index: 0 }
  - { method: "<java.lang.Runtime: java.lang.Process exec(java.lang.String,java.lang.String[],java.io.File)>", index: 0 }
  - { method: "<java.lang.ProcessBuilder: void <init>(java.lang.String[])>", index: 0 }
  - { method: "<java.lang.ProcessBuilder: void <init>(java.util.List)>", index: 0 }
  - { method: "<java.lang.ProcessBuilder: java.lang.ProcessBuilder command(java.lang.String[])>", index: 0 }
  - { method: "<java.lang.ProcessBuilder: java.lang.ProcessBuilder command(java.util.List)>", index: 0 }

  # ── Path Traversal / File Access ──────────────────────────────────────────
  - { method: "<java.io.File: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.io.File: void <init>(java.lang.String,java.lang.String)>", index: 0 }
  - { method: "<java.io.File: void <init>(java.lang.String,java.lang.String)>", index: 1 }
  - { method: "<java.io.FileInputStream: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.io.FileOutputStream: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.io.FileReader: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.io.FileWriter: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.nio.file.Paths: java.nio.file.Path get(java.lang.String,java.lang.String[])>", index: 0 }
  - { method: "<java.nio.file.Path: java.nio.file.Path resolve(java.lang.String)>", index: 0 }
  - { method: "<java.nio.file.Files: java.util.List readAllLines(java.nio.file.Path)>", index: 0 }
  - { method: "<java.nio.file.Files: byte[] readAllBytes(java.nio.file.Path)>", index: 0 }
  - { method: "<java.nio.file.Files: java.nio.file.Path write(java.nio.file.Path,byte[],java.nio.file.OpenOption[])>", index: 0 }

  # ── XSS (Cross-Site Scripting) ────────────────────────────────────────────
  # --- Servlet Response ---
  - { method: "<javax.servlet.http.HttpServletResponse: java.io.PrintWriter getWriter()>", index: 0 }
  - { method: "<java.io.PrintWriter: void print(java.lang.String)>", index: 0 }
  - { method: "<java.io.PrintWriter: void println(java.lang.String)>", index: 0 }
  - { method: "<java.io.PrintWriter: void write(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.ServletOutputStream: void print(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.ServletOutputStream: void write(byte[])>", index: 0 }
  # --- JSP ---
  - { method: "<javax.servlet.jsp.JspWriter: void print(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.jsp.JspWriter: void println(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.jsp.JspWriter: void write(java.lang.String)>", index: 0 }

  # ── SSRF (Server-Side Request Forgery) ────────────────────────────────────
  - { method: "<java.net.URL: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.net.URI: void <init>(java.lang.String)>", index: 0 }
  - { method: "<java.net.URI: java.net.URI create(java.lang.String)>", index: 0 }
  - { method: "<java.net.HttpURLConnection: void connect()>", index: base }
  - { method: "<org.apache.http.client.HttpClient: org.apache.http.HttpResponse execute(org.apache.http.client.methods.HttpUriRequest)>", index: 0 }
  - { method: "<org.springframework.web.client.RestTemplate: java.lang.Object getForObject(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.springframework.web.client.RestTemplate: org.springframework.http.ResponseEntity getForEntity(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.springframework.web.client.RestTemplate: java.lang.Object postForObject(java.lang.String,java.lang.Object,java.lang.Class)>", index: 0 }
  - { method: "<org.springframework.web.client.RestTemplate: org.springframework.http.ResponseEntity exchange(java.lang.String,org.springframework.http.HttpMethod,org.springframework.http.HttpEntity,java.lang.Class)>", index: 0 }

  # ── URL Redirect / Open Redirect ──────────────────────────────────────────
  - { method: "<javax.servlet.http.HttpServletResponse: void sendRedirect(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.RequestDispatcher: void forward(javax.servlet.ServletRequest,javax.servlet.ServletResponse)>", index: 0 }

  # ── LDAP Injection ────────────────────────────────────────────────────────
  - { method: "<javax.naming.directory.DirContext: javax.naming.NamingEnumeration search(java.lang.String,java.lang.String,javax.naming.directory.SearchControls)>", index: 0 }
  - { method: "<javax.naming.directory.DirContext: javax.naming.NamingEnumeration search(java.lang.String,java.lang.String,javax.naming.directory.SearchControls)>", index: 1 }
  - { method: "<javax.naming.directory.InitialDirContext: javax.naming.NamingEnumeration search(java.lang.String,java.lang.String,javax.naming.directory.SearchControls)>", index: 0 }
  - { method: "<javax.naming.directory.InitialDirContext: javax.naming.NamingEnumeration search(java.lang.String,java.lang.String,javax.naming.directory.SearchControls)>", index: 1 }
  - { method: "<org.springframework.ldap.core.LdapTemplate: java.util.List search(java.lang.String,java.lang.String,javax.naming.directory.SearchControls,org.springframework.ldap.core.AttributesMapper)>", index: 1 }

  # ── XPath Injection ───────────────────────────────────────────────────────
  - { method: "<javax.xml.xpath.XPath: java.lang.String evaluate(java.lang.String,java.lang.Object)>", index: 0 }
  - { method: "<javax.xml.xpath.XPath: javax.xml.xpath.XPathExpression compile(java.lang.String)>", index: 0 }

  # ── XML External Entity (XXE) ─────────────────────────────────────────────
  - { method: "<javax.xml.parsers.DocumentBuilder: org.w3c.dom.Document parse(java.io.InputStream)>", index: 0 }
  - { method: "<javax.xml.parsers.DocumentBuilder: org.w3c.dom.Document parse(java.lang.String)>", index: 0 }
  - { method: "<javax.xml.parsers.SAXParser: void parse(java.io.InputStream,org.xml.sax.helpers.DefaultHandler)>", index: 0 }
  - { method: "<org.xml.sax.XMLReader: void parse(org.xml.sax.InputSource)>", index: 0 }

  # ── Expression Language / Template Injection ──────────────────────────────
  # --- Spring SpEL ---
  - { method: "<org.springframework.expression.ExpressionParser: org.springframework.expression.Expression parseExpression(java.lang.String)>", index: 0 }
  - { method: "<org.springframework.expression.spel.standard.SpelExpressionParser: org.springframework.expression.Expression parseExpression(java.lang.String)>", index: 0 }
  # --- OGNL ---
  - { method: "<ognl.Ognl: java.lang.Object parseExpression(java.lang.String)>", index: 0 }
  - { method: "<ognl.Ognl: java.lang.Object getValue(java.lang.String,java.lang.Object)>", index: 0 }
  # --- MVEL ---
  - { method: "<org.mvel2.MVEL: java.lang.Object eval(java.lang.String)>", index: 0 }
  - { method: "<org.mvel2.MVEL: java.lang.Object eval(java.lang.String,java.lang.Object)>", index: 0 }
  # --- JEXL ---
  - { method: "<org.apache.commons.jexl3.JexlEngine: org.apache.commons.jexl3.JexlExpression createExpression(java.lang.String)>", index: 0 }
  - { method: "<org.apache.commons.jexl3.JexlEngine: org.apache.commons.jexl3.JexlScript createScript(java.lang.String)>", index: 0 }
  # --- Freemarker ---
  - { method: "<freemarker.template.Template: void <init>(java.lang.String,java.io.Reader)>", index: 0 }
  # --- Velocity ---
  - { method: "<org.apache.velocity.app.VelocityEngine: boolean evaluate(org.apache.velocity.context.Context,java.io.Writer,java.lang.String,java.lang.String)>", index: 3 }

  # ── Deserialization ───────────────────────────────────────────────────────
  - { method: "<java.io.ObjectInputStream: java.lang.Object readObject()>", index: base }
  - { method: "<java.io.ObjectInputStream: void <init>(java.io.InputStream)>", index: 0 }
  - { method: "<com.alibaba.fastjson.JSON: java.lang.Object parse(java.lang.String)>", index: 0 }
  - { method: "<com.alibaba.fastjson.JSON: java.lang.Object parseObject(java.lang.String)>", index: 0 }
  - { method: "<com.alibaba.fastjson.JSON: java.lang.Object parseObject(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<com.fasterxml.jackson.databind.ObjectMapper: java.lang.Object readValue(java.lang.String,java.lang.Class)>", index: 0 }
  - { method: "<org.yaml.snakeyaml.Yaml: java.lang.Object load(java.lang.String)>", index: 0 }
  - { method: "<org.yaml.snakeyaml.Yaml: java.lang.Object load(java.io.InputStream)>", index: 0 }

  # ── Logging Injection ─────────────────────────────────────────────────────
  # Note: These are lower severity but can enable log forging / log4shell
  - { method: "<org.apache.logging.log4j.Logger: void info(java.lang.String)>", index: 0 }
  - { method: "<org.apache.logging.log4j.Logger: void warn(java.lang.String)>", index: 0 }
  - { method: "<org.apache.logging.log4j.Logger: void error(java.lang.String)>", index: 0 }
  - { method: "<org.apache.logging.log4j.Logger: void debug(java.lang.String)>", index: 0 }
  - { method: "<org.apache.logging.log4j.Logger: void fatal(java.lang.String)>", index: 0 }
  - { method: "<org.slf4j.Logger: void info(java.lang.String)>", index: 0 }
  - { method: "<org.slf4j.Logger: void warn(java.lang.String)>", index: 0 }
  - { method: "<org.slf4j.Logger: void error(java.lang.String)>", index: 0 }
  - { method: "<org.slf4j.Logger: void debug(java.lang.String)>", index: 0 }

  # ── Reflection (Code Injection) ───────────────────────────────────────────
  - { method: "<java.lang.Class: java.lang.Class forName(java.lang.String)>", index: 0 }
  - { method: "<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>", index: 0 }
  - { method: "<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>", index: 1 }

  # ── Server-Side Include / RequestDispatcher ───────────────────────────────
  - { method: "<javax.servlet.ServletContext: javax.servlet.RequestDispatcher getRequestDispatcher(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.http.HttpServletRequest: javax.servlet.RequestDispatcher getRequestDispatcher(java.lang.String)>", index: 0 }

  # ── Cookie Manipulation ───────────────────────────────────────────────────
  - { method: "<javax.servlet.http.Cookie: void <init>(java.lang.String,java.lang.String)>", index: 1 }
  - { method: "<javax.servlet.http.Cookie: void setValue(java.lang.String)>", index: 0 }
  - { method: "<javax.servlet.http.HttpServletResponse: void addHeader(java.lang.String,java.lang.String)>", index: 1 }
  - { method: "<javax.servlet.http.HttpServletResponse: void setHeader(java.lang.String,java.lang.String)>", index: 1 }
""";

    // =========================================================================
    // Standard taint transfer rules (universal, framework-agnostic)
    // =========================================================================
    private static final String STANDARD_TRANSFERS = """
  # ── java.lang.String ─────────────────────────────────────────────────────
  - { method: "<java.lang.String: java.lang.String concat(java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String concat(java.lang.String)>", from: 0, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String substring(int)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String substring(int,int)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String replace(char,char)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String replace(java.lang.CharSequence,java.lang.CharSequence)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String replaceAll(java.lang.String,java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String replaceFirst(java.lang.String,java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String trim()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String strip()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String stripLeading()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String stripTrailing()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String toLowerCase()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String toLowerCase(java.util.Locale)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String toUpperCase()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String toUpperCase(java.util.Locale)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String intern()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String valueOf(java.lang.Object)>", from: 0, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String valueOf(char[])>", from: 0, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String format(java.lang.String,java.lang.Object[])>", from: 0, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String format(java.util.Locale,java.lang.String,java.lang.Object[])>", from: 1, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String join(java.lang.CharSequence,java.lang.CharSequence[])>", from: 1, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: java.lang.String join(java.lang.CharSequence,java.lang.Iterable)>", from: 1, to: result, type: "java.lang.String" }
  - { method: "<java.lang.String: char[] toCharArray()>", from: base, to: result, type: "char[]" }

  # ── java.lang.StringBuilder ───────────────────────────────────────────────
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(char[])>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(boolean)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(int)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(long)>", from: 0, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.String)>", from: base, to: result }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.Object)>", from: base, to: result }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder append(java.lang.CharSequence)>", from: base, to: result }
  - { method: "<java.lang.StringBuilder: java.lang.String toString()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.String)>", from: 1, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.Object)>", from: 1, to: base }
  - { method: "<java.lang.StringBuilder: java.lang.StringBuilder insert(int,java.lang.CharSequence)>", from: 1, to: base }

  # ── java.lang.StringBuffer ────────────────────────────────────────────────
  - { method: "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>", from: 0, to: base }
  - { method: "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.String)>", from: base, to: result }
  - { method: "<java.lang.StringBuffer: java.lang.StringBuffer append(java.lang.Object)>", from: base, to: result }
  - { method: "<java.lang.StringBuffer: java.lang.String toString()>", from: base, to: result, type: "java.lang.String" }

  # ── java.util.Map ─────────────────────────────────────────────────────────
  - { method: "<java.util.Map: java.lang.Object get(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.HashMap: java.lang.Object get(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.LinkedHashMap: java.lang.Object get(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.TreeMap: java.lang.Object get(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.concurrent.ConcurrentHashMap: java.lang.Object get(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>", from: 1, to: base }
  - { method: "<java.util.HashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>", from: 1, to: base }
  - { method: "<java.util.LinkedHashMap: java.lang.Object put(java.lang.Object,java.lang.Object)>", from: 1, to: base }
  - { method: "<java.util.Map: void putAll(java.util.Map)>", from: 0, to: base }
  - { method: "<java.util.Map: java.util.Collection values()>", from: base, to: result, type: "java.util.Collection" }
  - { method: "<java.util.Map: java.util.Set entrySet()>", from: base, to: result, type: "java.util.Set" }
  - { method: "<java.util.Map$Entry: java.lang.Object getValue()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.Map$Entry: java.lang.Object getKey()>", from: base, to: result, type: "java.lang.Object" }

  # ── java.util.List / Collection ───────────────────────────────────────────
  - { method: "<java.util.List: java.lang.Object get(int)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.ArrayList: java.lang.Object get(int)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.LinkedList: java.lang.Object get(int)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.List: boolean add(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.util.ArrayList: boolean add(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.util.LinkedList: boolean add(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.util.Collection: boolean add(java.lang.Object)>", from: 0, to: base }
  - { method: "<java.util.Iterator: java.lang.Object next()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.ListIterator: java.lang.Object next()>", from: base, to: result, type: "java.lang.Object" }

  # ── java.util.Optional ────────────────────────────────────────────────────
  - { method: "<java.util.Optional: java.lang.Object get()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.Optional: java.lang.Object orElse(java.lang.Object)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.Optional: java.lang.Object orElseGet(java.util.function.Supplier)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.util.Optional: java.util.Optional of(java.lang.Object)>", from: 0, to: result, type: "java.util.Optional" }
  - { method: "<java.util.Optional: java.util.Optional ofNullable(java.lang.Object)>", from: 0, to: result, type: "java.util.Optional" }

  # ── java.lang.Object ──────────────────────────────────────────────────────
  - { method: "<java.lang.Object: java.lang.String toString()>", from: base, to: result, type: "java.lang.String" }

  # ── Servlet request parameter / header / URI ──────────────────────────────
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getParameter(java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.ServletRequest: java.lang.String getParameter(java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String[] getParameterValues(java.lang.String)>", from: base, to: result, type: "java.lang.String[]" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getHeader(java.lang.String)>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getQueryString()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getRequestURI()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.StringBuffer getRequestURL()>", from: base, to: result, type: "java.lang.StringBuffer" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.String getRemoteAddr()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<javax.servlet.http.HttpServletRequest: java.lang.Object getAttribute(java.lang.String)>", from: base, to: result, type: "java.lang.Object" }

  # ── Shiro propagation chain ───────────────────────────────────────────────
  - { method: "<org.apache.shiro.subject.Subject: java.lang.Object getPrincipal()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<org.apache.shiro.subject.Subject: org.apache.shiro.subject.PrincipalCollection getPrincipals()>", from: base, to: result, type: "org.apache.shiro.subject.PrincipalCollection" }
  - { method: "<org.apache.shiro.subject.PrincipalCollection: java.lang.Object getPrimaryPrincipal()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<org.apache.shiro.subject.PrincipalCollection: java.util.Collection fromRealm(java.lang.String)>", from: base, to: result, type: "java.util.Collection" }

  # ── Spring Security propagation chain ─────────────────────────────────────
  - { method: "<org.springframework.security.core.context.SecurityContext: org.springframework.security.core.Authentication getAuthentication()>", from: base, to: result, type: "org.springframework.security.core.Authentication" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.Object getPrincipal()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.Object getDetails()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<org.springframework.security.core.Authentication: java.lang.String getName()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<org.springframework.security.core.Authentication: java.util.Collection getAuthorities()>", from: base, to: result, type: "java.util.Collection" }
  - { method: "<org.springframework.security.core.userdetails.UserDetails: java.lang.String getUsername()>", from: base, to: result, type: "java.lang.String" }
  - { method: "<org.springframework.security.core.GrantedAuthority: java.lang.String getAuthority()>", from: base, to: result, type: "java.lang.String" }

  # ── HttpSession / ThreadLocal propagation ─────────────────────────────────
  - { method: "<javax.servlet.http.HttpSession: java.lang.Object getAttribute(java.lang.String)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<javax.servlet.http.HttpSession: java.lang.Object getValue(java.lang.String)>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<javax.servlet.http.HttpSession: void setAttribute(java.lang.String,java.lang.Object)>", from: 1, to: base }
  - { method: "<java.lang.ThreadLocal: java.lang.Object get()>", from: base, to: result, type: "java.lang.Object" }
  - { method: "<java.lang.InheritableThreadLocal: java.lang.Object get()>", from: base, to: result, type: "java.lang.Object" }
""";
}