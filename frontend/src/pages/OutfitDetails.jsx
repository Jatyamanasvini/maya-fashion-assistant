import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../services/api';
import { AuthedImage } from '../components/AuthedImage';
import { Shirt, Scissors, Sparkles, ArrowLeft, Calendar, Image as ImageIcon } from 'lucide-react';

export const OutfitDetails = () => {
  const { id } = useParams();
  const [outfit, setOutfit] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchOutfit = async () => {
      try {
        const res = await api.get(`/outfits/${id}`);
        setOutfit(res.data);
      } catch (err) {
        console.error('Failed to load outfit details:', err);
        setError('Outfit not found or could not be loaded.');
      } finally {
        setLoading(false);
      }
    };

    fetchOutfit();
  }, [id]);

  if (loading) {
    return (
      <div className="main-content" style={{ textAlign: 'center', padding: '3rem' }}>
        <p style={{ color: 'var(--text-secondary)' }}>Loading outfit details...</p>
      </div>
    );
  }

  if (error || !outfit) {
    return (
      <div className="main-content">
        <div className="alert-error">{error || 'Outfit not found'}</div>
        <Link to="/dashboard" className="btn btn-secondary">
          <ArrowLeft size={16} />
          <span>Back to Dashboard</span>
        </Link>
      </div>
    );
  }

  return (
    <div className="main-content" style={{ maxWidth: '900px' }}>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link to="/dashboard" className="btn btn-secondary btn-sm" style={{ marginBottom: '1rem' }}>
          <ArrowLeft size={16} />
          <span>Back to Dashboard</span>
        </Link>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <span className="badge badge-primary" style={{ marginBottom: '0.4rem' }}>
              Curated Look #{outfit.id}
            </span>
            <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: '2.2rem' }}>
              Outfit Details
            </h1>
          </div>
          {outfit.createdAt && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-muted)', fontSize: '0.88rem' }}>
              <Calendar size={16} />
              <span>{new Date(outfit.createdAt).toLocaleDateString()}</span>
            </div>
          )}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '1.5rem' }}>
        {/* Styling Notes & Hairstyle */}
        <div className="card">
          <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.3rem', marginBottom: '0.75rem' }}>
            Styling Rationale
          </h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1rem', lineHeight: '1.6', marginBottom: '1.25rem' }}>
            {outfit.reason || 'Curated to match your personal style and wardrobe constraints.'}
          </p>

          {outfit.hairstyle && (
            <div
              style={{
                backgroundColor: 'var(--primary-light)',
                borderRadius: 'var(--radius-sm)',
                padding: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
              }}
            >
              <Scissors size={24} color="var(--primary)" />
              <div>
                <strong style={{ display: 'block', fontSize: '0.92rem', color: 'var(--primary)' }}>
                  Recommended Hairstyle
                </strong>
                <span style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                  {outfit.hairstyle}
                </span>
              </div>
            </div>
          )}
        </div>

        {/* Selected Items from Real Wardrobe */}
        <div className="card">
          <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.3rem', marginBottom: '1rem' }}>
            Wardrobe Items Used ({outfit.items?.length || 0})
          </h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
            {outfit.items?.map((it, idx) => (
              <div
                key={idx}
                style={{
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1rem',
                  backgroundColor: 'var(--bg-main)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <span className="badge badge-secondary">{it.role}</span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>#{it.wardrobeItemId}</span>
                </div>
                <h4 style={{ fontSize: '0.95rem', marginBottom: '0.3rem' }}>
                  {it.description || `Item #${it.wardrobeItemId}`}
                </h4>
                {it.color && (
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                    Color: {it.color}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Generated Visualization */}
        {outfit.generatedImageUrl && (
          <div className="card" style={{ textAlign: 'center' }}>
            <h3 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.3rem', marginBottom: '1rem' }}>
              Outfit Visualization
            </h3>
            <AuthedImage
              src={outfit.generatedImageUrl}
              alt="Generated Look"
              style={{
                maxHeight: '450px',
                maxWidth: '100%',
                borderRadius: 'var(--radius-md)',
                objectFit: 'contain',
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};
