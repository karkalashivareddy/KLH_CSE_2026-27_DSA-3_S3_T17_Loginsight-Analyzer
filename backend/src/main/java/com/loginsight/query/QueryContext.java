package com.loginsight.query;

import java.util.LinkedHashMap;
import java.util.Map;

import com.loginsight.dto.request.Scope;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.LogEvent;
import com.loginsight.model.QueryType;

/**
 * Immutable carrier assembled by a controller/service before dispatch (docs/02 §4). Everything an
 * engine needs is resolved ahead of time by the service layer: the resolved haystack {@code text},
 * scratch {@code values}, the {@code pattern}, and a free-form {@code params} bag for scenario
 * knobs (rounds, table sizes, parallelism, …). Engines never touch the dataset or request directly.
 */
public final class QueryContext {

    public static final String DATASET = "dataset";

    private final QueryType queryType;
    private final AlgorithmType algorithm;
    private final Object request;
    private final String source;
    private final Scope scope;
    private final String text;
    private final String pattern;
    private final long[] values;
    private final Map<String, Object> params;

    private QueryContext(Builder builder) {
        this.queryType = builder.queryType;
        this.algorithm = builder.algorithm;
        this.request = builder.request;
        this.source = builder.source;
        this.scope = builder.scope;
        this.text = builder.text;
        this.pattern = builder.pattern;
        this.values = builder.values == null ? null : builder.values.clone();
        this.params = builder.params.isEmpty() ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(builder.params));
    }

    public static Builder builder(QueryType queryType, AlgorithmType algorithm) {
        return new Builder(queryType, algorithm);
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public Object getRequest() {
        return request;
    }

    /** Human-readable source name: "dataset" when the haystack came from logs, else "request". */
    public String getSource() {
        return source;
    }

    public Scope getScope() {
        return scope;
    }

    /** Resolved haystack text: non-null though possibly empty. */
    public String getText() {
        return text == null ? "" : text;
    }

    public String getPattern() {
        return pattern;
    }

    public long[] getValues() {
        return values == null ? null : values.clone();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public Object getParam(String key) {
        return params.get(key);
    }

    public static final class Builder {
        private final QueryType queryType;
        private final AlgorithmType algorithm;
        private Object request;
        private String source;
        private Scope scope = Scope.EXPLICIT;
        private String text;
        private String pattern;
        private long[] values;
        private final Map<String, Object> params = new LinkedHashMap<>();

        private Builder(QueryType queryType, AlgorithmType algorithm) {
            this.queryType = queryType;
            this.algorithm = algorithm;
        }

        public Builder request(Object request) {
            this.request = request;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder values(long[] values) {
            this.values = values;
            return this;
        }

        public Builder param(String key, Object value) {
            this.params.put(key, value);
            return this;
        }

        public Builder params(Map<String, Object> all) {
            this.params.putAll(all);
            return this;
        }

        public QueryContext build() {
            if (source == null) {
                source = queryType.name();
            }
            return new QueryContext(this);
        }
    }

    /** Renders a dataset into the searchable text used by {@code Scope.DATASET} searches. */
    public static String renderDataset(java.util.List<LogEvent> events) {
        StringBuilder sb = new StringBuilder();
        for (LogEvent event : events) {
            sb.append(event.getLevel() == null ? "" : event.getLevel().name()).append(' ')
                    .append(event.getService() == null ? "" : event.getService()).append(' ')
                    .append(event.getEndpoint() == null ? "" : event.getEndpoint()).append(' ')
                    .append(event.getMessage() == null ? "" : event.getMessage()).append('\n');
        }
        return sb.toString();
    }
}