import React from 'react';
import { useAuth } from '../context/AuthContext';
import { User, Mail, Calendar, Sparkles, LogOut, ShieldCheck } from 'lucide-react';

export const Profile = () => {
  const { user, logout } = useAuth();

  return (
    <div className="main-content" style={{ maxWidth: '680px' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: '2.2rem', marginBottom: '0.4rem' }}>
          My Account
        </h1>
        <p style={{ color: 'var(--text-secondary)' }}>
          Manage your Maya account profile and personal stylist settings.
        </p>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem', padding: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1.5rem' }}>
          <div
            style={{
              width: '64px',
              height: '64px',
              borderRadius: '50%',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '1.5rem',
              fontWeight: '700',
            }}
          >
            {user?.name ? user.name[0].toUpperCase() : 'M'}
          </div>
          <div>
            <h2 style={{ fontSize: '1.3rem', fontWeight: '700' }}>{user?.name}</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{user?.email}</p>
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.92rem', color: 'var(--text-secondary)' }}>
            <Mail size={18} color="var(--primary)" />
            <span><strong>Email:</strong> {user?.email}</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.92rem', color: 'var(--text-secondary)' }}>
            <ShieldCheck size={18} color="var(--primary)" />
            <span><strong>Security:</strong> Protected by JWT session token</span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', fontSize: '0.92rem', color: 'var(--text-secondary)' }}>
            <Sparkles size={18} color="var(--primary)" />
            <span><strong>Stylist Mode:</strong> Multimodal Wardrobe Reasoning Active</span>
          </div>
        </div>

        <button
          onClick={logout}
          className="btn btn-secondary"
          style={{ color: 'var(--danger)', borderColor: '#fecaca' }}
        >
          <LogOut size={16} />
          <span>Sign Out</span>
        </button>
      </div>
    </div>
  );
};
