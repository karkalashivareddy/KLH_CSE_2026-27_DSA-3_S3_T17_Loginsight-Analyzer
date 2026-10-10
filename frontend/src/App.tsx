import { lazy, Suspense, type ComponentType, type ReactNode } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import OverviewPage from './pages/OverviewPage';
import LogsPage from './pages/LogsPage';
import IncidentWorkbenchPage from './pages/IncidentWorkbenchPage';
import { ReplayProvider } from './replay/ReplayContext';
import { TelemetryProvider } from './telemetry/TelemetryContext';
import { GuidedDemoProvider } from './presentation/GuidedDemo';

/**
 * The command center, log explorer and incident workbench stay in the entry
 * chunk: they are the primary operational surfaces and a cold navigation to any
 * of them should not pay a network round trip.
 *
 * Everything else is split out. This matters most for the WebGL topology, which
 * is only reachable from the Services page and would otherwise add its whole
 * dependency graph to the first load.
 */
const SearchPage = lazyPage(() => import('./pages/SearchPage'));
const AnalyticsPage = lazyPage(() => import('./pages/AnalyticsPage'));
const PatternsPage = lazyPage(() => import('./pages/PatternsPage'));
const IncidentsPage = lazyPage(() => import('./pages/IncidentsPage'));
const ServicesPage = lazyPage(() => import('./pages/ServicesPage'));
const LivePage = lazyPage(() => import('./pages/LivePage'));
const MonitorPage = lazyPage(() => import('./pages/MonitorPage'));
const DatasetsPage = lazyPage(() => import('./pages/DatasetsPage'));
const IngestionPage = lazyPage(() => import('./pages/IngestionPage'));
const AnalysisPage = lazyPage(() => import('./pages/AnalysisPage'));
const AlgorithmsPage = lazyPage(() => import('./pages/AlgorithmsPage'));
const BenchmarksPage = lazyPage(() => import('./pages/BenchmarksPage'));
const RunsPage = lazyPage(() => import('./pages/RunsPage'));
const SystemPage = lazyPage(() => import('./pages/SystemPage'));
const DocsPage = lazyPage(() => import('./pages/DocsPage'));
const ScenarioLabPage = lazyPage(() => import('./pages/ScenarioLabPage'));
const NotFoundPage = lazyPage(() => import('./pages/NotFoundPage'));

function lazyPage(load: () => Promise<{ default: ComponentType }>): ComponentType {
  return lazy(load);
}

/**
 * Chunk placeholder.
 *
 * Deliberately keeps the page title visible rather than replacing the route
 * with a spinner, so navigating never leaves the workspace without context.
 * It is removed from the a11y tree so it is not announced as content.
 */
function RouteFallback() {
  return (
    <div className="route-fallback" aria-hidden="true">
      <div className="skeleton-line skeleton-line--wide" />
      <div className="skeleton-grid">
        {Array.from({ length: 3 }, (_, index) => <div key={index} className="skeleton-metric" />)}
      </div>
    </div>
  );
}

function withSuspense(element: ReactNode) {
  return <Suspense fallback={<RouteFallback />}>{element}</Suspense>;
}

export default function App() {
  return (
    <ReplayProvider>
      <TelemetryProvider>
        <BrowserRouter>
          <GuidedDemoProvider>
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<OverviewPage />} />
                <Route path="/command-center" element={<OverviewPage />} />
                <Route path="/overview" element={<OverviewPage />} />
                <Route path="/scenario-lab" element={withSuspense(<ScenarioLabPage />)} />
                <Route path="/simulation" element={withSuspense(<ScenarioLabPage />)} />
                <Route path="/logs" element={<LogsPage />} />
                <Route path="/logs/:id" element={<LogsPage />} />
                <Route path="/search" element={withSuspense(<SearchPage />)} />
                <Route path="/analytics" element={withSuspense(<AnalyticsPage />)} />
                <Route path="/analyze" element={withSuspense(<AnalyticsPage />)} />
                <Route path="/patterns" element={withSuspense(<PatternsPage />)} />
                <Route path="/incidents/workbench" element={<IncidentWorkbenchPage />} />
                <Route path="/incidents" element={withSuspense(<IncidentsPage />)} />
                <Route path="/incidents/:id" element={withSuspense(<IncidentsPage />)} />
                <Route path="/investigate/:id" element={withSuspense(<IncidentsPage />)} />
                <Route path="/services" element={withSuspense(<ServicesPage />)} />
                <Route path="/services/:id" element={withSuspense(<ServicesPage />)} />
                <Route path="/live" element={withSuspense(<MonitorPage />)} />
                <Route path="/replay" element={withSuspense(<LivePage />)} />
                <Route path="/datasets" element={withSuspense(<DatasetsPage />)} />
                <Route path="/data" element={withSuspense(<DatasetsPage />)} />
                <Route path="/ingestion" element={withSuspense(<IngestionPage />)} />
                <Route path="/analysis" element={withSuspense(<AnalysisPage />)} />
                <Route path="/lab" element={withSuspense(<AnalysisPage />)} />
                <Route path="/algorithm-lab" element={withSuspense(<AnalysisPage />)} />
                <Route path="/analysis/algorithms" element={withSuspense(<AlgorithmsPage />)} />
                <Route path="/analysis/benchmarks" element={withSuspense(<BenchmarksPage />)} />
                <Route path="/algorithms" element={withSuspense(<AlgorithmsPage />)} />
                <Route path="/benchmarks" element={withSuspense(<BenchmarksPage />)} />
                <Route path="/runs" element={withSuspense(<RunsPage />)} />
                <Route path="/runs/:id" element={withSuspense(<RunsPage />)} />
                <Route path="/system" element={withSuspense(<SystemPage />)} />
                <Route path="/system/status" element={withSuspense(<SystemPage />)} />
                <Route path="/docs" element={withSuspense(<DocsPage />)} />
                <Route path="*" element={withSuspense(<NotFoundPage />)} />
              </Route>
            </Routes>
          </GuidedDemoProvider>
        </BrowserRouter>
      </TelemetryProvider>
    </ReplayProvider>
  );
}