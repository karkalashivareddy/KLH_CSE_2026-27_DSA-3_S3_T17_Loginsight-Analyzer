package com.loginsight.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.loginsight.dto.request.LogSearchRequest;
import com.loginsight.dto.response.LogEventDto;
import com.loginsight.dto.response.LogSearchResponse;
import com.loginsight.dto.response.SuggestionDto;
import com.loginsight.dsa.dp.editdistance.LevenshteinDistance;
import com.loginsight.dsa.string.KMPMatcher;
import com.loginsight.dsa.string.StringSearchResult;
import com.loginsight.exception.DatasetException;
import com.loginsight.index.LogIndex;
import com.loginsight.index.LogIndexService;
import com.loginsight.model.LogEvent;
import com.loginsight.service.Dataset;
import com.loginsight.service.DatasetService;

/**
 * The LogInsight product search service (docs/API.md §3).
 *
 * <p>Structured filters are resolved through the in-memory {@link LogIndex}; free text is executed
 * as a single-phrase pattern by the DSA string engine ({@link KMPMatcher} by default) over a
 * rendered haystack of the candidate events, with measured duration. Zero matches trigger an
 * algorithmically computed "did you mean" via Levenshtein edit distance over the distinct message
 * corpus. Nothing here is faked: every number in the response is derived from the loaded dataset
 * and the actual engine run.</p>
 */
@Service
public class LogSearchService {

    private final DatasetService datasetService;
    private final LogIndexService indexService;
    private final SearchQueryParser queryParser = new SearchQueryParser();
    private final KMPMatcher kmp = new KMPMatcher();
    private final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public LogSearchService(DatasetService datasetService, LogIndexService indexService) {
        this.datasetService = datasetService;
        this.indexService = indexService;
    }

    /** Execute the search (docs/API.md §3). */
    public LogSearchResponse search(LogSearchRequest request) {
        Dataset dataset = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"));
        List<LogEvent> events = dataset.events();
        LogIndex index = indexService.current();
        long start = System.nanoTime();

        SearchQuery query = queryParser.parse(request.query(), request.from(), request.to(),
                request.sort());
        List<Integer> candidates = candidatePositions(index, query);

        String pattern = query.freeText();
        String strategy = pattern.isBlank() ? "Structured filters only"
                : "Substring search over searchable text";

        List<LogSearchResponse.SearchHit> hits = new ArrayList<>();
        long total;
        String algorithm = null;
        int patternLength = 0;
        long textSize = 0;

        if (pattern.isBlank()) {
            total = candidates.size();
            int from = (request.page() - 1) * request.size();
            int to = Math.min(from + request.size(), candidates.size());
            for (int i = from; i < to; i++) {
                hits.add(new LogSearchResponse.SearchHit(LogEventDto.from(events.get(candidates.get(i))),
                        null, 0));
            }
            textSize = candidates.size();
        } else {
            // Render the candidate haystack (lowercased) and run the DSA matcher once.
            String patternLower = pattern.toLowerCase(java.util.Locale.ROOT);
            StringBuilder haystack = new StringBuilder();
            int[] lineStarts = new int[candidates.size()];
            List<String> originalLines = new ArrayList<>(candidates.size());
            Set<Integer> matchedLines = new LinkedHashSet<>();
            Map<Integer, Integer> matchesPerEvent = new HashMap<>();
            for (int i = 0; i < candidates.size(); i++) {
                LogEvent event = events.get(candidates.get(i));
                String searchable = event.searchableText();
                lineStarts[i] = haystack.length();
                originalLines.add(searchable);
                haystack.append(searchable.toLowerCase(java.util.Locale.ROOT)).append('\n');
            }
            String text = haystack.toString();
            StringSearchResult result = kmp.match(text, patternLower);
            algorithm = "KMP";
            patternLength = pattern.length();
            textSize = text.length();
            int[] positions = result.getMatchPositions();
            if (positions.length > 0) {
                int lineCount = candidates.size();
                int lineIdx = -1;
                for (int pos : positions) {
                    int idx = locateLine(lineStarts, lineCount, pos);
                    if (idx < 0) {
                        continue;
                    }
                    if (idx != lineIdx) {
                        lineIdx = idx;
                        matchesPerEvent.put(idx, 0);
                    }
                    matchesPerEvent.put(idx, matchesPerEvent.get(idx) + 1);
                }
                matchedLines.addAll(matchesPerEvent.keySet());
            }
            total = matchedLines.size();
            List<Integer> ordered = new ArrayList<>(matchedLines);
            int from = (request.page() - 1) * request.size();
            int to = Math.min(from + request.size(), ordered.size());
            for (int i = from; i < to; i++) {
                int candidateIndex = ordered.get(i);
                int position = candidates.get(candidateIndex);
                String lineText = originalLines.get(candidateIndex);
                hits.add(new LogSearchResponse.SearchHit(
                        LogEventDto.from(events.get(position)),
                        snippet(lineText, pattern),
                        matchesPerEvent.getOrDefault(candidateIndex, 0)));
            }
        }

        long durationNanos = System.nanoTime() - start;
        LogSearchResponse.FuzzySuggestion suggestion = null;
        if (total == 0 && !pattern.isBlank()) {
            suggestion = fuzzySuggest(events, index, pattern);
        }
        return new LogSearchResponse(request.query(), strategy, algorithm, pattern, patternLength,
                textSize, durationNanos, total, request.page(), request.size(), request.sort(),
                dataset.name(), hits, suggestion);
    }

