import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import OverviewPage from './pages/OverviewPage';
import LogsPage from './pages/LogsPage';
import SearchPage from './pages/SearchPage';
import AnalyticsPage from './pages/AnalyticsPage';
import PatternsPage from './pages/PatternsPage';
import IncidentsPage from './pages/IncidentsPage';
import ServicesPage from './pages/ServicesPage';
import LivePage from './pages/LivePage';
import MonitorPage from './pages/MonitorPage';
import IncidentWorkbenchPage from './pages/IncidentWorkbenchPage';
import DatasetsPage from './pages/DatasetsPage';
import IngestionPage from './pages/IngestionPage';
import AnalysisPage from './pages/AnalysisPage';
import AlgorithmsPage from './pages/AlgorithmsPage';
import BenchmarksPage from './pages/BenchmarksPage';
import RunsPage from './pages/RunsPage';
import SystemPage from './pages/SystemPage';
import DocsPage from './pages/DocsPage';
import NotFoundPage from './pages/NotFoundPage';
import ScenarioLabPage from './pages/ScenarioLabPage';
import { ReplayProvider } from './replay/ReplayContext';
import { TelemetryProvider } from './telemetry/TelemetryContext';

export default function App() {
  return (
    <ReplayProvider>
      <TelemetryProvider>
        <BrowserRouter>
          <Routes>
        <Route element={<Layout />}>
      <Route path="/" element={<OverviewPage />} />
      <Route path="/command-center" element={<OverviewPage />} />
      <Route path="/overview" element={<OverviewPage />} />
          <Route path="/scenario-lab" element={<ScenarioLabPage />} />
          <Route path="/simulation" element={<ScenarioLabPage />} />
          <Route path="/logs" element={<LogsPage />} />
          <Route path="/logs/:id" element={<LogsPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/analyze" element={<AnalyticsPage />} />
          <Route path="/patterns" element={<PatternsPage />} />
          <Route path="/incidents/workbench" element={<IncidentWorkbenchPage />} />
          <Route path="/incidents" element={<IncidentsPage />} />
          <Route path="/incidents/:id" element={<IncidentsPage />} />
          <Route path="/investigate/:id" element={<IncidentsPage />} />
          <Route path="/services" element={<ServicesPage />} />
          <Route path="/services/:id" element={<ServicesPage />} />
          <Route path="/live" element={<MonitorPage />} />
          <Route path="/replay" element={<LivePage />} />
          <Route path="/datasets" element={<DatasetsPage />} />
          <Route path="/data" element={<DatasetsPage />} />
          <Route path="/ingestion" element={<IngestionPage />} />
          <Route path="/analysis" element={<AnalysisPage />} />
          <Route path="/lab" element={<AnalysisPage />} />
          <Route path="/algorithm-lab" element={<AnalysisPage />} />
          <Route path="/analysis/algorithms" element={<AlgorithmsPage />} />
          <Route path="/analysis/benchmarks" element={<BenchmarksPage />} />
          <Route path="/algorithms" element={<AlgorithmsPage />} />
          <Route path="/benchmarks" element={<BenchmarksPage />} />
          <Route path="/runs" element={<RunsPage />} />
          <Route path="/runs/:id" element={<RunsPage />} />
          <Route path="/system" element={<SystemPage />} />
          <Route path="/system/status" element={<SystemPage />} />
          <Route path="/docs" element={<DocsPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
        </BrowserRouter>
      </TelemetryProvider>
    </ReplayProvider>
  );
}
