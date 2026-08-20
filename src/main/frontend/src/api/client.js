// Single entry point for every backend call.
// It attaches the JWT and normalizes the backend error shape, so no page or
// hook ever calls fetch directly.

const BASE_URL = '/api/v1';

let tokenProvider = () => null;
let onUnauthorized = () => {};
let onMutation = () => {};

// The auth context installs its token getter here.
export function setTokenProvider(provider) {
  tokenProvider = provider;
}

// Called when a request that carried a token is refused. A rejected login is
// not this: no token goes out with it.
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

// Called after any successful write. Funnel actions settle notifications as a
// side effect, so listeners can refresh state no page knows has changed.
export function setMutationHandler(handler) {
  onMutation = handler;
}

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

async function request(path, { method = 'GET', body, headers = {} } = {}) {
  const token = tokenProvider();

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && token) {
    onUnauthorized();
  }

  if (response.ok && method !== 'GET') {
    onMutation();
  }

  if (response.status === 204) {
    return null;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new ApiError(
      response.status,
      payload?.code ?? 'UNKNOWN_ERROR',
      payload?.message ?? 'Une erreur est survenue. Veuillez réessayer.',
    );
  }

  return payload;
}

// Fetches a binary body with the JWT attached, for downloads an anchor tag
// cannot authenticate on its own.
async function requestBlob(path) {
  const token = tokenProvider();
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (response.status === 401 && token) {
    onUnauthorized();
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      payload?.code ?? 'UNKNOWN_ERROR',
      payload?.message ?? 'Une erreur est survenue. Veuillez réessayer.',
    );
  }
  return response.blob();
}

export const api = {
  get: (path) => request(path),
  getBlob: (path) => requestBlob(path),
  post: (path, body) => request(path, { method: 'POST', body }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  patch: (path, body) => request(path, { method: 'PATCH', body }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
