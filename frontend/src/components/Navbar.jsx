import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Sparkles, Shirt, UploadCloud, MessageSquare, User, LogOut, Layers } from 'lucide-react';

export const Navbar = () => {
  const { user, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) return null;

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <NavLink to="/dashboard" className="brand-logo">
          <Sparkles size={24} color="#c98a7d" />
          <span>MAYA</span>
          <span className="brand-tag">AI Fashion</span>
        </NavLink>

        <div className="nav-links">
          <NavLink to="/dashboard" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Layers size={18} />
            <span>Dashboard</span>
          </NavLink>

          <NavLink to="/wardrobe" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <Shirt size={18} />
            <span>Wardrobe</span>
          </NavLink>

          <NavLink to="/upload" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <UploadCloud size={18} />
            <span>Upload Photos</span>
          </NavLink>

          <NavLink to="/chat" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <MessageSquare size={18} />
            <span>Chat with Maya</span>
          </NavLink>

          <NavLink to="/profile" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
            <User size={18} />
            <span>Profile</span>
          </NavLink>
        </div>

        <div className="nav-user">
          <span className="user-badge">{user?.name}</span>
          <button onClick={handleLogout} className="btn btn-secondary btn-sm" title="Log Out">
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </div>
    </nav>
  );
};
