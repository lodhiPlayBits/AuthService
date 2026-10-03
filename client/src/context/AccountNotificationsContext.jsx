import { createContext, useContext } from 'react';
import { useAccountNotifications } from '../hooks/useAccountNotifications';

/**
 * Shares a single SSE stream across the app. Without this, the global banner
 * (App) and the Dashboard sidebar would each run their own EventSource and
 * the user would hold two identical streams — doubling the connections and
 * tracking two independent replay windows.
 */
const AccountNotificationsContext = createContext(null);

export const useAccountNotificationsContext = () => useContext(AccountNotificationsContext);

export const AccountNotificationsProvider = ({ children }) => {
  const value = useAccountNotifications();
  return (
    <AccountNotificationsContext.Provider value={value}>{children}</AccountNotificationsContext.Provider>
  );
};
