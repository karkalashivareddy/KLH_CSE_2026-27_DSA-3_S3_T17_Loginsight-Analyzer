/**
 * Edit-distance family (DSA-3 Module 3): Levenshtein (Wagner-Fischer), Damerau-Levenshtein (Optimal
 * String Alignment variant) and weighted edit distance with configurable operation costs.
 *
 * <p>All three share {@link com.loginsight.dsa.dp.editdistance.EditDistanceResult} for reconstruction:
 * the metric, the DP matrix and the forward-ordered edit script. Unit costs are 1 for
 * insert/delete/substitute; the weighted variant takes arbitrary non-negative costs.</p>
 */
package com.loginsight.dsa.dp.editdistance;
