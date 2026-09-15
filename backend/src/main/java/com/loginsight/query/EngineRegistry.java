package com.loginsight.query;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.loginsight.query.engine.approximation.IncidentCoverEngine;
import com.loginsight.query.engine.approximation.SetCoverEngine;
import com.loginsight.query.engine.approximation.VertexCoverEngine;
import com.loginsight.query.engine.dp.BitmaskTspEngine;
import com.loginsight.query.engine.dp.DamerauEngine;
import com.loginsight.query.engine.dp.GlobalAlignmentEngine;
import com.loginsight.query.engine.dp.HamiltonianEngine;
import com.loginsight.query.engine.dp.LevenshteinEngine;
import com.loginsight.query.engine.dp.LocalAlignmentEngine;
import com.loginsight.query.engine.dp.MatrixChainEngine;
import com.loginsight.query.engine.dp.OptimalBstEngine;
import com.loginsight.query.engine.dp.RerootingEngine;
import com.loginsight.query.engine.dp.SosDpEngine;
import com.loginsight.query.engine.dp.TreeDpEngine;
import com.loginsight.query.engine.dp.WeightedEditEngine;
import com.loginsight.query.engine.flow.DinicEngine;
import com.loginsight.query.engine.flow.EdmondsKarpEngine;
import com.loginsight.query.engine.flow.FordFulkersonEngine;
import com.loginsight.query.engine.flow.MatchingEngine;
import com.loginsight.query.engine.flow.MinCostEngine;
import com.loginsight.query.engine.flow.MinCutEngine;
import com.loginsight.query.engine.parallel.ReduceEngine;
import com.loginsight.query.engine.parallel.ScanEngine;
import com.loginsight.query.engine.parallel.SortEngine;
import com.loginsight.query.engine.randomized.MillerRabinEngine;
import com.loginsight.query.engine.randomized.QuicksortEngine;
import com.loginsight.query.engine.randomized.ReservoirEngine;
import com.loginsight.query.engine.randomized.UniversalHashEngine;
import com.loginsight.query.engine.string.AhoCorasickEngine;
import com.loginsight.query.engine.string.FuzzySearchEngine;
import com.loginsight.query.engine.string.KmpSearchEngine;
import com.loginsight.query.engine.string.NaiveSearchEngine;
import com.loginsight.query.engine.string.RabinKarpSearchEngine;
import com.loginsight.query.engine.string.SuffixEngine;
import com.loginsight.query.engine.string.ZSearchEngine;

/**
 * Central engine registry (docs/02 §4). Engines stay annotation-free by design; this single config
 * exposes them all to Spring so {@link QueryDispatcher} can collect them through the constructor
 * injection list. Adding a new engine means adding one line here and registering its composite
 * {@link QueryKey} in the documentation.
 */
@Configuration
public class EngineRegistry {

    @Bean
    public List<QueryEngine> queryEngines() {
        return List.of(
                // string
                new NaiveSearchEngine(), new KmpSearchEngine(), new ZSearchEngine(),
                new RabinKarpSearchEngine(), new AhoCorasickEngine(), new SuffixEngine(),
                new FuzzySearchEngine(),
                // dp
                new LevenshteinEngine(), new DamerauEngine(), new WeightedEditEngine(),
                new GlobalAlignmentEngine(), new LocalAlignmentEngine(), new MatrixChainEngine(),
                new OptimalBstEngine(), new BitmaskTspEngine(), new HamiltonianEngine(),
                new TreeDpEngine(), new RerootingEngine(), new SosDpEngine(),
                // flow
                new FordFulkersonEngine(), new EdmondsKarpEngine(), new DinicEngine(),
                new MinCutEngine(), new MatchingEngine(), new MinCostEngine(),
                // approximation
                new VertexCoverEngine(), new IncidentCoverEngine(), new SetCoverEngine(),
                // randomized
                new MillerRabinEngine(), new ReservoirEngine(), new UniversalHashEngine(),
                new QuicksortEngine(),
                // parallel
                new ReduceEngine(), new ScanEngine(), new SortEngine());
    }
}