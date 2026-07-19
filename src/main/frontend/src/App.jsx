import { Routes, Route } from 'react-router-dom';
import HealthCheck from './pages/HealthCheck/HealthCheck.jsx';

// Application route table.
export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HealthCheck />} />
    </Routes>
  );
}
