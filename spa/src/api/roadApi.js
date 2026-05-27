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

export const roadApi = {
  async getAll() {
    return apiFetch(`${API_BASE_URL}/roads`);
  },
  async create(data) {
    return apiFetch(`${API_BASE_URL}/roads`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },
  async update(id, data) {
    return apiFetch(`${API_BASE_URL}/roads/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },
  async delete(id) {
    return apiFetch(`${API_BASE_URL}/roads/${id}`, { method: 'DELETE' });
  },
};
