import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import OverviewPage from './pages/OverviewPage';
import LogsPage from './pages/LogsPage';
import AnalyticsPage from './pages/AnalyticsPage';
import DatasetsPage from './pages/DatasetsPage';
import LabPage from './pages/LabPage';
import TextHackPage from './pages/TextHackPage';
import RunsPage from './pages/RunsPage';
import CourseMapPage from './pages/CourseMapPage';
import BenchmarksPage from './pages/BenchmarksPage';
import SystemPage from './pages/SystemPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';

/**
 * TextHack application shell. Every page drives the real backend API (api/client.ts) — catalog
 * modules, TextHack queries, recorded runs with SSE replay, and the six module laboratories.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/"             element={<OverviewPage />} />
          <Route path="/text-hack"    element={<TextHackPage />} />
          <Route path="/labs"         element={<LabPage />} />
          <Route path="/labs/:key"    element={<LabPage />} />
          <Route path="/labs/:module/:key" element={<LabPage />} />
          <Route path="/runs"         element={<RunsPage />} />
          <Route path="/course-map"   element={<CourseMapPage />} />
          <Route path="/logs"         element={<LogsPage />} />
          <Route path="/analytics"    element={<AnalyticsPage />} />
          <Route path="/datasets"     element={<DatasetsPage />} />
          <Route path="/benchmarks"   element={<BenchmarksPage />} />
          <Route path="/system"       element={<SystemPage />} />
          <Route path="/docs"         element={<DocsPage />} />
          <Route path="*"             element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}