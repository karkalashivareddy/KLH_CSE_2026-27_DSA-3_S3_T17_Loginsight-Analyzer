package com.loginsight.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loginsight.dto.request.FuzzyRequest;
import com.loginsight.dto.request.MultiPatternRequest;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.SuffixBuildRequest;
import com.loginsight.dto.request.SuffixSearchRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.service.SearchService;

/**
 * String-search endpoints (docs/12 §1): the four single-matcher algorithms over the dataset or an
 * explicit text, the multi-pattern automaton search, suffix-array build/search and the fuzzy
 * (edit-distance) search over the dataset lines.
 */
@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search/naive")
    public AlgorithmResultDto naive(@RequestBody SearchRequest request) {
        return searchService.naive(request);
    }

    @PostMapping("/search/kmp")
    public AlgorithmResultDto kmp(@RequestBody SearchRequest request) {
        return searchService.kmp(request);
    }

    @PostMapping("/search/z")
    public AlgorithmResultDto z(@RequestBody SearchRequest request) {
        return searchService.z(request);
    }

    @PostMapping("/search/rabin-karp")
    public AlgorithmResultDto rabinKarp(@RequestBody SearchRequest request) {
        return searchService.rabinKarp(request);
    }

    @PostMapping("/search/multi")
    public AlgorithmResultDto multi(@RequestBody MultiPatternRequest request) {
        return searchService.multi(request);
    }

    @PostMapping("/string/suffix/build")
    public AlgorithmResultDto suffixBuild(@RequestBody SuffixBuildRequest request) {
        return searchService.suffixBuild(request);
    }

    @PostMapping("/string/suffix/search")
    public AlgorithmResultDto suffixSearch(@RequestBody SuffixSearchRequest request) {
        return searchService.suffixSearch(request);
    }

    @PostMapping("/fuzzy/search")
    public AlgorithmResultDto fuzzy(@RequestBody FuzzyRequest request) {
        return searchService.fuzzy(request);
    }
}