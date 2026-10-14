import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { Sparkles, Shirt, UploadCloud, MessageSquare, ArrowRight, Heart } from 'lucide-react';

export const Dashboard = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState({ itemsCount: 0, outfitsCount: 0 });
  const [recentOutfits, setRecentOutfits] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [itemsRes, outfitsRes] = await Promise.all([
          api.get('/wardrobe/items'),
          api.get('/outfits'),
        ]);
        setStats({
          itemsCount: itemsRes.data?.length || 0,
          outfitsCount: outfitsRes.data?.length || 0,
        });
        setRecentOutfits((outfitsRes.data || []).slice(0, 3));
      } catch (err) {
        console.error('Failed to load dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  return (
    <div className="main-content">
      {/* Welcome Banner */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, #ffeaf3 0%, #f3f0ff 100%)',
          borderColor: '#f7dcea',
          marginBottom: '2rem',
          padding: '2.5rem 2rem',
        }}
      >
        <span className="badge badge-primary" style={{ marginBottom: '0.8rem' }}>
          Personal Stylist
        </span>
        <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: '2.4rem', color: 'var(--text-primary)', marginBottom: '0.6rem' }}>
          Good day, {user?.name}
        </h1>
        <p style={{ color: 'var(--text-secondary)', maxWidth: '650px', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
          Your digital wardrobe is synced. Ask Maya to curate the perfect look for brunch, a dinner date, work, or casual outings.
        </p>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Link to="/chat" className="btn btn-primary">
            <MessageSquare size={18} />
            <span>Chat with Maya</span>
          </Link>
          <Link to="/upload" className="btn btn-secondary">
            <UploadCloud size={18} />
            <span>Upload Photos</span>
          </Link>
        </div>
      </div>

      {/* Quick Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{ padding: '1rem', background: 'var(--primary-light)', borderRadius: 'var(--radius-md)', color: 'var(--primary)' }}>
            <Shirt size={28} />
          </div>
          <div>
            <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Wardrobe Items</div>
            <div style={{ fontSize: '1.8rem', fontWeight: '700', color: 'var(--text-primary)' }}>{stats.itemsCount}</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{ padding: '1rem', background: '#fff1dc', borderRadius: 'var(--radius-md)', color: '#c98a3c' }}>
            <Sparkles size={28} />
          </div>
          <div>
            <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Generated Outfits</div>
            <div style={{ fontSize: '1.8rem', fontWeight: '700', color: 'var(--text-primary)' }}>{stats.outfitsCount}</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div style={{ padding: '1rem', background: '#f0e9ff', borderRadius: 'var(--radius-md)', color: '#8b6fc9' }}>
            <Heart size={28} />
          </div>
          <div>
            <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', fontWeight: '500' }}>Assistant Mode</div>
            <div style={{ fontSize: '1.2rem', fontWeight: '700', color: 'var(--text-primary)' }}>Multimodal AI</div>
          </div>
        </div>
      </div>

      {/* Suggested Prompts */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.3rem', marginBottom: '1rem' }}>
          Try Asking Maya:
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '0.75rem' }}>
          {[
            'Pick something cute for brunch with my girlies tomorrow.',
            'What should I wear for a family dinner tonight?',
            'What hairstyle should I do with my casual blazer?',
            'Give me a comfortable outfit for warm weather.',
          ].map((prompt, idx) => (
            <Link
              key={idx}
              to="/chat"
              state={{ initialPrompt: prompt }}
              style={{
                padding: '0.85rem 1rem',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--bg-main)',
                fontSize: '0.9rem',
                color: 'var(--text-secondary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                transition: 'all 0.2s',
              }}
            >
              <span>"{prompt}"</span>
              <ArrowRight size={14} color="var(--primary)" />
            </Link>
          ))}
        </div>
      </div>

      {/* Recent Outfits */}
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.6rem' }}>Recent Curated Looks</h2>
          <Link to="/wardrobe" style={{ color: 'var(--primary)', fontWeight: '600', fontSize: '0.92rem' }}>
            View Wardrobe →
          </Link>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading looks...</p>
        ) : recentOutfits.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
            <Shirt size={40} color="var(--text-muted)" style={{ marginBottom: '1rem' }} />
            <h3 style={{ marginBottom: '0.5rem', color: 'var(--text-primary)' }}>No outfits created yet</h3>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '1.25rem', maxWidth: '420px', margin: '0 auto 1.25rem' }}>
              Upload your photos so Maya can discover your clothing items, then ask Maya to style an outfit!
            </p>
            <Link to="/upload" className="btn btn-primary">
              <UploadCloud size={16} />
              <span>Upload Photos First</span>
            </Link>
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
            {recentOutfits.map((outfit) => (
              <div key={outfit.id} className="card" style={{ display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <span className="badge badge-primary">Outfit #{outfit.id}</span>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    {outfit.createdAt ? new Date(outfit.createdAt).toLocaleDateString() : ''}
                  </span>
                </div>
                <h4 style={{ fontSize: '1.05rem', marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
                  {outfit.hairstyle ? `Hairstyle: ${outfit.hairstyle}` : 'Curated Look'}
                </h4>
                <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: '1rem', flex: 1 }}>
                  {outfit.reason || 'Curated to match your styling requirements.'}
                </p>
                <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    {outfit.items?.length || 0} items
                  </span>
                  <Link to={`/outfits/${outfit.id}`} className="btn btn-outline btn-sm">
                    View Details
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
