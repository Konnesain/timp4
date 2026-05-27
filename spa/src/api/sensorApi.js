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

export const sensorApi = {
  async getAll() {
    return apiFetch(`${API_BASE_URL}/sensors`);
  },
  async getByBuilding(buildingId) {
    return apiFetch(`${API_BASE_URL}/sensors/building/${buildingId}`);
  },
};
