/**
 * Static project identity shared by the app shell. Expanded into full API/data contracts in
 * later phases (types/api.d.ts, types/algorithms.d.ts, ... per docs/03 §3).
 */
export const PROJECT = {
  name: 'LogInsight Analyzer',
  tagline: 'DSA-3 Advanced Algorithmic Log Intelligence and Text Analytics System',
  version: '0.1.0'
} as const;

/** Summarizes the current build stage for the shell status footer. */
export interface BuildStage {
  phase: string;
  description: string;
}