import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { offersApi } from '../../api/offers.js';
import { traitsApi } from '../../api/traits.js';
import ErrorState from '../../components/ErrorState/ErrorState.jsx';
import Skeleton from '../../components/Skeleton/Skeleton.jsx';
import { useToast } from '../../components/Toast/ToastContext.jsx';
import useResource from '../../hooks/useResource.js';
import Workspace from '../Workspace/Workspace.jsx';
import OfferForm from './OfferForm.jsx';

const BACK = { to: '/offres', label: 'Retour aux offres' };

/** Creating and editing an offer, each at its own address. */
export default function OfferEditor({ mode }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const [submitting, setSubmitting] = useState(false);

  const { status, data, reload } = useResource(
    async () => {
      const [catalogue, offer] = await Promise.all([
        traitsApi.catalogue(),
        mode === 'edit' ? offersApi.get(id) : Promise.resolve(null),
      ]);
      return { catalogue, offer };
    },
    [id, mode],
  );

  async function submit(payload) {
    setSubmitting(true);
    try {
      if (mode === 'edit') {
        await offersApi.update(id, payload);
        toast.success('Offre mise à jour.');
      } else {
        await offersApi.create(payload);
        toast.success(payload.publishNow ? 'Offre publiée.' : 'Brouillon enregistré.');
      }
      navigate('/offres');
    } finally {
      setSubmitting(false);
    }
  }

  const title = mode === 'edit' ? 'Modifier l\'offre' : 'Nouvelle offre';

  if (status !== 'ready') {
    return (
      <Workspace title={title} back={BACK}>
        {status === 'loading' ? (
          <Skeleton count={2} label="Chargement du formulaire" />
        ) : (
          <ErrorState onRetry={reload}>
            Le formulaire n'a pas pu être chargé. Réessayez dans un instant.
          </ErrorState>
        )}
      </Workspace>
    );
  }

  return (
    <Workspace title={title} subtitle={data.offer?.title} back={BACK}>
      <OfferForm
        mode={mode}
        offer={data.offer}
        catalogue={data.catalogue}
        submitting={submitting}
        onSubmit={submit}
        onCancel={() => navigate('/offres')}
      />
    </Workspace>
  );
}
