import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import OverviewPage from './pages/OverviewPage';
import LogsPage from './pages/LogsPage';
import SearchPage from './pages/SearchPage';
import AnalyticsPage from './pages/AnalyticsPage';
import PatternsPage from './pages/PatternsPage';
import IncidentsPage from './pages/IncidentsPage';
import ServicesPage from './pages/ServicesPage';
import LivePage from './pages/LivePage';
import DatasetsPage from './pages/DatasetsPage';
import IngestionPage from './pages/IngestionPage';
import AnalysisPage from './pages/AnalysisPage';
import AlgorithmsPage from './pages/AlgorithmsPage';
import BenchmarksPage from './pages/BenchmarksPage';
import RunsPage from './pages/RunsPage';
import SystemPage from './pages/SystemPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';

/**
 * LogInsight Analyzer application shell. Every page drives the real backend API (api/client.ts) —
 * the DSA string/pattern engines power the search and analytics layer behind the product UI.
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/"                       element={<OverviewPage />} />
          <Route path="/logs"                   element={<LogsPage />} />
          <Route path="/logs/:id"               element={<LogsPage />} />
          <Route path="/search"                 element={<SearchPage />} />
          <Route path="/analytics"              element={<AnalyticsPage />} />
          <Route path="/patterns"               element={<PatternsPage />} />
          <Route path="/incidents"              element={<IncidentsPage />} />
          <Route path="/services"               element={<ServicesPage />} />
          <Route path="/services/:name"         element={<ServicesPage />} />
          <Route path="/live"                   element={<LivePage />} />
          <Route path="/datasets"               element={<DatasetsPage />} />
          <Route path="/ingestion"              element={<IngestionPage />} />
          <Route path="/analysis"               element={<AnalysisPage />} />
          <Route path="/analysis/algorithms"    element={<AlgorithmsPage />} />
          <Route path="/analysis/benchmarks"    element={<BenchmarksPage />} />
          <Route path="/runs"                   element={<RunsPage />} />
          <Route path="/system"                 element={<SystemPage />} />
          <Route path="/docs"                   element={<DocsPage />} />
          <Route path="*"                       element={<NotFoundPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}