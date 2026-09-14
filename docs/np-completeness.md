# 04b — NP-Completeness & Approximation (Module 5 theory)

Companion to `docs/04-dsa-mapping.md` and the `com.loginsight.dsa.approximation` package. This
document covers the conceptual Module 5 body; executable counterparts are implemented and tested in
Phase 6 and marked below with their class name.

---

## 1. Complexity classes: P, NP, co-NP

| Class | Informal definition |
|---|---|
| **P** | Decision problems solvable in time polynomial in the input length by a *deterministic* algorithm. |
| **NP** | Decision problems for which every **YES** instance has a certificate that a *deterministic* verifier can check in polynomial time. (Non-deterministic guessing power = the certificate.) |
| **co-NP** | The complements of problems in NP: every **NO** instance has a poly-time-checkable certificate. |

Known facts, stated carefully:

- `P ⊆ NP` and `P ⊆ co-NP` (if a problem is decidable in polynomial time, its YES answers are trivially
  certified by running the algorithm, and so are its NO answers).
- Whether `P = NP` (equivalently whether `NP = co-NP`, by a classic symmetry argument) is **unresolved**.
  This document never asserts `P ≠ NP` as a fact.
- **NP-hard** and **NP-complete** are different concepts: a problem is *NP-hard* if every problem in NP
  reduces to it (it is at least as hard as everything in NP); it is *NP-complete* if it is NP-hard *and*
  belongs to NP. NP-hard problems need not lie in NP.

Example problems: SAT, 3-SAT, Vertex Cover, Independent Set, Clique, Set Cover, 0/1 Knapsack are
NP-complete (Knapsack only in the strong/exact-decision sense; it is NP-complete and weakly
NP-hard). Minimum Vertex Cover optimization is NP-hard.

---

## 2. Cook–Levin theorem and why SAT is NP-complete

- The **Cook–Levin theorem** (1971) states that SAT is NP-complete: (1) SAT is in NP (a satisfying
  assignment is a polynomial-time-checkable certificate); (2) every problem in NP has a
  polynomial-time reduction to SAT, by encoding the *computation of the non-deterministic machine* and
  its acceptance as a Boolean formula of polynomial size.
- **Reduction direction matters.** To show a problem `B` is NP-hard we reduce from a known NP-complete
  problem `A` to `B` in polynomial time (`A ≤_p B`): "if we could solve B quickly, we could solve A
  quickly." Reducing *from* B (or "solving B with an A-solver") proves nothing about B's hardness.
- Once SAT is NP-complete, the reduction chain (SAT → 3-SAT → Clique → Independent Set → Vertex Cover
  → Set Cover, and Vertex Cover → 0/1 Knapsack people-wise along parameterized/optimization lines)
  propagates NP-completeness.

This project implements only *small*, *educational* reduction demos (Vertex Cover ↔ Independent Set;
Clique ↔ Independent Set in the complement). A toy encoder of a bounded combinatorial problem into
Boolean constraints documents the flavour of Cook–Levin but is **not** a proof of the theorem — the
theorem is a statement about *all* of NP. The demo is `IndependentSetReduction` / `ComplementGraph`;
the theorem itself is documentation-only.

---

## 3. The reduction zoo

Executable demos in this project:

| Reduction | Claim | Class |
|---|---|---|
| Vertex Cover ↔ Independent Set | `S` is a vertex cover iff `V − S` is an independent set; `tau(G) = n − alpha(G)` | `IndependentSetReduction` |
| Clique ↔ Independent Set | `S` is a clique in `G` iff `S` is an independent set in `complement(G)` | `ComplementGraph` |

Narrative-only reductions (documented, part of viva material): SAT → 3-SAT (clause splitting),
3-SAT → Clique (triangle gadgets + a vertex per literal with consistency non-edges), Clique →
Independent Set (complement), Independent Set → Vertex Cover (complement), Vertex Cover → Set Cover
(edges as universe, vertex-neighbourhoods as sets).

---

## 4. Vertex Cover 2-approximation (executable)

`VertexCoverApproximation` (with its `ApproximationResult`):

- Compute a greedy maximal matching `M` (`MaximalMatching`); return both endpoints of every matched
  edge, plus every self-loop vertex (documented graph contract — `UndirectedGraph`).
- **Proof of the 2-approximation.** Every vertex cover touches every edge of `M`; because matched edges
  are vertex-disjoint this needs at least one *distinct* vertex per matched edge, and each forced
  self-loop vertex is mandatory and disjoint from matched endpoints, so
  `OPT ≥ |M| + |forced|`. The algorithm returns `|C| = 2|M| + |forced| ≤ 2(|M| + |forced|) ≤ 2·OPT`.
  Maximality ensures every non-loop edge touches a matched vertex, so `C` is a valid cover. Hence
  `|C| ≤ 2·OPT` — a worst-case guarantee, not a claim that every instance attains ratio 2.
