package com.loginsight.service.text;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.loginsight.catalog.AlgorithmInfo;
import com.loginsight.dto.RequestFactory;
import com.loginsight.dto.request.FlowRequest;
import com.loginsight.dto.request.FuzzyRequest;
import com.loginsight.dto.request.AlignmentRequest;
import com.loginsight.dto.request.MillerRabinRequest;
import com.loginsight.dto.request.Scope;
import com.loginsight.dto.request.SearchRequest;
import com.loginsight.dto.request.VertexCoverRequest;
import com.loginsight.dto.response.AlgorithmResultDto;
import com.loginsight.dto.response.TextHackResponseDto;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryDispatcher;
import com.loginsight.service.CatalogService;
import com.loginsight.service.LogService;

/**
 * The TextHack natural-style query facade (docs/REBUILD_BASELINE Phase-3). Six fixed query classes
 * each route to one hand-built engine through the existing {@link QueryDispatcher}; no execution
 * path is duplicated and every number in the response is produced by the real algorithm.
 */
@Service
public class TextHackService {

    private static final long DEFAULT_PRIME_SEED = 0x5EEDL;

    private final QueryDispatcher dispatcher;
    private final LogService logService;
    private final CatalogService catalog;

    public TextHackService(QueryDispatcher dispatcher, LogService logService,
                           CatalogService catalog) {
        this.dispatcher = dispatcher;
        this.logService = logService;
        this.catalog = catalog;
    }

    public TextHackResponseDto execute(String queryClass, Map<String, Object> input) {
        return switch (TextHackCommand.parse(queryClass)) {
            case PATTERN_SEARCH -> patternSearch(input);
            case FUZZY_MATCH -> fuzzyMatch(input);
            case DOCUMENT_SIMILARITY -> documentSimilarity(input);
            case CITATION_FLOW -> citationFlow(input);
            case PROJECT_SCHEDULING -> projectScheduling(input);
            case PRIME_TESTING -> primeTesting(input);
        };
    }

    private TextHackResponseDto patternSearch(Map<String, Object> input) {
        SearchRequest request = RequestFactory.search(input);
        AlgorithmResultDto executed = dispatchPattern(request, AlgorithmType.KMP);
        return respond(TextHackCommand.PATTERN_SEARCH, "strings", executed, "kmp",
                "kmp", "z", "rabinkarp", "naive");
    }

    private TextHackResponseDto fuzzyMatch(Map<String, Object> input) {
        FuzzyRequest request = RequestFactory.fuzzy(input);
        String lines = request.text() == null || request.text().isBlank()
                ? logService.searchableText() : request.text();
        QueryContext context = QueryContext
                .builder(QueryType.FUZZY_SEARCH, AlgorithmType.LEVENSHTEIN)
                .request(request).text(lines)
                .source(request.text() == null || request.text().isBlank()
                        ? QueryContext.DATASET : "request")
                .build();
        AlgorithmResultDto executed = AlgorithmResultDto.from(dispatcher.dispatch(context),
                request.query());
        return respond(TextHackCommand.FUZZY_MATCH, "strings", executed, null,
                "fuzzy_search", "levenshtein");
    }

