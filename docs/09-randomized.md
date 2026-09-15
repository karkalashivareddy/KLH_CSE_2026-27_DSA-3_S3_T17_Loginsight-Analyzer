# 09 - Randomized Algorithms Theory (Module 6A)

This document records the theory behind the randomized algorithms implemented in
`backend/src/main/java/com/loginsight/dsa/randomized/`. Every claim below is stated honestly:
probability statements are bounds over the randomness, never per-run guarantees.

---

## 1. Las Vegas vs Monte Carlo

A randomized algorithm falls into one of two families, classified by whether the **output** or
the **runtime** is guaranteed.

| Property | Las Vegas | Monte Carlo |
|---|---|---|
| Output | always correct | correct with probability ≥ 1 − ε |
| Runtime | random (expected-case analysis) | deterministic bound |
| Randomization used for | choosing pivots / samples | probabilistic evidence |
| Example here | `RandomizedQuickSort` (always sorted, random runtime) | `MillerRabin` probabilistic mode (constant runtime, small false-positive chance) |

A Las Vegas algorithm can be converted to Monte Carlo by adding a time bound and declaring
failure on timeout. A Monte Carlo algorithm with a verifier becomes Las Vegas (rerun until the
verifier accepts). This project keeps both pure forms:

- `RandomizedQuickSort` — Las Vegas: output is guaranteed sorted; only the comparisons/swaps
  are random (expected O(n log n)).
- `MillerRabin` (probabilistic mode) — Monte Carlo: constant rounds, false-positive probability
  at most 4^(−rounds).

---

## 2. Randomized Quicksort

`RandomizedQuickSort` picks a uniformly random pivot in the current subarray, moves it to the
low end, and partitions with the Hoare scheme. Tail-recursion is eliminated by recursing on the
smaller half and looping on the larger, keeping stack depth O(log n) expected.

- **Expected** O(n log n) comparisons.
- **Worst case** O(n²) — happens only if the random generator keeps picking the worst pivot
  (probability 1/n! for a fixed adversarial order).
- Hoare's partition spreads elements equal to the pivot across both sides, so all-equal inputs
  cost O(n) per level, O(n log n) total (unlike Lomuto, which would degenerate to O(n²)).
- The input array is never mutated; a sorted defensive copy is returned.

Empirical check in `RandomizedQuicksortTest`: output equals a test-only `Arrays.sort` oracle on
25 test cases including sorted, reverse-sorted, all-equal, duplicates, negatives, and 5,000
random longs.

---

## 3. Miller-Rabin Primality

Miller-Rabin tests the equality constraint that any prime `n` must satisfy for every base `a`:

```
write n−1 = d · 2^s   (d odd)
a^d ≡ 1 (mod n)  OR  a^(d·2^r) ≡ −1 (mod n) for some 0 ≤ r < s
```

If this fails for some base `a`, then `a` is a **witness** that `n` is composite.

Two modes are exposed (see `MillerRabin.Mode`):

| Mode | Bases | Guarantee |
|---|---|---|
| DETERMINISTIC | fixed set `{2, 325, 9375, 28178, 450775, 9780504, 1795265022}` | proves primality for every `n < 2^64`; since all positive `long` satisfy `n < 2^63`, this covers the whole `long` range |
| PROBABILISTIC | `rounds` random bases in `[2, n−2]` | false-positive probability ≤ 4^(−rounds) |

The deterministic 7-witness set is the smallest published set that covers all 64-bit inputs;
popular 12-prime-base sets only cover a fraction of the 64-bit range and are deliberately
not claimed here.

**Modular arithmetic.** `ModularArithmetic.powMod` (square-and-multiply) drives each exponent
`a^d mod n`; every multiplication uses double-and-add, so no intermediate product ever exceeds
`2^63 − 1`. No `BigInteger` appears in production code. Cost per round is O(log² n) 64-bit word
operations.

**Test oracles** (`MillerRabinTest`): trial-division results for small values, known Carmichael
numbers (561, 1105, 1729, 2465, 2821, 6601, 8911), pseudoprimes (341550071728321, and the strong
pseudoprime 3825123056546413051), `2^61−1` and `2^63−25` (prime), `2^63−1` (composite), and a
`BigInteger.isProbablePrime` cross-check (test-only).

