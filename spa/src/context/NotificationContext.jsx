import { useState, useContext, createContext, useEffect, useCallback } from 'react';
import { useAuth, tryRefresh } from './AuthContext';
import { useLocation } from 'react-router-dom';

const NotificationContext = createContext(null);

export const useNotification = () => useContext(NotificationContext);

export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);
  const [dismissedBuildings, setDismissedBuildings] = useState(new Set());
  const { user } = useAuth();
  const location = useLocation();

  useEffect(() => {
    if (location.pathname === '/buildings') {
      setDismissedBuildings(new Set());
    }
  }, [location.pathname]);

  const dismissNotification = useCallback((id, buildingId) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
    if (buildingId != null) {
      setDismissedBuildings(prev => new Set(prev).add(buildingId));
    }
  }, []);

  const addNotification = useCallback((notification) => {
    if (notification.buildingId != null && dismissedBuildings.has(notification.buildingId)) {
      return null;
    }
    const n = { id: Date.now() + Math.random(), ...notification };
    setNotifications(prev => [...prev, n]);
    setTimeout(() => setNotifications(prev => prev.filter(x => x.id !== n.id)), 10000);
    return n;
  }, [dismissedBuildings]);

  useEffect(() => {
    if (!user) return;

    let eventSource;
    let reconnectTimeout;

    const connect = () => {
      eventSource = new EventSource('/api/notifications/stream');

      eventSource.addEventListener('critical-alert', (event) => {
        try {
          if (location.pathname === '/buildings') return;

          const data = JSON.parse(event.data);
          addNotification({ ...data });
        } catch (e) {
          console.error('Failed to parse notification:', e);
        }
      });

      eventSource.onerror = async () => {
        eventSource.close();
        if (eventSource.readyState === EventSource.CLOSED) {
          console.log('SSE connection closed');
          return;
        }
        await tryRefresh();
        reconnectTimeout = setTimeout(connect, 5000);
      };
    };

    connect();

    return () => {
      if (reconnectTimeout) clearTimeout(reconnectTimeout);
      if (eventSource) eventSource.close();
    };
  }, [user, addNotification, location.pathname]);

  return (
    <NotificationContext.Provider value={{ notifications, addNotification, dismissNotification }}>
      {children}
    </NotificationContext.Provider>
  );
}
