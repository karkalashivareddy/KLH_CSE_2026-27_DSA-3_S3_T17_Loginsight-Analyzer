import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, SseParser } from './client';

describe('SseParser', () => {
  it('parses chunked frames, comments, ids, and multiline data', () => {
    const parser = new SseParser();

    expect(parser.push(': keep-alive\n')).toEqual([]);
    expect(parser.push('event: step\r\nid: 7\r\ndata: first\r\ndata: sec')).toEqual([]);
    expect(parser.push('ond\r\n\r\n')).toEqual([
      { event: 'step', data: 'first\nsecond', id: '7' }
    ]);
  });

  it('flushes an unterminated final frame', () => {
    const parser = new SseParser();

    expect(parser.push('data: final')).toEqual([]);
    expect(parser.flush()).toEqual([{ event: 'message', data: 'final' }]);
  });
});

describe('dataset upload requests', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('lets the browser set the FormData content type boundary', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    const file = new File(['log line'], 'events.log', { type: 'text/plain' });

    await api.uploadDataset(file, 'sample');

    expect(fetchMock).toHaveBeenCalledOnce();
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers);
    const body = init.body as FormData;

    expect(url).toBe('/api/datasets');
    expect(init.method).toBe('POST');
    expect(headers.has('Content-Type')).toBe(false);
    expect(body).toBeInstanceOf(FormData);
    expect(body.get('file')).toBe(file);
    expect(body.get('name')).toBe('sample');
  });
});
