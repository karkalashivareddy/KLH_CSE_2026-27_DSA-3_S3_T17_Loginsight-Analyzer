/**
 * Hand-written DSA engine. This is the syllabus-authentic core of LogInsight Analyzer.
 *
 * <p>ARCHITECTURE RULE (see docs/02 §8): algorithm implementations in this package must not
 * delegate to {@code java.util} collections/sorting to hide the required logic. The DSA engine
 * stays free of Spring/web references and is developed in later phases.</p>
 *
 * <p>Phase boundary: Phase 1 creates only the package skeleton. No algorithms exist yet.</p>
 */
package com.loginsight.dsa;