- The `ApproximationResult` reports `matchingSize`, `forcedSize`, `lowerBound = |M| + |forced|` and
  `ratio = |C| / lowerBound ≤ 2`. The production class never computes `OPT`; optimum values exist only
  in the independent subset-enumeration oracle used by the tests.

### Ratio vocabulary

For a minimization problem with instance `I`: `A(I)` = algorithm output, `OPT(I)` = optimal value;
the approximation ratio is `A(I)/OPT(I)`, and a `ρ`-approximation guarantees `A(I)/OPT(I) ≤ ρ` for all
instances. Division by zero is avoided: the empty-graph case (both zero) is reported as ratio `1.0` by
convention.

---

## 5. PTAS, FPTAS, APX

- **PTAS** (polynomial-time approximation scheme): for every fixed `ε > 0`, a `(1+ε)`-approximation
  (minimization) or `(1−ε)` (maximization), with runtime polynomial in the input size **for each fixed
  ε**. The running time may be, e.g., exponential in `1/ε`. A PTAS is thus a *parameterized family of
  approximations*.
- **FPTAS** (fully polynomial-time approximation scheme): a PTAS whose runtime is also **polynomial in
  `1/ε`**. `KnapsackFPTAS` is an FPTAS: `O(n³/ε)` time with guarantee `A ≥ (1−ε)·OPT`.
- **APX**: the class of problems admitting a *constant-factor* approximation algorithm. Greedy Set
  Cover (`SetCoverDemo`) has the (non-constant in the worst case) `H(n) ≤ 1+ln n` bound and is in
  log-APX; Vertex Cover is in APX (factor 2); general Set Cover is **not** believed to admit a constant
  factor.
- Breadth of a guarantee: Vertex Cover's constant 2 bound is exactly what puts it in APX — **not** in
  PTAS/FPTAS (VC is NP-hard to approximate below ~1.36 by the ETH/PCP folklore, so no PTAS unless
  `P = NP`).

Distinguish the two guarantees: the **approximation guarantee** (quality ratio) and the **runtime
guarantee** (polynomiality, and in which parameters).

---

## 6. FPT (fixed-parameter tractable) algorithms

An algorithm with parameter `k` is FPT if its runtime is `f(k) · poly(n)` for a computable function
`f` depending only on `k`. Exact, not approximate, and independent of `n`.

`BoundedVertexCover` is the executable FPT example: branching on an edge `(u,v)`, every vertex cover of
size ≤ k must contain `u` or `v`, so one branch survives; `O(2^k(V+E))`. Do **not** confuse this exact
FPT decision with `VertexCoverApproximation`:

| | 2-approximation | FPT decision |
|---|---|---|
| Purpose | near-optimal solution, fast, any instance | exact yes/no for small `k` |
| Guarantee | `|C| ≤ 2·OPT` | exact (finds a cover of size ≤ k if one exists) |
| Runtime | `O(E)` | `O(2^k(V+E))` |

---

## 7. Kernelization

Kernelization = polynomial preprocessing that reduces an input `(G, k)` to an *equivalent* smaller
instance. `VertexCoverKernelization` implements two safe rules:

- **Self-loop rule**: a self-loop at `v` forces `v` into every cover (delete `v`, `k := k−1`).
- **Degree rule**: if `degree(v) > k`, `v` is in every cover of size ≤ k (otherwise its neighbours
  alone exceed the budget); delete `v`, `k := k−1`. Isolated vertices (degree 0) are inert and skipped.

These rules preserve the decision `tau(G) ≤ k` in both directions, and whenever the original instance
is a YES instance they also preserve the stronger identity `tau(G) = tau(reduced) + |forced|` (each
forced vertex provably lies in every cover of the current budget). The tests check the decision
equivalence on all random instances and the optimum identity on the YES ones, both against the exact
subset oracle applied to the original and the reduced graph. The documentation deliberately makes **no**
claim of a specific numeric kernel size (e.g. `O(k²)`) because achieving that requires the full
high-degree + crown machinery not implemented here — only the size-rule argument is delivered.
`KernelizationResult` reports the forced vertices, reduced graph, remaining `k`, and an
`infeasible` flag when the budget is already exhausted.

---

## 8. Exact vs approximation experiment

`ApproximationExperimentTest` is a deterministic, test-only comparison on fixed small graphs:
for each graph it computes (a) the exact minimum vertex cover (subset-enumeration oracle), (b) the
2-approximate cover and its ratio `approx/OPT`, and (c) the FPT decision `BoundedVertexCover.hasVertexCover(G, k)`
for several `k`. This makes the trade-off explicit:

> exact optimization is NP-hard (exponential in `n`), FPT is exact when `k` is small, and the
> 2-approximation runs in `O(E)` but only promises `|C| ≤ 2·OPT`.

No UI or benchmark integration is built in this phase.