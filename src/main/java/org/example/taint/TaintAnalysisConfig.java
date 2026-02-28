// File: TaintAnalysisConfig.java
package org.example.taint;

import java.util.ArrayList;
import java.util.List;

public class TaintAnalysisConfig {

    private final List<SourceConfig> sources = new ArrayList<>();
    private final List<SinkConfig> sinks = new ArrayList<>();

    public void addSource(SourceConfig source) {
        sources.add(source);
    }

    public void addSink(SinkConfig sink) {
        sinks.add(sink);
    }

    public List<SourceConfig> getSources() {
        return sources;
    }

    public List<SinkConfig> getSinks() {
        return sinks;
    }

    public static class SourceConfig {
        private final String method;
        private final int index;
        private final String type;
        private final String kind;

        public SourceConfig(String method, int index, String type, String kind) {
            this.method = method;
            this.index = index;
            this.type = type;
            this.kind = kind;
        }

        public String getMethod() { return method; }
        public int getIndex() { return index; }
        public String getType() { return type; }
        public String getKind() { return kind; }
    }

    public static class SinkConfig {
        private final String method;
        private final int index;

        public SinkConfig(String method, int index) {
            this.method = method;
            this.index = index;
        }

        public String getMethod() { return method; }
        public int getIndex() { return index; }
    }
}