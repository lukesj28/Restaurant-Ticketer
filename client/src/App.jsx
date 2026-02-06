import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import './App.css';
import Navbar from './components/layout/Navbar';
import Tickets from './pages/Tickets';
import TicketDetail from './pages/TicketDetail';
import Menu from './pages/Menu';
import Analysis from './pages/Analysis';
import Settings from './pages/Settings';
import { ToastProvider } from './context/ToastContext';

function App() {
  return (
    <ToastProvider>
      <Router>
        <div className="app-container">
          <div className="content">
            <Routes>
              <Route path="/" element={<Navigate to="/tickets" replace />} />
              <Route path="/tickets" element={<Tickets />} />
              <Route path="/tickets/:id" element={<TicketDetail />} />
              <Route path="/menu" element={<Menu />} />
              <Route path="/analysis" element={<Analysis />} />
              <Route path="/settings" element={<Settings />} />
            </Routes>
          </div>
          <Navbar />
        </div>
      </Router>
    </ToastProvider>
  );
}

export default App;
