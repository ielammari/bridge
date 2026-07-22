import { useAuth } from '../../context/AuthContext.jsx';
import CandidateFeed from './CandidateFeed.jsx';
import HrOffers from './HrOffers.jsx';

// The offers surface differs by role: candidates get the matched feed, HR gets
// the management view.
export default function OffersPage() {
  const { user } = useAuth();
  return user.role === 'RH' ? <HrOffers /> : <CandidateFeed />;
}
