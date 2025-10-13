import { Navigate, useLocation } from 'react-router-dom';
import LoadingState from '../states/LoadingState.tsx';
import { useAuth } from '../../context/AuthProvider.tsx';

function RequireAuth({ children }: { children: JSX.Element }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <LoadingState />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

export default RequireAuth;

