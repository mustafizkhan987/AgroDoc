package com.agrodoc.app.ml

import com.agrodoc.app.domain.model.TrustLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class TrustScorerTest {
    @Test fun lowConfidenceIsLowTrust() {
        assertEquals(TrustLevel.LOW, TrustScorer.score(0.49f, 0.4f, 0.9f, 0.8f))
    }

    @Test fun allStrongSignalsAreHighTrust() {
        assertEquals(TrustLevel.HIGH, TrustScorer.score(0.9f, 0.4f, 0.8f, 0.5f))
    }

    @Test fun intermediateSignalsAreMediumTrust() {
        assertEquals(TrustLevel.MEDIUM, TrustScorer.score(0.7f, 0.2f, 0.6f, 0.4f))
    }
}