---

## 4. Universal Hashing

`UniversalHashFamily` implements the classic universal family

```
h(x) = ((a·x + b) mod p) mod m
```

with `p` prime, `a ∈ [1, p−1]`, `b ∈ [0, p−1]`, and table size `m < p`. The default prime is
the Mersenne prime `2^61 − 1 = 2305843009213693951`.

**Pairwise independence.** For any two distinct keys `x ≠ y`, over a uniform choice of `(a, b)`:

```
Pr[h(x) = h(y)] ≤ ⌈p/m⌉ / p ≈ 1/m
```

because `a·(x−y) ≡ 0 (mod p)` only when `a ≡ 0 (mod p)`, which is excluded by construction.
The constant `⌈p/m⌉` absorbs the rounding when `p` is not an exact multiple of `m`.

**Two honest caveats, both labeled in code and tests:**
1. This is a *probability bound over the family*, not a guarantee for a single parameter set.
2. Empirical collision counts on a finite sample are a sanity demonstration, not a proof of
   universality.
3. Consecutive integers are a **known invalid** stress test for a linear universal hash: on an
   un-wrapped arithmetic sequence, `((a·i+b) mod p) mod m` becomes an affine map over `m` with
   structure dominated by `gcd(a, m)`. Random keys from the full `long` range are used instead.

The `RandomizedHash` demo hashes a static key set and reports the number of colliding keys, the
maximum chain length, and the load factor, making the uniform-bucket behaviour visible.

---

## 5. Perfect Hashing (FKS)

`PerfectHash` implements the two-level scheme of Fredman–Komlós–Szemerédi (1984) for a static set
of `n` distinct keys:

1. A first-level universal hash maps keys into `m = n` buckets.
2. If bucket `j` holds `b_j` keys, give it a secondary table of size `m_j = b_j²` and a fresh
   random secondary hash, rebuilt until the bucket is collision-free (retry cap 128).
3. Empty secondary slots hold a sentinel value that is not one of the keys.

**Guarantees**
- `contains` is worst-case O(1): two hash computations and one comparison.
- Space is expected O(n): `E[Σ m_j] = E[Σ b_j²] ≈ 2n` when `m = n`.
- Worst-case space O(n²) arises only when many keys land in one first-level bucket; this is
  documented honestly and the class refuses buckets larger than 46,340 keys (to avoid int
  overflow in `b_j²`).
- The structure is static and read-only: `insert`/`remove` throw `UnsupportedOperationException`.

The sentinel is chosen dynamically (0, 1, −1, 2, −2, …) so it can never collide with a key by
construction.

---

## 6. Reservoir Sampling

`ReservoirSampling` implements Algorithm R for uniform sampling over a stream of unknown length:

- For the first `k` elements: store them.
- For element `i` (`i > k`): draw `j ← RNG.nextInt(i)`; if `j < k`, replace element `j`.

**Invariant.** After processing `i` elements, every one of the `i` elements is in the reservoir
with probability `k / i`. Proof by induction: element `i` is kept with probability `k / i` by
construction, and each of the first `i−1` elements survives with probability
`(1 − 1/k)·(i−1 choose …)` — the classic telescoping product.

**Contract**
- `k = 0` → the sample is always empty.
- `k ≥ stream length` → the sample contains every element seen so far.
- Time O(1) per offer, O(k) memory.

**Empirical check** (`ReservoirSamplingTest`): with stream length 10, `k = 3`, and 40,000
seeded trials, each element appears in about 3/10 of the samples (tolerance ± 0.015); this is a
finite-sample sanity check, not a mathematical proof of uniformity.

---

## 7. Randomness Engineering

- No global mutable random state exists; every algorithm receives its random source by
  constructor/method injection (`RandomSource`).
- `RandomSource.seeded(seed)` gives reproducible sequences for tests and demos;
  `RandomSource.unseeded()` is time-seeded for production.
- A fixed seed reproduces not only the answer but the exact same witness set, pivot choices, and
  reservoir contents across runs, which is what makes the deterministic tests meaningful.
- The statistical tests (hashes, reservoir) use many fixed seeds, so the suite is fast and
  deterministic (< 1 s for the randomized module).