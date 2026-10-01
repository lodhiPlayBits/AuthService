import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import About from './pages/About';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
import ManageUsers from './pages/ManageUsers';
import EditProfile from './pages/EditProfile';
import NotificationBanner from './components/NotificationBanner';
import { useAccountNotifications } from './hooks/useAccountNotifications';

function AppContent() {
  const { lastEvent, connectionState } = useAccountNotifications();
  const [currentNotification, setCurrentNotification] = useState(null);

  // Update notification when new event arrives
  useEffect(() => {
    if (lastEvent) {
      setCurrentNotification(lastEvent);
    }
  }, [lastEvent]);

  return (
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
      <NotificationBanner 
        notification={currentNotification} 
        onClose={() => setCurrentNotification(null)} 
      />
    </div>
  );
}

function App() {
  return (
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <AuthProvider>
        <Router basename="/client">
          <AppContent />
        </Router>
      </AuthProvider>
    </GoogleOAuthProvider>
  );
}

export default App;
