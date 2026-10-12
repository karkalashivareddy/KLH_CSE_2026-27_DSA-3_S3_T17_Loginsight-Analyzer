import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import AppErrorBoundary from './components/AppErrorBoundary';
import './styles/signal-atlas.css';

const container = document.getElementById('root');
if (!container) throw new Error('LogInsight could not start: #root is missing from index.html.');

ReactDOM.createRoot(container).render(
  <React.StrictMode>
    <AppErrorBoundary scope="LogInsight">
      <App />
    </AppErrorBoundary>
  </React.StrictMode>
);