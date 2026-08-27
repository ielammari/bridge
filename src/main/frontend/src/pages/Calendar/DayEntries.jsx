import { Link } from 'react-router-dom';
import { isTerminal } from '../../constants/enums.js';
import { clockTime } from '../../constants/format.js';
import { INTERVIEW } from '../../constants/navigation.js';

/**
 * A day's interviews, earliest first. A card is the whole target and carries
 * only who, what for and when; `showEvaluator` adds who runs it. What the
 * interview recorded is on the page it opens, and one already assessed reads
 * back rather than ahead.
 */
export default function DayEntries({ entries, role, showEvaluator, from }) {
  if (entries.length === 0) {
    return <p className="dayfeed__none">Rien de prévu ce jour.</p>;
  }

  return (
    <ol className="dayfeed">
      {entries.map((entry) => {
        const name = `${entry.candidateFirstName} ${entry.candidateLastName}`;
        const who = showEvaluator ? entry.evaluatorName : null;
        return (
          <li key={`${entry.applicationId}-${entry.type}`}>
            <Link
              className={`dayitem${entry.recorded ? ' dayitem--recorded' : ''}`}
              to={destination(entry, role)}
              state={{ from }}
              aria-label={`${clockTime(entry.time)}, ${name}, ${entry.offerTitle}`
                + (who ? `, expert technique ${who}` : '')
                + (entry.recorded ? ', évalué' : '')}
            >
              <span className="dayitem__hour mono" aria-hidden="true">{clockTime(entry.time)}</span>
              <span className="dayitem__body">
                <span className="dayitem__name">{name}</span>
                <span className="dayitem__offer">{entry.offerTitle}</span>
                {who && <span className="dayitem__who">Expert technique : {who}</span>}
              </span>
            </Link>
          </li>
        );
      })}
    </ol>
  );
}

/**
 * Where the interview lives: the card that acts on it, in the expert's queue or
 * the recruiter's listing, or the record, opened at the interview it names.
 */
function destination(entry, role) {
  if (inRecord(entry, role)) {
    return `/historique/candidatures/${entry.applicationId}?${INTERVIEW}=${entry.type}`;
  }
  if (role === 'EXPERT') return `/evaluations?fiche=${entry.applicationId}`;
  return `/candidatures?offre=${entry.offerId}&fiche=${entry.applicationId}`;
}

/**
 * Whether the record holds it. An expert's keeps every exam they assessed; a
 * recruiter's keeps the applications that closed, so an exam on one still
 * moving is on their listing.
 */
function inRecord(entry, role) {
  return role === 'EXPERT' ? entry.recorded : isTerminal(entry.applicationStatus);
}
