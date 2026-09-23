package com.loginsight.catalog;

/**
 * Static metadata for one algorithm in the laboratory catalogue (docs/REBUILD_BASELINE §Phase-2).
 *
 * <p>Every value is derived from the implemented source (complexity strings follow the class-level
 * javadoc of the owning DSA class, endpoint paths follow the verified REST surface). The catalogue
 * never invents an entry: a {@code canonicalEndpoint} / {@code traceEndpoint} is present only when
 * the endpoint really exists, and library-only algorithms are marked {@code exposed=false} so the
 * UI and the course map can distinguish "reachable from the UI" from "implemented and tested".</p>
 *
 * @param key              stable identifier used by runs and the frontend
 * @param name             human-readable algorithm name
 * @param moduleId         owning module id (strings / dp / flow / approximation / randomized /
 *                         parallel)
 * @param moduleLabel      academic category label (matches TraceCatalog categories)
 * @param problem          the DSA-3 feature this algorithm answers
 * @param queryType        {@link com.loginsight.model.QueryType} name the dispatcher routes
 * @param algorithmType    {@link com.loginsight.model.AlgorithmType} name of the concrete engine
 * @param canonicalEndpoint canonical REST endpoint (null when there is no direct endpoint)
 * @param traceEndpoint    trace REST endpoint (null when the algorithm is not trace-instrumented)
 * @param timeComplexity   best-accuracy complexity statement honoured by the implementation
 * @param spaceComplexity  space statement honoured by the implementation
 * @param tracked          true when the implementation records ordered algorithm steps
 * @param exposed          true when at least one REST endpoint (canonical or trace) exists
 * @param defaultInput     demonstration input reused by labs and the trace API (null otherwise)
 * @param description      short teaching note
 */
public record AlgorithmInfo(String key, String name, String moduleId, String moduleLabel,
                            String problem, String queryType, String algorithmType,
                            String canonicalEndpoint, String traceEndpoint, String timeComplexity,
                            String spaceComplexity, boolean tracked, boolean exposed,
                            Object defaultInput, String description) {
}