    /** Resolve structured filters to candidate positions and sort by the requested order. */
    private List<Integer> candidatePositions(LogIndex index, SearchQuery query) {
        List<int[]> groups = new ArrayList<>();
        if (!query.levels().isEmpty()) {
            int[] byLevel = index.bySeverity(query.levels());
            if (byLevel.length == 0) {
                return List.of();
            }
            groups.add(byLevel);
        }
        addIfPresent(groups, index.byService(query.service()));
        addIfPresent(groups, index.byHost(query.host()));
        addIfPresent(groups, index.bySource(query.source()));
        addIfPresent(groups, index.byStatus(query.status()));
        addIfPresent(groups, index.byTrace(query.traceId()));
        if (query.from() != null || query.to() != null) {
            long from = query.from() == null ? Long.MIN_VALUE : query.from().toEpochMilli();
            long to = query.to() == null ? Long.MAX_VALUE : query.to().toEpochMilli();
            int[] window = index.inTimeWindow(from, to);
            if (window.length == 0) {
                return List.of();
            }
            groups.add(window);
        }
        int[] positions;
        if (groups.isEmpty()) {
            int[] all = new int[index.size()];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            positions = all;
        } else {
            positions = groups.get(0);
            for (int i = 1; i < groups.size(); i++) {
                positions = LogIndex.intersect(positions, groups.get(i));
            }
        }
        List<Integer> result = new ArrayList<>(positions.length);
        for (int p : positions) {
            result.add(p);
        }
        sort(result, index, query.sort());
        return result;
    }

    private static void addIfPresent(List<int[]> groups, int[] list) {
        if (list.length > 0) {
            groups.add(list);
        }
    }

    /** Stable ordering of positions by a requestable event field (timestamp, level, service). */
    private static void sort(List<Integer> positions, LogIndex index, String sortSpec) {
        String field = sortSpec == null ? "timestamp" : sortSpec.split(":")[0];
        boolean desc = sortSpec == null || !sortSpec.endsWith("asc");
        java.util.Comparator<Integer> comparator = switch (field) {
            case "level" -> java.util.Comparator.comparingInt(
                    p -> index.eventAt(p).getLevel() == null
                            ? -1 : index.eventAt(p).getLevel().ordinal());
            case "service" -> java.util.Comparator.comparing(
                    p -> index.eventAt(p).getService() == null
                            ? "" : index.eventAt(p).getService());
            case "id" -> java.util.Comparator.comparingLong(p -> index.eventAt(p).getId());
            default -> java.util.Comparator.comparingLong((Integer p) -> {
                LogEvent event = index.eventAt(p);
                if (event.getTimestamp() == null) {
                    return Long.MIN_VALUE;
                }
                return event.getTimestamp().toEpochMilli();
            });
        };
        if (desc) {
            comparator = comparator.reversed();
        }
        positions.sort(comparator);
    }

