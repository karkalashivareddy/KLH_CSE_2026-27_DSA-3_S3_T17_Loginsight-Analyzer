package com.loginsight.query.engine.string;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.dsa.string.aho.AhoCorasick;
import com.loginsight.dsa.string.aho.PatternMatch;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Multi-pattern matcher over the dataset text using the Aho-Corasick automaton (docs/12 §1). All
 * patterns feed one automaton and the text is scanned once; every occurrence is reported with its
 * owning pattern. The trie node count and the failure-link evidence are exposed as intermediate
 * data.
 */
public final class AhoCorasickEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.MULTI_PATTERN_SEARCH;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.AHO_CORASICK;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        Object raw = context.getParam("patterns");
        String[] patterns = raw instanceof String[] arr ? arr : null;
        if (patterns == null || patterns.length == 0) {
            throw new InvalidQueryException("at least one pattern is required");
        }
        for (String pattern : patterns) {
            QueryValidator.requirePattern(pattern);
        }
        QueryValidator.requireText(context.getText());

        long start = System.nanoTime();
        AhoCorasick automaton = new AhoCorasick(patterns);
        StringSearchResult matched = automaton.match(context.getText());
        long elapsed = System.nanoTime() - start;

        PatternMatch[] occurrences = automaton.search(context.getText());
        List<Map<String, Object>> occurrenceList = new ArrayList<>(occurrences.length);
        for (PatternMatch match : occurrences) {
            occurrenceList.add(Map.of("pattern", match.getPattern(), "start", match.getStart(),
                    "patternId", match.getPatternId()));
        }
        Map<String, Object> result = Map.of("totalMatches", occurrenceList.size(),
                "patterns", patterns.length, "occurrences", occurrenceList);
        Map<String, Object> intermediate = Map.of("trieNodes", automaton.nodeCount(),
                "patterns", automaton.patternCount());
        return Results.measured(type(), algorithm(), context.getText().length(), start, result,
                intermediate, 48L * automaton.nodeCount(), matched.getTimeComplexity(),
                matched.getSpaceComplexity(),
                "single automaton, one pass, output chained through dictionary-suffix links");
    }
}