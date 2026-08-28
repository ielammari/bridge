import { Link } from 'react-router-dom';
import { DEGREE_LABELS } from '../../constants/enums.js';
import './MatchNote.css';

/** Why an offer is out of reach: the level it asks for, the required traits the
 *  candidate does not hold, or both. */
function reasonsFor(match, requiredDegree) {
  const reasons = [];
  if (!match.degreeMet) {
    reasons.push(`niveau ${DEGREE_LABELS[requiredDegree] ?? requiredDegree} demandé`);
  }
  if (match.missingTraits.length > 0) {
    reasons.push(`il vous manque ${match.missingTraits.join(', ')}`);
  }
  return reasons;
}

/**
 * That an offer is out of reach, on a card that previews it. What it lacks
 * waits on the offer's own page.
 */
export function MatchMark({ match }) {
  if (!match || match.compatible) return null;
  return <span className="matchmark">Incompatible</span>;
}

/**
 * The same state on the offer's page, where it says what is missing and leads
 * to the profile that declares it.
 */
export default function MatchNote({ match, requiredDegree }) {
  if (!match || match.compatible) return null;

  return (
    <p className="matchnote">
      <span className="matchnote__mark">Incompatible</span>
      <span className="matchnote__why">{reasonsFor(match, requiredDegree).join(' · ')}</span>
      <Link to="/profil" className="matchnote__fix">Compléter mon profil</Link>
    </p>
  );
}