    private static int locateLine(int[] lineStarts, int lineCount, int offset) {
        int lo = 0;
        int hi = lineCount - 1;
        int answer = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (lineStarts[mid] <= offset) {
                answer = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        if (answer == -1) {
            return -1;
        }
        int start = lineStarts[answer];
        int end = answer + 1 < lineCount ? lineStarts[answer + 1] : Integer.MAX_VALUE;
        return offset >= start && offset < end ? answer : -1;
    }

    private static String snippet(String lineText, String pattern) {
        int idx = indexOfIgnoreCase(lineText, pattern);
        if (idx < 0) {
            String trimmed = lineText.trim();
            return trimmed.length() <= 140 ? trimmed : trimmed.substring(0, 140) + "\u2026";
        }
        int from = Math.max(0, idx - 30);
        int to = Math.min(lineText.length(), idx + pattern.length() + 40);
        return (from > 0 ? "\u2026" : "") + lineText.substring(from, to)
                + (to < lineText.length() ? "\u2026" : "");
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        String lower = haystack.toLowerCase(java.util.Locale.ROOT);
        String lowerNeedle = needle.toLowerCase(java.util.Locale.ROOT);
        return lower.indexOf(lowerNeedle);
    }

    /** "Did you mean?" via Levenshtein over the distinct message corpus. */
    private LogSearchResponse.FuzzySuggestion fuzzySuggest(List<LogEvent> events, LogIndex index,
                                                           String pattern) {
        Map<String, Integer> frequency = new HashMap<>();
        List<String> messages = new ArrayList<>();
        for (LogEvent event : events) {
            String message = event.getMessage();
            if (message == null) {
                continue;
            }
            if (frequency.containsKey(message)) {
                frequency.merge(message, 1, Integer::sum);
            } else {
                frequency.put(message, 1);
                messages.add(message);
                if (messages.size() >= 4000) {
                    break;
                }
            }
        }
        String best = null;
        long bestDistance = Long.MAX_VALUE;
        for (String candidate : messages) {
            long d = levenshtein.distance(pattern, candidate);
            if (d < bestDistance) {
                bestDistance = d;
                best = candidate;
            }
        }
        if (best == null) {
            return null;
        }
        int maxLen = Math.max(pattern.length(), best.length());
        int threshold = Math.max(2, (int) Math.ceil(maxLen * 0.35));
        if (bestDistance > threshold) {
            return null;
        }
        int similarity = (int) Math.round(100.0 * (1 - (double) bestDistance / Math.max(1, maxLen)));
        return new LogSearchResponse.FuzzySuggestion(best, Math.max(0, Math.min(100, similarity)),
                frequency.getOrDefault(best, 0), bestDistance, "Levenshtein");
    }

    /**
     * Typeahead candidates for the search bar derived from the loaded dataset (docs/API.md §3.5):
     * matching services, hosts, endpoints, sources, status codes and levels, plus any message that
     * starts with the typed prefix. Values are committed to the query as field filters.
     */
    public List<SuggestionDto> suggest(String raw, int limit) {
        List<LogEvent> events = datasetService.currentDataset()
                .orElseThrow(() -> new DatasetException("No dataset loaded"))
                .events();
        String q = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        Set<String> services = new LinkedHashSet<>();
        Set<String> hosts = new LinkedHashSet<>();
        Set<String> endpoints = new LinkedHashSet<>();
        Set<String> sources = new LinkedHashSet<>();
        Set<String> statuses = new LinkedHashSet<>();
        Set<String> levels = new LinkedHashSet<>();
        List<SuggestionDto> out = new ArrayList<>();
        int budget = limit;

        for (LogEvent event : events) {
            if (event.getService() != null && event.getService().toLowerCase(java.util.Locale.ROOT)
                    .contains(q)) {
                services.add(event.getService());
            }
            if (event.getHost() != null && event.getHost().toLowerCase(java.util.Locale.ROOT)
                    .contains(q)) {
                hosts.add(event.getHost());
            }
            if (event.getEndpoint() != null && event.getEndpoint()
                    .toLowerCase(java.util.Locale.ROOT).contains(q)) {
                endpoints.add(event.getEndpoint());
            }
            if (event.getSource() != null && event.getSource().toLowerCase(java.util.Locale.ROOT)
                    .contains(q)) {
                sources.add(event.getSource());
            }
            if (event.getStatusCode() != 0
                    && Integer.toString(event.getStatusCode()).contains(q)) {
                statuses.add(Integer.toString(event.getStatusCode()));
            }
            if (event.getLevel() != null
                    && event.getLevel().name().toLowerCase(java.util.Locale.ROOT).contains(q)) {
                levels.add(event.getLevel().name());
            }
        }
        for (String level : levels) {
            out.add(new SuggestionDto("level", level, "level:" + level));
        }
        for (String status : statuses) {
            out.add(new SuggestionDto("status", status, "status:" + status));
        }
        for (String service : services) {
            out.add(new SuggestionDto("service", service, "service:" + service));
            if (out.size() >= budget) {
                return out;
            }
        }
        for (String host : hosts) {
            out.add(new SuggestionDto("host", host, "host:" + host));
            if (out.size() >= budget) {
                return out;
            }
        }
        for (String source : sources) {
            out.add(new SuggestionDto("source", source, "source:" + source));
            if (out.size() >= budget) {
                return out;
            }
        }
        if (out.size() < budget) {
            for (String endpoint : endpoints) {
                out.add(new SuggestionDto("endpoint", endpoint, "message:\"" + endpoint + "\""));
                if (out.size() >= budget) {
                    break;
                }
            }
        }
        return out;
    }
}