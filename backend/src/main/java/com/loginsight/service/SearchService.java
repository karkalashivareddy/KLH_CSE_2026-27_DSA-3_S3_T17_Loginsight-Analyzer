package com.loginsight.service;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.FuzzyRequest;
import com.loginsight.dto.request.MultiPatternRequest;
import com.loginsight.dto.request.Scope;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.SuffixBuildRequest;
import com.loginsight.dto.request.SuffixSearchRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.exception.DatasetException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;

/**
 * Orchestrates the string-search scenarios (docs/12 §1). Resolves the haystack from the request
 * scope, assembles a fully-resolved {@link QueryContext} and dispatches through the engine layer,
 * mapping the engine {@link QueryResult} to the canonical AlgorithmResult envelope with the pattern
 * echoed back.
 */
@Service
public class SearchService {

    private final QueryDispatcher dispatcher;
    private final LogService logService;

    public SearchService(QueryDispatcher dispatcher, LogService logService) {
        this.dispatcher = dispatcher;
        this.logService = logService;
    }

    public AlgorithmResultDto naive(SearchRequest request) {
        return dispatchPattern(request, AlgorithmType.NAIVE);
    }

    public AlgorithmResultDto kmp(SearchRequest request) {
        return dispatchPattern(request, AlgorithmType.KMP);
    }

    public AlgorithmResultDto z(SearchRequest request) {
        return dispatchPattern(request, AlgorithmType.Z);
    }

    public AlgorithmResultDto rabinKarp(SearchRequest request) {
        return dispatchPattern(request, AlgorithmType.RABIN_KARP);
    }

    public AlgorithmResultDto multi(MultiPatternRequest request) {
        Scope scope = request.scope() == null ? Scope.EXPLICIT : request.scope();
        QueryContext context = QueryContext
                .builder(QueryType.MULTI_PATTERN_SEARCH, AlgorithmType.AHO_CORASICK)
                .request(request)
                .text(resolveScope(scope, request.text()))
                .source(sourceOf(scope))
                .param("patterns", request.patterns() == null ? new String[0] : request.patterns())
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto suffixBuild(SuffixBuildRequest request) {
        QueryContext context = QueryContext
                .builder(QueryType.SUFFIX_ANALYSIS, AlgorithmType.SUFFIX_ARRAY)
                .request(request)
                .text(resolveSuffixText(request.text()))
                .param("variant", "build")
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), null);
    }

    public AlgorithmResultDto suffixSearch(SuffixSearchRequest request) {
        String text = request.text() == null || request.text().isBlank()
                ? logService.searchableText() : request.text();
        QueryContext context = QueryContext
                .builder(QueryType.SUFFIX_ANALYSIS, AlgorithmType.SUFFIX_ARRAY)
                .request(request)
                .text(text)
                .pattern(request.pattern())
                .param("variant", "search")
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), request.pattern());
    }

    public AlgorithmResultDto fuzzy(FuzzyRequest request) {
        String lines = request.text() == null || request.text().isBlank()
                ? logService.searchableText() : request.text();
        QueryContext context = QueryContext
                .builder(QueryType.FUZZY_SEARCH, AlgorithmType.LEVENSHTEIN)
                .request(request)
                .text(lines)
                .source(request.text() == null || request.text().isBlank()
                        ? QueryContext.DATASET : "request")
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), request.query());
    }

    private AlgorithmResultDto dispatchPattern(SearchRequest request, AlgorithmType algorithm) {
        Scope scope = request.scope() == null ? Scope.EXPLICIT : request.scope();
        QueryContext context = QueryContext
                .builder(QueryType.PATTERN_SEARCH, algorithm)
                .request(request)
                .scope(scope)
                .text(resolveScope(scope, request.text()))
                .pattern(request.pattern())
                .source(sourceOf(scope))
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), request.pattern());
    }

    /** Scope resolution: dataset requires a loaded dataset (404); EXPLICIT uses the given text. */
    private String resolveScope(Scope scope, String explicitText) {
        if (scope == Scope.EXPLICIT) {
            return explicitText == null ? "" : explicitText;
        }
        return logService.searchableText();
    }

    private String resolveSuffixText(String text) {
        if (text != null && !text.isBlank()) {
            return text;
        }
        return logService.searchableText();
    }

    private static String sourceOf(Scope scope) {
        return scope == Scope.DATASET ? QueryContext.DATASET : "request";
    }
}