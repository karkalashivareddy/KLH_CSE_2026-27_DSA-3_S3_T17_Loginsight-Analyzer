import { PROJECT, type BuildStage } from './types/project';

const stage: BuildStage = {
  phase: 'Phase 1 - Project Skeleton',
  description:
    'Buildable foundation only. Log engine, DSA modules, REST API and the dashboard land in later phases.'
};

/**
 * Minimal application shell for the Phase 1 skeleton. Proves the React + TypeScript + Vite
 * toolchain builds and renders. Navigation, pages and visualizations arrive from Phase 11 onward.
 */
export default function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>{PROJECT.name}</h1>
        <p>{PROJECT.tagline}</p>
      </header>

      <main className="app-main">
        <section className="status-card">
          <span className="status-dot" aria-hidden="true" />
          <div>
            <strong>{stage.phase}</strong>
            <p>{stage.description}</p>
          </div>
        </section>
      </main>

      <footer className="app-footer">
        <span>v{PROJECT.version}</span>
        <span>React + TypeScript + Vite</span>
      </footer>
    </div>
  );
}