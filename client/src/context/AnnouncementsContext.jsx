import { createContext, useContext } from 'react';
import { useAnnouncements } from '../hooks/useAnnouncements';

/**
 * Shares a single STOMP connection across the app (App banner and the admin
 * composer would otherwise open two WebSocket sessions per user).
 */
const AnnouncementsContext = createContext(null);

export const useAnnouncementsContext = () => useContext(AnnouncementsContext);

export const AnnouncementsProvider = ({ children }) => {
  const value = useAnnouncements();
  return <AnnouncementsContext.Provider value={value}>{children}</AnnouncementsContext.Provider>;
};
