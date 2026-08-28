import { Link } from 'react-router-dom';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';

// What the strip holds before it sends the reader to the whole listing.
const SHOWN = 6;

/**
 * The open positions themselves, as early on the page as they can be: the cards
 * are the application's own, carrying the offers it is publishing right now.
 */
export default function OpenPositions({ offers, domains, pending, leaving }) {
  const shown = offers.slice(0, SHOWN);

  return (
    <section className="pubband positions">
      <div className="pubband__inner">
        <div className="section__head">
          <p className="section__eyebrow">Ce qui recrute</p>
          <h2 className="section__title">Les postes ouverts</h2>
        </div>

        {domains.length > 0 && (
          <ul className="positions__domains" aria-label="Domaines des offres ouvertes">
            {domains.map((domain) => (
              <li key={domain}>
                <Link className="positions__domain" to={`/emplois?domaine=${encodeURIComponent(domain)}`}>
                  {domain}
                </Link>
              </li>
            ))}
          </ul>
        )}

        {pending && <Skeleton count={3} leaving={leaving} label="Chargement des offres" />}

        {shown.length > 0 && (
          <CardGrid label="Offres ouvertes">
            {shown.map((offer) => (
              <OfferCard key={offer.id} offer={offer} to={`/emplois/${offer.id}`} />
            ))}
          </CardGrid>
        )}

        {shown.length > 0 && (
          <p className="positions__more">
            <Link to="/emplois">
              Voir {offers.length > SHOWN ? `les ${offers.length} offres` : 'toutes les offres'}
            </Link>
          </p>
        )}
      </div>
    </section>
  );
}
