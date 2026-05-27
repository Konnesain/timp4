import React, { useState, useContext, createContext } from 'react';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export async function tryRefresh() {
  const refreshToken = localStorage.getItem('authRefreshToken');
  if (!refreshToken) return false;

  try {
    const params = new URLSearchParams();
    params.append('refreshToken', refreshToken);

    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString(),
    });

    if (!response.ok) return false;

    const data = await response.json();
    localStorage.setItem('authToken', data.token);
    localStorage.setItem('authRefreshToken', data.refreshToken);
    localStorage.setItem('authUser', JSON.stringify({ username: data.username }));
    return true;
  } catch {
    return false;
  }
}

export async function authFetch(url, options = {}) {
  const token = localStorage.getItem('authToken');
  const headers = { ...options.headers };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let response = await fetch(url, { ...options, headers });

  if ((response.status === 401 || response.status === 403) && token) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      const newToken = localStorage.getItem('authToken');
      headers['Authorization'] = `Bearer ${newToken}`;
      response = await fetch(url, { ...options, headers });
    } else {
      localStorage.removeItem('authUser');
      localStorage.removeItem('authToken');
      localStorage.removeItem('authRefreshToken');
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
    localStorage.setItem('authToken', data.token);
    localStorage.setItem('authRefreshToken', data.refreshToken);
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
    localStorage.setItem('authToken', data.token);
    localStorage.setItem('authRefreshToken', data.refreshToken);
    return data;
  };

  const logout = async () => {
    const refreshToken = localStorage.getItem('authRefreshToken');
    if (refreshToken) {
      const params = new URLSearchParams();
      params.append('refreshToken', refreshToken);
      try {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: params.toString(),
        });
      } catch {}
    }
    setUser(null);
    localStorage.removeItem('authUser');
    localStorage.removeItem('authToken');
    localStorage.removeItem('authRefreshToken');
  };

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
