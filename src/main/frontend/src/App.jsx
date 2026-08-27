import { Navigate, Route, Routes } from 'react-router-dom';
import Login from './pages/auth/Login.jsx';
import Signup from './pages/auth/Signup.jsx';
import ForcedPassword from './pages/auth/ForcedPassword.jsx';
import Profile from './pages/Profile/Profile.jsx';
import OffersPage from './pages/Offers/OffersPage.jsx';
import OfferEditor from './pages/Offers/OfferEditor.jsx';
import CandidateApplications from './pages/Applications/CandidateApplications.jsx';
import HrApplications from './pages/Applications/HrApplications.jsx';
import FinalEvaluationPage from './pages/Applications/FinalEvaluationPage.jsx';
import TechnicalEvaluations from './pages/Evaluations/TechnicalEvaluations.jsx';
import TechnicalEvaluation from './pages/Evaluations/TechnicalEvaluation.jsx';
import Messages from './pages/Messages/Messages.jsx';
import PastApplications from './pages/History/PastApplications.jsx';
import PastOffers from './pages/History/PastOffers.jsx';
import Hirings from './pages/History/Hirings.jsx';
import PastEvaluations from './pages/History/PastEvaluations.jsx';
import ApplicationRecord from './pages/History/ApplicationRecord.jsx';
import OfferPage from './pages/Offers/OfferPage.jsx';
import PersonPage from './pages/People/PersonPage.jsx';
import SavedOffers from './pages/Offers/SavedOffers.jsx';
import Settings from './pages/Settings/Settings.jsx';
import NotFound from './pages/NotFound/NotFound.jsx';
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

      <Route path="/connexion" element={<GuestOnly><Login /></GuestOnly>} />
      <Route path="/inscription" element={<GuestOnly><Signup /></GuestOnly>} />

      {/* The one address an account owing its first password change can reach. */}
      <Route
        path="/mot-de-passe"
        element={
          <ProtectedRoute>
            <ForcedPassword />
          </ProtectedRoute>
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
        path="/offres/nouvelle"
        element={
          <ProtectedRoute roles={['RH']}>
            <OfferEditor mode="create" />
          </ProtectedRoute>
        }
      />
      <Route
        path="/offres/:id/modifier"
        element={
          <ProtectedRoute roles={['RH']}>
            <OfferEditor mode="edit" />
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
        path="/candidatures/:id/entretien"
        element={
          <ProtectedRoute roles={['RH']}>
            <FinalEvaluationPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/evaluations"
        element={
          <ProtectedRoute roles={['EXPERT']}>
            <TechnicalEvaluations />
          </ProtectedRoute>
        }
      />
      <Route
        path="/evaluations/:id"
        element={
          <ProtectedRoute roles={['EXPERT']}>
            <TechnicalEvaluation />
          </ProtectedRoute>
        }
      />

      <Route
        path="/messages"
        element={
          <ProtectedRoute>
            <Messages />
          </ProtectedRoute>
        }
      />

      <Route
        path="/historique/candidatures"
        element={
          <ProtectedRoute roles={['CANDIDAT', 'RH']}>
            <PastApplications />
          </ProtectedRoute>
        }
      />
      <Route
        path="/historique/candidatures/:id"
        element={
          <ProtectedRoute>
            <ApplicationRecord />
          </ProtectedRoute>
        }
      />
      <Route
        path="/historique/offres"
        element={
          <ProtectedRoute roles={['RH']}>
            <PastOffers />
          </ProtectedRoute>
        }
      />
      <Route
        path="/historique/embauches"
        element={
          <ProtectedRoute roles={['RH']}>
            <Hirings />
          </ProtectedRoute>
        }
      />
      <Route
        path="/historique/evaluations"
        element={
          <ProtectedRoute roles={['RH', 'EXPERT']}>
            <PastEvaluations />
          </ProtectedRoute>
        }
      />

      {/* Open to every role: the service decides who may read whom, so a
          candidate reaching their own address is the same request as HR
          reaching anyone's. */}
      <Route
        path="/offres/enregistrees"
        element={
          <ProtectedRoute roles={['CANDIDAT']}>
            <SavedOffers />
          </ProtectedRoute>
        }
      />

      <Route
        path="/offres/:id"
        element={
          <ProtectedRoute>
            <OfferPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/personnes/:id"
        element={
          <ProtectedRoute>
            <PersonPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/parametres"
        element={
          <ProtectedRoute>
            <Settings />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
