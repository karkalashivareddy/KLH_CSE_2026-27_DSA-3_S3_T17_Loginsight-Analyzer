import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle, RotateCcw } from 'lucide-react';

interface Props {
  children: ReactNode;
  /** Shown in the recovery panel so the user knows what stopped working. */
  scope: string;
}

interface State {
  error: Error | null;
}

/**
 * Last line of defence for the workspace.
 *
 * Without this, any render-time failure unmounts the entire tree and leaves a
 * blank page with no message and no recovery path. A reachable-but-failing
 * backend must never be able to produce that.
 */
export default class AppErrorBoundary extends Component<Props, State> {
  override state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    // Kept in the console for developers; the UI never shows a stack trace.
    console.error(`[loginsight] ${this.props.scope} failed to render`, error, info.componentStack);
  }

  private readonly retry = () => this.setState({ error: null });

  override render(): ReactNode {
    const { error } = this.state;
    if (!error) return this.props.children;

    return (
      <div className="crash-screen" role="alert">
        <div className="crash-card">
          <span className="crash-mark" aria-hidden="true"><AlertTriangle size={22} /></span>
          <span className="eyebrow">Workspace interrupted</span>
          <h1>{this.props.scope} could not be displayed</h1>
          <p>{error.message}</p>
          <p className="crash-note">
            Your data was not changed. Retry the view, or reload the workspace if the backend is
            still unavailable.
          </p>
          <div className="crash-actions">
            <button className="btn btn-primary" type="button" onClick={this.retry}>
              <RotateCcw size={14} aria-hidden="true" /> Try again
            </button>
            <button className="btn" type="button" onClick={() => window.location.reload()}>
              Reload workspace
            </button>
          </div>
        </div>
      </div>
    );
  }
}