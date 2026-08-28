import { useId, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { publicApi } from '../../api/public.js';
import CardGrid from '../../components/CardGrid/CardGrid.jsx';
import EmptyState from '../../components/EmptyState/EmptyState.jsx';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Icon from '../../components/Icon/Icon.jsx';
import OfferCard from '../../components/OfferCard/OfferCard.jsx';
import PublicLayout from '../../components/PublicChrome/PublicLayout.jsx';
import Select from '../../components/Select/Select.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useResource from '../../hooks/useResource.js';
import './public.css';

const QUERY = 'q';
const DOMAIN = 'domaine';

const plural = (count, one, many) => `${count} ${count > 1 ? many : one}`;

/**
 * Every open position, narrowed by what a reader types and by the domain they
 * pick. Both filters apply at once and both live in the address, so a narrowed
 * listing can be shared and returned to.
 */
export default function PublicOffers() {
  useDocumentTitle('Offres');

  const searchId = useId();
  const [params, setParams] = useSearchParams();
  const query = params.get(QUERY) ?? '';
  const domain = params.get(DOMAIN) ?? '';

  const { status, data, reload, pending, leaving } = useResource(() => publicApi.market(), []);

  function write(key, value) {
    const updated = new URLSearchParams(params);
    if (value) {
      updated.set(key, value);
    } else {
      updated.delete(key);
    }
    setParams(updated, { replace: true });
  }

  const offers = data?.offers ?? [];
  const domains = data?.domains ?? [];

  const kept = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return offers.filter((offer) => {
      if (domain && !offer.domains.includes(domain)) return false;
      if (!needle) return true;
      return `${offer.title} ${offer.company}`.toLowerCase().includes(needle);
    });
  }, [offers, query, domain]);

  const filtered = Boolean(query.trim() || domain);

  return (
    <PublicLayout>
      <section className="pubband pubband--head">
        <div className="pubband__inner pubhead-band">
          <div className="pubhead-band__title">
            <h1 className="pubtitle">Offres</h1>
            {status === 'ready' && (
              <p className="pubtitle__note">
                {plural(offers.length, 'poste ouvert', 'postes ouverts')}
                {domains.length > 0 && ` · ${plural(domains.length, 'domaine', 'domaines')}`}
              </p>
            )}
          </div>

          {status === 'ready' && (
            <div className="pubfilters">
              <div className="pubfilters__field">
                <label className="field__label" htmlFor={searchId}>Titre ou entreprise</label>
                <div className="pubfilters__search">
                  <Icon name="search" className="pubfilters__icon" />
                  <input
                    id={searchId}
                    type="search"
                    className="field__input pubfilters__input"
                    value={query}
                    placeholder="Ingénieur, Bridge"
                    onChange={(event) => write(QUERY, event.target.value)}
                  />
                </div>
              </div>

              <Select
                label="Domaine"
                value={domain}
                onChange={(event) => write(DOMAIN, event.target.value)}
                options={domains.map((label) => ({ value: label, label }))}
                placeholder="Tous les domaines"
              />

              <p className="pubfilters__count" aria-live="polite">
                {plural(kept.length, 'offre', 'offres')}
              </p>
            </div>
          )}
        </div>
      </section>

      <section className="pubband pubband--list">
        <div className="pubband__inner">
          {pending && <Skeleton variant="cards" count={6} leaving={leaving} label="Chargement des offres" />}

          {status === 'error' && (
            <ErrorState onRetry={reload}>
              Les offres n'ont pas pu être chargées. Réessayez dans un instant.
            </ErrorState>
          )}

          {status === 'ready' && kept.length === 0 && (
            filtered ? (
              <EmptyState
                title="Aucune offre ne correspond"
                actionLabel="Effacer les filtres"
                onAction={() => setParams(new URLSearchParams(), { replace: true })}
              >
                Élargissez la recherche ou choisissez un autre domaine.
              </EmptyState>
            ) : (
              <EmptyState title="Aucun poste ouvert pour le moment">
                Revenez plus tard, ou créez un compte pour être prêt quand une offre paraîtra.
              </EmptyState>
            )
          )}

          {status === 'ready' && kept.length > 0 && (
            <CardGrid label="Offres ouvertes">
              {kept.map((offer) => (
                <OfferCard key={offer.id} offer={offer} to={`/emplois/${offer.id}`} />
              ))}
            </CardGrid>
          )}
        </div>
      </section>
    </PublicLayout>
  );
}
