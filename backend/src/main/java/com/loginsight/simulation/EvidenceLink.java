package com.loginsight.simulation;

import java.util.List;

/**
 * One recorded algorithm invocation used as incident evidence.
 *
 * <p>Every field here is measured at runtime. {@code runtimeNanos} is the wall-clock duration of the
 * actual invocation, {@code inputSize} is the length of the data actually handed to the algorithm,
 * and {@code result} is a short factual summary of what came back. Nothing is estimated or
 * hand-written, so an evidence card can always be reproduced by re-running the same input.</p>
 *
 * @param algorithm     product-facing algorithm name, e.g. {@code Aho-Corasick}
 * @param purpose       what the invocation decided for this incident
 * @param inputSize     size of the actual input (characters, events, nodes — see {@code inputUnit})
 * @param inputUnit     unit for {@code inputSize}
 * @param result        factual summary of the returned result
 * @param runtimeNanos  measured execution time
 * @param complexity    the complexity actually claimed by the implementation
 * @param references    ids of the events or services the result points at
 */
public record EvidenceLink(
        String algorithm,
        String purpose,
        int inputSize,
        String inputUnit,
        String result,
        long runtimeNanos,
        String complexity,
        List<String> references) {

    public EvidenceLink {
        references = references == null ? List.of() : List.copyOf(references);
    }
}
