import { useEffect, useState } from 'react';
import { api } from '../../api/client.js';

// Displays backend and database availability.
export default function HealthCheck() {
  const [state, setState] = useState({ status: 'loading' });

  useEffect(() => {
    api
      .get('/health')
      .then((data) => setState({ status: 'ok', data }))
      .catch((error) => setState({ status: 'error', error }));
  }, []);

  if (state.status === 'loading') {
    return <main><p>Vérification de la connexion...</p></main>;
  }

  if (state.status === 'error') {
    return (
      <main>
        <p>Backend injoignable : {state.error.message}</p>
      </main>
    );
  }

  return (
    <main>
      <h1>Bridge</h1>
      <p>Application : {state.data.application}</p>
      <p>Base de données : {state.data.database}</p>
      <p>Traits en catalogue : {state.data.traitCount}</p>
    </main>
  );
}
