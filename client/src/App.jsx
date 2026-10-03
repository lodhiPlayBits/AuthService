import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AnnouncementsProvider, useAnnouncementsContext } from './context/AnnouncementsContext';
import { AccountNotificationsProvider, useAccountNotificationsContext } from './context/AccountNotificationsContext';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { useCallback, useMemo, useState } from 'react';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import About from './pages/About';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import ManageUsers from './pages/ManageUsers';
import EditProfile from './pages/EditProfile';
import NotificationBanner from './components/NotificationBanner';
import AuroraBackground from './components/AuroraBackground';

const toMillis = (value) => {
  if (typeof value === 'number') return value;
  const parsed = Date.parse(value);
  return Number.isNaN(parsed) ? 0 : parsed;
};

const notificationKey = (notification) =>
  String(
    notification.announcementId ||
    notification.eventId ||
    notification.id ||
    notification.timestamp ||
    notification.type
  );

function AppContent() {
  const { sessionReady } = useAuth();
  // SSE (EventSource) keeps delivering account events; STOMP delivers announcements.
  // Both streams are provided once at the App level and shared with consumers
  // (Dashboard sidebar) through context.
  const { lastEvent, connectionState } = useAccountNotificationsContext();
  const { announcements, acknowledgeAnnouncement, dismissAnnouncement } = useAnnouncementsContext();
  const [dismissedKeys, setDismissedKeys] = useState(new Set());

  const unreadAnnouncements = useMemo(() => 
    announcements.filter(a => !dismissedKeys.has(notificationKey(a))),
  [announcements, dismissedKeys]);

  const unreadLastEvent = useMemo(() => 
    lastEvent && !dismissedKeys.has(notificationKey(lastEvent)) ? lastEvent : null,
  [lastEvent, dismissedKeys]);

  const latestNotification = useMemo(() => {
    const latestAnnouncement = unreadAnnouncements[0];
    if (!latestAnnouncement) return unreadLastEvent;
    if (!unreadLastEvent) return latestAnnouncement;
    return toMillis(latestAnnouncement.timestamp) >= toMillis(unreadLastEvent.timestamp)
      ? latestAnnouncement
      : unreadLastEvent;
  }, [unreadLastEvent, unreadAnnouncements]);

  const currentKey = latestNotification ? notificationKey(latestNotification) : null;
  const currentNotification = latestNotification;

  const handleBannerClose = useCallback(() => {
    if (currentKey) {
      setDismissedKeys(prev => {
        const next = new Set(prev);
        next.add(currentKey);
        return next;
      });
    }
  }, [currentKey]);

  // Hold the UI until the silent /refresh bootstrap settles so restored
  // sessions never flash an anonymous navbar.
  if (!sessionReady) {
    return (
      <AuroraBackground>
        <div className="app-container">
          <main className="main-content" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
            <p style={{ color: 'var(--text-secondary)' }}>Restoring session…</p>
          </main>
        </div>
      </AuroraBackground>
    );
  }

  return (
    <AuroraBackground>
      <div className="app-container">
        <Navbar connectionState={connectionState} />
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/about" element={<About />} />
            <Route path="/login" element={<Login />} />
            <Route path="/signup" element={<Signup />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/manage-users" element={<ManageUsers />} />
            <Route path="/edit-profile" element={<EditProfile />} />
            <Route path="/edit-profile/:userId" element={<EditProfile />} />
          </Routes>
        </main>
        {currentNotification && (
          <NotificationBanner
            key={currentKey}
            notification={currentNotification}
            onClose={handleBannerClose}
            onAcknowledge={acknowledgeAnnouncement}
            onDismiss={dismissAnnouncement}
          />
        )}
      </div>
    </AuroraBackground>
  );
}

function App() {
  return (
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <AuthProvider>
        <AccountNotificationsProvider>
          <AnnouncementsProvider>
            <Router basename="/client">
              <AppContent />
            </Router>
          </AnnouncementsProvider>
        </AccountNotificationsProvider>
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
