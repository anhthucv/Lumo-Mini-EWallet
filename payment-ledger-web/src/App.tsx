import { Navigate, Route, Routes } from 'react-router-dom';

import RegisterPage from './pages/RegisterPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/register" replace />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="*" element={<Navigate to="/register" replace />} />
    </Routes>
  );
}
