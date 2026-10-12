package com.loginsight.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.loginsight.dto.request.DistanceRequest;
import com.loginsight.exception.InvalidQueryException;
import com.loginsight.model.AlgorithmType;
import com.loginsight.model.QueryResult;
import com.loginsight.model.QueryType;

/**
 * Verifies the Spring wiring of the engine registry (docs/02 §4): every engine found on the
 * classpath is registered under its composite key, dispatch reaches the right engine and unknown
 * keys fail with the documented 400-domain error.
 */
@SpringBootTest
class QueryDispatcherSpringTest {

    @Autowired
    private QueryDispatcher dispatcher;

    @Test
    void allEnginesRegistered() {
        assertTrue(dispatcher.engineCount() >= 30,
                "expected the full engine suite, got " + dispatcher.engineCount());
    }

    @Test
    void dispatchRunsLevenshteinEngine() {
        QueryContext context = QueryContext
                .builder(QueryType.EDIT_DISTANCE, AlgorithmType.LEVENSHTEIN)
                .request(new DistanceRequest("kitten", "sitting", false))
                .param("a", "kitten")
                .param("b", "sitting")
                .build();
        QueryResult result = dispatcher.dispatch(context);
        assertEquals(AlgorithmType.LEVENSHTEIN, result.getAlgorithm());
        assertEquals(QueryType.EDIT_DISTANCE, result.getQueryType());
        assertTrue(result.getExecutionTimeNanos() >= 0);
    }

    @Test
    void unknownCompositeKeyRejected() {
        QueryContext context = QueryContext
                .builder(QueryType.DOCUMENT_SIMILARITY, AlgorithmType.NAIVE).build();
        assertThrows(InvalidQueryException.class, () -> dispatcher.dispatch(context));
    }
}