import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './pages/auth/Login.jsx';
import Signup from './pages/auth/Signup.jsx';
import Profile from './pages/Profile/Profile.jsx';
import OffersPage from './pages/Offers/OffersPage.jsx';
import CandidateApplications from './pages/Applications/CandidateApplications.jsx';
import HrApplications from './pages/Applications/HrApplications.jsx';
import Workspace from './pages/Workspace/Workspace.jsx';
import ProtectedRoute, { HOME_BY_ROLE } from './components/ProtectedRoute/ProtectedRoute.jsx';
import { useAuth } from './context/AuthContext.jsx';

// Sends an already signed in visitor to their own workspace.
function Entry() {
  const { user, loading } = useAuth();
  if (loading) return null;
  return <Navigate to={user ? HOME_BY_ROLE[user.role] : '/connexion'} replace />;
}

// Keeps a signed in user off the auth pages.
function GuestOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  return user ? <Navigate to={HOME_BY_ROLE[user.role]} replace /> : children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Entry />} />

      <Route
        path="/connexion"
        element={
          <GuestOnly>
            <Login />
          </GuestOnly>
        }
      />
      <Route
        path="/inscription"
        element={
          <GuestOnly>
            <Signup />
          </GuestOnly>
        }
      />

      <Route
        path="/offres"
        element={
          <ProtectedRoute roles={['CANDIDAT', 'RH']}>
            <OffersPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profil"
        element={
          <ProtectedRoute roles={['CANDIDAT']}>
            <Profile />
          </ProtectedRoute>
        }
      />
      <Route
        path="/mes-candidatures"
        element={
          <ProtectedRoute roles={['CANDIDAT']}>
            <CandidateApplications />
          </ProtectedRoute>
        }
      />
      <Route
        path="/candidatures"
        element={
          <ProtectedRoute roles={['RH']}>
            <HrApplications />
          </ProtectedRoute>
        }
      />
      <Route
        path="/evaluations"
        element={
          <ProtectedRoute roles={['EXPERT']}>
            <Workspace title="Évaluations" />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
