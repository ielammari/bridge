import OfferLink from '../OfferLink/OfferLink.jsx';
import { CONTRACT_LABELS } from '../../constants/enums.js';
import { salaryText } from '../../constants/format.js';
import './OfferCard.css';

/**
 * One offer in a listing: what identifies the position, and nothing else. The
 * description and the traits belong to the offer's own page. `badge` and
 * `children` are the caller's, since each role acts on an offer differently,
 * and `to` names where the title leads for a reader outside the application.
 */
export default function OfferCard({ offer, to, badge, children }) {
  const salary = salaryText(offer.salaryMin, offer.salaryMax);

  return (
    <li className="tile tile--openable offercard">
      <div className="offercard__head">
        <h2 className="tile__title offercard__title">
          <OfferLink id={offer.id} to={to} className="tile__stretch">{offer.title}</OfferLink>
        </h2>
        {badge}
      </div>

      <p className="offercard__where">
        <span className="offercard__company">{offer.company}</span>
        {offer.location && <span className="offercard__location">{offer.location}</span>}
      </p>

      <div className="offercard__terms">
        <span className="offercard__contract">{CONTRACT_LABELS[offer.contractType]}</span>
        {salary && <span className="offercard__salary mono">{salary}</span>}
      </div>

      {children && <div className="tile__foot">{children}</div>}
    </li>
  );
}
