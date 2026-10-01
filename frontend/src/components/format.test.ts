import { describe, expect, it } from 'vitest';
import { eventKey, formatDuration, formatMillis, formatNanos } from './format';

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

describe('eventKey', () => {
  it('keys ingested events on their dataset id', () => {
    expect(eventKey({ id: 7, timestamp: '2026-04-12T10:00:00Z' })).toBe('d7');
    expect(eventKey({ id: 0, timestamp: '2026-04-12T10:00:00Z' })).toBe('d0');
  });

  it('keeps generated events distinct even though they all carry the placeholder id', () => {
    const first = { id: -1, timestamp: '2026-04-12T10:00:00.250Z', requestId: 'req-a-1', message: 'a' };
    const second = { id: -1, timestamp: '2026-04-12T10:00:00.500Z', requestId: 'req-a-2', message: 'b' };
    expect(eventKey(first)).not.toBe(eventKey(second));
  });

  it('is stable for the same generated event', () => {
    const event = { id: -1, timestamp: '2026-04-12T10:00:00.250Z', requestId: 'req-a-1', message: 'a' };
    expect(eventKey(event)).toBe(eventKey({ ...event }));
  });
});
