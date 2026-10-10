import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import AppErrorBoundary from './AppErrorBoundary';

function Boom({ shouldThrow }: { shouldThrow: boolean }): JSX.Element {
  if (shouldThrow) throw new Error('render failed in the workspace');
  return <p>workspace content</p>;
}

describe('AppErrorBoundary', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders children while nothing throws', () => {
    render(<AppErrorBoundary scope="LogInsight"><Boom shouldThrow={false} /></AppErrorBoundary>);
    expect(screen.getByText('workspace content')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('replaces a crashed workspace with a recoverable alert instead of a blank page', async () => {
    // React logs the caught error; keep the test output readable.
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(<AppErrorBoundary scope="LogInsight"><Boom shouldThrow /></AppErrorBoundary>);

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /could not be displayed/i })).toBeInTheDocument();
    expect(screen.getByText('render failed in the workspace')).toBeInTheDocument();
    // The recovery surface must offer a way forward, not just an explanation.
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /reload workspace/i })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /try again/i }));
    // Retrying re-renders the subtree, which throws again and re-shows the alert.
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('never renders a stack trace to the user', () => {
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
    render(<AppErrorBoundary scope="LogInsight"><Boom shouldThrow /></AppErrorBoundary>);
    expect(document.body.textContent).not.toMatch(/at\s+\w+\s+\(/);
    expect(document.body.textContent).not.toContain('ErrorBoundary');
  });
});