    private TextHackResponseDto documentSimilarity(Map<String, Object> input) {
        AlignmentRequest request = RequestFactory.alignment(input);
        QueryContext context = QueryContext
                .builder(QueryType.GLOBAL_ALIGNMENT, AlgorithmType.NEEDLEMAN_WUNSCH)
                .request(request).build();
        AlgorithmResultDto executed = AlgorithmResultDto.from(dispatcher.dispatch(context));

        AlgorithmResultDto withSimilarity = executed;
        if (executed.result() instanceof Map<?, ?> result) {
            Object alignedA = result.get("alignedA");
            Object alignedB = result.get("alignedB");
            Object lengthValue = result.get("alignmentLength");
            int length = lengthValue instanceof Integer ? (Integer) lengthValue : 0;
            if (alignedA instanceof List<?> a && alignedB instanceof List<?> b && length > 0) {
                int identity = 0;
                for (int i = 0; i < length; i++) {
                    Object left = i < a.size() ? a.get(i) : null;
                    Object right = i < b.size() ? b.get(i) : null;
                    if (left != null && right != null && left.equals(right)
                            && !"-".equals(left)) {
                        identity++;
                    }
                }
                Map<String, Object> enriched = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : result.entrySet()) {
                    enriched.put(String.valueOf(e.getKey()), e.getValue());
                }
                enriched.put("similarityPercent", (int) Math.round(100.0 * identity / length));
                enriched.put("identityMatches", identity);
                withSimilarity = new AlgorithmResultDto(executed.algorithm(),
                        executed.queryType(), executed.inputSize(), null, enriched,
                        executed.intermediateData(), executed.executionTimeNanos(),
                        executed.memoryEstimateBytes(), executed.timeComplexity(),
                        executed.spaceComplexity(), executed.notes());
            }
        }
        return respond(TextHackCommand.DOCUMENT_SIMILARITY, "dp", withSimilarity, null,
                "global_alignment", "levenshtein");
    }

    private TextHackResponseDto citationFlow(Map<String, Object> input) {
        FlowRequest request = RequestFactory.flow(input);
        QueryContext context = QueryContext
                .builder(QueryType.SERVICE_FLOW, AlgorithmType.DINIC)
                .request(request).build();
        AlgorithmResultDto executed = AlgorithmResultDto.from(dispatcher.dispatch(context));
        return respond(TextHackCommand.CITATION_FLOW, "flow", executed, "dinic",
                "dinic", "edmondskarp", "fordfulkerson", "min_cut");
    }

    private TextHackResponseDto projectScheduling(Map<String, Object> input) {
        VertexCoverRequest request = RequestFactory.vertexCover(input);
        QueryContext context = QueryContext
                .builder(QueryType.APPROXIMATE_COVER, AlgorithmType.VERTEX_COVER)
                .request(request).build();
        AlgorithmResultDto executed = AlgorithmResultDto.from(dispatcher.dispatch(context));
        return respond(TextHackCommand.PROJECT_SCHEDULING, "approximation", executed,
                "vertexcover", "vertexcover", "bounded_vertex_cover",
                "vertex_cover_kernelization");
    }

    private TextHackResponseDto primeTesting(Map<String, Object> input) {
        MillerRabinRequest request = RequestFactory.millerRabin(input);
        long seed = RequestFactory.seedOf(input) == null ? DEFAULT_PRIME_SEED
                : RequestFactory.seedOf(input);
        QueryContext context = QueryContext
                .builder(QueryType.PRIMALITY_TEST, AlgorithmType.MILLER_RABIN)
                .request(request)
                .param("seed", seed)
                .build();
        AlgorithmResultDto executed = AlgorithmResultDto.from(dispatcher.dispatch(context));
        return respond(TextHackCommand.PRIME_TESTING, "randomized", executed, "millerrabin",
                "millerrabin");
    }

    private AlgorithmResultDto dispatchPattern(SearchRequest request, AlgorithmType algorithm) {
        Scope scope = request.scope() == null ? Scope.EXPLICIT : request.scope();
        String text = scope == Scope.EXPLICIT
                ? (request.text() == null ? "" : request.text()) : logService.searchableText();
        QueryContext context = QueryContext
                .builder(QueryType.PATTERN_SEARCH, algorithm)
                .request(request).scope(scope).text(text).pattern(request.pattern())
                .source(scope == Scope.DATASET ? QueryContext.DATASET : "request")
                .build();
        return AlgorithmResultDto.from(dispatcher.dispatch(context), request.pattern());
    }

    private TextHackResponseDto respond(TextHackCommand command, String moduleId,
                                        AlgorithmResultDto executed, String traceKey,
                                        String... recommendedKeys) {
        List<AlgorithmInfo> recommended = Arrays.stream(recommendedKeys)
                .map(key -> catalog.algorithm(key).orElseThrow())
                .toList();
        String moduleLabel = catalog.module(moduleId).orElseThrow().title();
        return new TextHackResponseDto(command.name(), command.label(), moduleId, moduleLabel,
                command.description(), recommended, executed, traceKey);
    }
}