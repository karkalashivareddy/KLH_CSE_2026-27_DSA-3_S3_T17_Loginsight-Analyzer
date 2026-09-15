package com.loginsight.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.loginsight.dsa.parallel.BenchmarkResult;
import com.loginsight.dsa.parallel.ParallelBenchmark;
import com.loginsight.dsa.parallel.ParallelReduce;
import com.loginsight.dto.request.BenchmarkRequest;
import com.loginsight.dto.response.BenchmarkResultDto;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.query.QueryValidator;

/**
 * Benchmark sweep over the parallel engines (docs/12 §10). All timings are measured live by
 * {@link ParallelBenchmark} (warm-up + median over repetitions), so the reported rows are genuine
 * measurements of this machine, not canned numbers.
 */
@Service
public class BenchmarkService {

    public List<BenchmarkResultDto> run(BenchmarkRequest request) {
        String scenario = request.scenario();
        int[] sizes = sanitize(request.sizes());
        int repetitions = request.repetitions() == null ? 3 : request.repetitions();
        if (repetitions < 1 || repetitions > 20) {
            throw new InvalidQueryException("repetitions must be between 1 and 20");
        }
        int parallelism = request.parallelism() == null ? 0 : request.parallelism();
        if (parallelism < 0) {
            throw new InvalidQueryException("parallelism must be >= 0");
        }
        if (parallelism == 0) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }

        return switch (scenario) {
            case "dataset", "reduce" -> map(ParallelBenchmark.benchmarkReduce(sizes,
                    ParallelReduce.ReduceOp.SUM, parallelism, repetitions));
            case "scan" -> map(ParallelBenchmark.benchmarkScan(sizes, parallelism, repetitions));
            case "sort" -> map(ParallelBenchmark.benchmarkSort(sizes, parallelism, repetitions));
            default -> throw new InvalidQueryException("unknown scenario '" + scenario
                    + "' (dataset|reduce|scan|sort)");
        };
    }

    private static int[] sanitize(int[] sizes) {
        if (sizes == null || sizes.length == 0) {
            return BenchmarkRequest.DEFAULT_SIZES;
        }
        for (int size : sizes) {
            QueryValidator.requireSizes(size, 10_000_000);
        }
        return sizes.clone();
    }

    private static List<BenchmarkResultDto> map(List<BenchmarkResult> rows) {
        List<BenchmarkResultDto> mapped = new ArrayList<>(rows.size());
        for (BenchmarkResult row : rows) {
            mapped.add(BenchmarkResultDto.parallel(row.getAlgorithm(), row.getInputSize(),
                    row.getSequentialNanos(), row.getParallelNanos(), row.getSpeedup(),
                    row.getInputSize(), row.getWork(), row.getSpan(), row.getParallelism()));
        }
        return mapped;
    }
}