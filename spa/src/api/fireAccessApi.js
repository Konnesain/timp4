import { authFetch } from '../context/AuthContext';

const API_BASE_URL = '/api';

async function apiFetch(url, options = {}) {
  const response = await authFetch(url, options);

  if (!response.ok) {
    const text = await response.text();
    let message = response.statusText;
    try { message = JSON.parse(text).error || JSON.parse(text).message || message; } catch {}
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const fireAccessApi = {
  async getAll() {
    return apiFetch(`${API_BASE_URL}/fire-access`);
  },
  async create(data) {
    return apiFetch(`${API_BASE_URL}/fire-access`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },
  async update(id, data) {
    return apiFetch(`${API_BASE_URL}/fire-access/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },
  async delete(id) {
    return apiFetch(`${API_BASE_URL}/fire-access/${id}`, { method: 'DELETE' });
  },
};
