import { authFetch } from '../context/AuthContext';

const API_BASE_URL = '/api';

async function apiFetch(url, options = {}) {
  const response = await authFetch(url, options);

  if (!response.ok) {
    const text = await response.text();
    let message = response.statusText;
    try {
      message = JSON.parse(text).error || JSON.parse(text).message || message;
    } catch {
      message = text || response.statusText;
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

export const employeeApi = {
  async getAll() {
    return apiFetch(`${API_BASE_URL}/employees`);
  },

  async getById(id) {
    return apiFetch(`${API_BASE_URL}/employees/${id}`);
  },

  async create(employee) {
    return apiFetch(`${API_BASE_URL}/employees`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(employee),
    });
  },

  async update(id, employee) {
    return apiFetch(`${API_BASE_URL}/employees/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(employee),
    });
  },

  async delete(id) {
    return apiFetch(`${API_BASE_URL}/employees/${id}`, {
      method: 'DELETE',
    });
  },

  async getBuildings() {
    return apiFetch(`${API_BASE_URL}/buildings`);
  }
};

export const logApi = {
  async getEvents(page = 0, size = 50, type = null) {
    let url = `${API_BASE_URL}/logs?page=${page}&size=${size}`;
    if (type && type !== 'ALL') {
      url += `&type=${type}`;
    }
    return apiFetch(url);
  },

  async getEventTypes() {
    return apiFetch(`${API_BASE_URL}/logs/types`);
  }
};
