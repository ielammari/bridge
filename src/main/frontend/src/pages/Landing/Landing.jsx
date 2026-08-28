import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { publicApi } from '../../api/public.js';
import PublicLayout from '../../components/PublicChrome/PublicLayout.jsx';
import useDocumentTitle from '../../hooks/useDocumentTitle.js';
import useResource from '../../hooks/useResource.js';
import Closing from './Closing.jsx';
import Features from './Features.jsx';
import Funnel from './Funnel.jsx';
import Hero from './Hero.jsx';
import Inside from './Inside.jsx';
import OpenPositions from './OpenPositions.jsx';
import './landing.css';

/** The offer that shows the most of what an offer asks for. */
function richest(offers) {
  return offers.reduce(
    (best, offer) => (best === null || offer.domains.length > best.domains.length ? offer : best),
    null,
  );
}

/**
 * The public front. Every figure and every card on it is read from the offers
 * the application is publishing, so the page cannot claim more than there is.
 */
export default function Landing() {
  useDocumentTitle('Bridge, recrutement');

  const { hash, key } = useLocation();

  const { status, data, pending, leaving } = useResource(async () => {
    const market = await publicApi.market();
    const sample = richest(market.offers);
    // One offer read in full, so the matching rule is shown on a real one.
    const detail = sample === null ? null : await publicApi.offer(sample.id).catch(() => null);
    return { market, detail };
  }, []);

  // A link from another page arrives with the section named in the address.
  // `key` changes on a second click of the same link, `status` waits for the
  // sections above the target to have their real height.
  useEffect(() => {
    if (!hash) return;
    document.querySelector(hash)?.scrollIntoView({ block: 'start' });
  }, [hash, key, status]);

  const offers = data?.market.offers ?? [];
  const domains = data?.market.domains ?? [];
  const sample = data?.detail
    ? { ...data.detail.offer, requirements: data.detail.requirements }
    : null;

  return (
    <PublicLayout>
      <Hero offers={offers.length} domains={domains.length} ready={status === 'ready'} />
      <OpenPositions offers={offers} domains={domains} pending={pending} leaving={leaving} />
      <Features sample={sample} />
      <Funnel />
      <Inside />
      <Closing />
    </PublicLayout>
  );
}
