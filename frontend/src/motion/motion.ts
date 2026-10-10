import type { Transition, Variants } from 'motion/react';

/**
 * Motion tokens.
 *
 * These mirror the CSS custom properties of the same name in
 * `styles/signal-atlas.css`. JavaScript-driven motion reads them from here so a
 * timing change is made once per medium rather than per component.
 *
 * Durations deliberately mirror the brief's budget: fast interaction feedback
 * under 180ms, standard transitions under 350ms, larger scenes under 700ms.
 */
export const MOTION = {
  /** Press/hover acknowledgement. */
  fast: 0.16,
  /** Standard component and route transition. */
  normal: 0.26,
  /** Larger scene: cards entering, drawers, the guided demo. */
  slow: 0.42,
  /** Full-surface transitions such as the command palette. */
  scene: 0.62
} as const;

export const EASE = {
  /** Decelerating, the default for anything entering the viewport. */
  out: [0.22, 0.78, 0.28, 1],
  /** For elements that both enter and leave. */
  inOut: [0.6, 0.02, 0.3, 1]
} as const;

export const spring: Transition = { type: 'spring', stiffness: 420, damping: 34, mass: 0.8 };

/** One place to build the standard enter transition so pages stay declarative. */
export const fadeTransition: Transition = { duration: MOTION.slow, ease: EASE.out };

/** Route surfaces rise a few pixels; content itself never fades from 0. */
export const routeVariants: Variants = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0, transition: { duration: MOTION.slow, ease: EASE.out } },
  exit: { opacity: 0, y: -6, transition: { duration: MOTION.fast, ease: EASE.inOut } }
};

/** Groups stagger their children without any child needing its own delay. */
export function stagger(staggerChildren = 0.055, delayChildren = 0): Variants {
  return {
    animate: { transition: { staggerChildren, delayChildren } }
  };
}

export const itemVariants: Variants = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0, transition: { duration: MOTION.normal, ease: EASE.out } }
};

/**
 * Presence variants for overlays. Both the dialog and its backdrop share the
 * `ease` so they never land out of step.
 */
export const overlayVariants: Variants = {
  initial: { opacity: 0 },
  animate: { opacity: 1, transition: { duration: MOTION.normal, ease: EASE.out } },
  exit: { opacity: 0, transition: { duration: MOTION.fast, ease: EASE.inOut } }
};

export const surfaceVariants: Variants = {
  initial: { opacity: 0, y: 12, scale: 0.985 },
  animate: { opacity: 1, y: 0, scale: 1, transition: { duration: MOTION.normal, ease: EASE.out } },
  exit: { opacity: 0, y: 8, scale: 0.99, transition: { duration: MOTION.fast, ease: EASE.inOut } }
};