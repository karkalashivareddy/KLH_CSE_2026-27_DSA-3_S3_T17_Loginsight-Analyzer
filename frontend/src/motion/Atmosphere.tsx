import { useRef } from 'react';
import { motion, useReducedMotion, useScroll, useTransform } from 'motion/react';

/**
 * The atmospheric backdrop.
 *
 * A decorative light field that drifts at a different rate from the page so the
 * interface reads as sitting inside a medium rather than on a flat plane.
 *
 * Performance: the two layers are driven by `useScroll`, which writes to
 * compositor-only motion values. No React component re-renders while scrolling,
 * and no scroll listener is registered by this component.
 *
 * Accessibility: purely decorative (`aria-hidden`), removed entirely under
 * `prefers-reduced-motion: reduce`, and never positioned above page content —
 * `Layout` renders it behind the shell.
 */
export default function Atmosphere() {
  const reduced = useReducedMotion();
  const veilRef = useRef<HTMLDivElement>(null);
  const contoursRef = useRef<SVGSVGElement>(null);
  const { scrollYProgress } = useScroll();

  // Two different rates: the veil leads, the contours lag. Both stay subtle.
  const veilY = useTransform(scrollYProgress, [0, 1], ['0%', '-14%']);
  const contoursY = useTransform(scrollYProgress, [0, 1], ['0%', '-30%']);
  const lightX = useTransform(scrollYProgress, [0, 0.5, 1], ['18%', '62%', '34%']);
  const veilOpacity = useTransform(scrollYProgress, [0, 0.35, 1], [0.95, 0.72, 0.5]);

  if (reduced) return null;

  return (
    <div className="atmosphere" aria-hidden="true">
      <motion.div
        ref={veilRef}
        className="atmosphere__veil"
        style={{ y: veilY, left: lightX, opacity: veilOpacity }}
      />
      <motion.svg
        ref={contoursRef}
        className="atmosphere__contours"
        viewBox="0 0 1600 900"
        preserveAspectRatio="xMidYMid slice"
        style={{ y: contoursY }}
      >
        {/* Isobars: pressure contours that read as weather, not as decoration. */}
        <path d="M-80 250 C 240 120, 520 330, 840 210 S 1380 120, 1700 260" />
        <path d="M-80 340 C 250 210, 540 420, 860 300 S 1390 210, 1700 350" />
        <path d="M-80 430 C 260 300, 560 510, 880 390 S 1400 300, 1700 440" />
        <path d="M-80 520 C 270 390, 580 600, 900 480 S 1410 390, 1700 530" />
        <path d="M-80 610 C 280 480, 600 690, 920 570 S 1420 480, 1700 620" />
        <path d="M-80 700 C 290 570, 620 780, 940 660 S 1430 570, 1700 710" />
      </motion.svg>
    </div>
  );
}