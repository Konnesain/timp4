import React, { useState, useContext, createContext } from 'react';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

let refreshPromise = null;

export async function tryRefresh() {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const response = await fetch('/api/auth/refresh', { method: 'POST' });
      if (!response.ok) return false;

      const data = await response.json();
      localStorage.setItem('authUser', JSON.stringify({ username: data.username }));
      return true;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

export async function authFetch(url, options = {}) {
  let response = await fetch(url, options);

  if (response.status === 401 || response.status === 403) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      response = await fetch(url, options);
    } else {
      localStorage.removeItem('authUser');
      window.location.href = '/login';
    }
  }

  return response;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('authUser');
    return saved ? JSON.parse(saved) : null;
  });

  const login = async (username, password) => {
    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);

    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Ошибка входа');
    }

    setUser({ username: data.username });
    localStorage.setItem('authUser', JSON.stringify({ username: data.username }));
    return data;
  };

  const register = async (username, password) => {
    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);

    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.message || 'Ошибка регистрации');
    }

    setUser({ username: data.username });
    localStorage.setItem('authUser', JSON.stringify({ username: data.username }));
    return data;
  };

  const logout = async () => {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } catch {}
    setUser(null);
    localStorage.removeItem('authUser');
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
