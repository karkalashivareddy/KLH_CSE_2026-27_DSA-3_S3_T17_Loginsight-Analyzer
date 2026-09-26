import { describe, expect, it } from 'vitest';
import { formatDuration, formatMillis, formatNanos } from './format';

describe('duration formatters', () => {
  it('treats duration values as milliseconds', () => {
    expect(formatDuration(12)).toBe('12 ms');
    expect(formatDuration(1_234)).toBe('1.2s');
    expect(formatDuration(123_456)).toBe('2m 3.5s');
  });

  it('treats nanosecond values as nanoseconds', () => {
    expect(formatNanos(12)).toBe('12 ns');
    expect(formatNanos(12_345)).toBe('12.3 µs');
    expect(formatNanos(1_200_000)).toBe('1.2 ms');
    expect(formatNanos(1_000_000_000)).toBe('1.00 s');
  });

  it('formats fractional millisecond measurements', () => {
    expect(formatMillis(0.5)).toBe('500 µs');
    expect(formatMillis(1_500)).toBe('1.50 s');
  });
});
