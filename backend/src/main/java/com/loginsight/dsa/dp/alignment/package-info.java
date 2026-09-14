/**
 * Sequence alignment (DSA-3 Module 3): Needleman-Wunsch (global) and Smith-Waterman (local), sharing
 * {@link com.loginsight.dsa.dp.alignment.AlignmentResult}.
 *
 * <p>Inputs are token sequences (a plain {@code String} is treated as one token per UTF-16 char, so
 * service traces such as {@code AUTH-USER-DB} can be passed as token arrays). Scoring is configurable
 * (match, mismatch, gap); gap placement and tie-breaking are deterministic and documented.</p>
 */
package com.loginsight.dsa.dp.alignment;
