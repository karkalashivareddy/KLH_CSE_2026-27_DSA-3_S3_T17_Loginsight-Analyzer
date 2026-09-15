import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import OverviewPage from './pages/OverviewPage';
import LogsPage from './pages/LogsPage';
import AnalyticsPage from './pages/AnalyticsPage';
import DatasetsPage from './pages/DatasetsPage';
import LabPage from './pages/LabPage';
import BenchmarksPage from './pages/BenchmarksPage';
import SystemPage from './pages/SystemPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';

/**
 * Phase 13 application shell: routed pages driven entirely by the real backend API
 * (api/client.ts). Placeholder routes for Lab/Benchmarks are filled in Phases 14-15.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/"          element={<OverviewPage />} />
          <Route path="/logs"      element={<LogsPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/datasets"  element={<DatasetsPage />} />
          <Route path="/lab"       element={<LabPage />} />
          <Route path="/benchmarks" element={<BenchmarksPage />} />
          <Route path="/system"    element={<SystemPage />} />
          <Route path="/docs"      element={<DocsPage />} />
          <Route path="*"          element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}