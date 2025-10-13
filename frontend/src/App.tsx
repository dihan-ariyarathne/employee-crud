import { Navigate, Route, Routes } from 'react-router-dom';

import EmployeesPage from './pages/EmployeesPage.tsx';
import Login from './pages/Login.tsx';
import RequireAuth from './components/auth/RequireAuth.tsx';

function App() {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <RequireAuth>
            <EmployeesPage />
          </RequireAuth>
        }
      />
      <Route path="/login" element={<Login />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
