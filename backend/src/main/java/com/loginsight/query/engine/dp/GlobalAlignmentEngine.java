package com.loginsight.query.engine.dp;

import java.util.List;
import java.util.Map;

import com.loginsight.dsa.dp.alignment.AlignmentResult;
import com.loginsight.dsa.dp.alignment.NeedlemanWunsch;
import com.loginsight.dto.request.AlignmentRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;
import com.loginsight.query.QueryContext;
import com.loginsight.query.QueryEngine;
import com.loginsight.query.QueryValidator;
import com.loginsight.query.Results;

/**
 * Global sequence alignment with Needleman-Wunsch (docs/12 §4). Aligns every token of both
 * sequences end-to-end; alignment score, aligned rows and matrix stats form the payload.
 */
public final class GlobalAlignmentEngine implements QueryEngine {

    @Override
    public QueryType type() {
        return QueryType.GLOBAL_ALIGNMENT;
    }

    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.NEEDLEMAN_WUNSCH;
    }

    @Override
    public QueryResult execute(QueryContext context) {
        AlignmentRequest request = (AlignmentRequest) context.getRequest();
        String a = request.a();
        String b = request.b();
        String[] seqA = request.seqA();
        String[] seqB = request.seqB();

        long start = System.nanoTime();
        AlignmentResult aligned;
        int inputSize;
        if (a != null || b != null) {
            String textA = QueryValidator.requireNotBlank(a, "a");
            String textB = QueryValidator.requireNotBlank(b, "b");
            QueryValidator.requireQuadraticSequences(textA, textB);
            aligned = new NeedlemanWunsch().align(textA, textB, request.match(), request.mismatch(),
                    request.gap());
            inputSize = textA.length() + textB.length();
        } else {
            if (seqA == null || seqA.length == 0) {
                throw new InvalidQueryException("seqA must not be empty");
            }
            if (seqB == null || seqB.length == 0) {
                throw new InvalidQueryException("seqB must not be empty");
            }
            QueryValidator.requireQuadraticSequences(seqA.length, seqB.length);
            aligned = new NeedlemanWunsch().align(seqA, seqB, request.match(), request.mismatch(),
                    request.gap());
            inputSize = seqA.length + seqB.length;
        }

        Map<String, Object> result = Map.of("score", aligned.getScore(),
                "alignedA", List.of(aligned.getAlignedA()),
                "alignedB", List.of(aligned.getAlignedB()),
                "gapCount", aligned.getGapCount(), "alignmentLength", aligned.getAlignmentLength());
        return Results.measured(type(), algorithm(), inputSize, start, result,
                aligned.getMatrix(), matrixBytes(aligned.getMatrix()),
                "O(n·m)", "O(n·m) scoring matrix",
                "global alignment: every token aligned, gaps penalized");
    }

    private static long matrixBytes(long[][] matrix) {
        if (matrix.length == 0) {
            return 0;
        }
        return (long) matrix.length * matrix[0].length * 8;
    }
}