import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import { AuthedImage } from '../components/AuthedImage';
import { Shirt, UploadCloud, Search, Filter, Trash2 } from 'lucide-react';

const CATEGORIES = [
  'ALL',
  'TOP',
  'BOTTOM',
  'DRESS',
  'OUTERWEAR',
  'SHOES',
  'BAG',
  'ACCESSORY',
  'JEWELRY',
  'OTHER',
];

export const Wardrobe = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [error, setError] = useState('');

  const fetchItems = async () => {
    setLoading(true);
    try {
      const res = await api.get('/wardrobe/items');
      setItems(res.data || []);
    } catch (err) {
      console.error('Failed to load wardrobe items:', err);
      setError('Could not load your wardrobe items.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchItems();
  }, []);

  const handleDelete = async (id) => {
    if (!window.confirm('Are you sure you want to remove this item from your wardrobe?')) return;
    try {
      await api.delete(`/wardrobe/items/${id}`);
      setItems((prev) => prev.filter((item) => item.id !== id));
    } catch (err) {
      alert('Failed to delete item: ' + (err.response?.data?.message || err.message));
    }
  };

  const filteredItems = items.filter((item) => {
    const matchesCategory = selectedCategory === 'ALL' || item.category === selectedCategory;
    const query = searchQuery.toLowerCase();
    const matchesSearch =
      !query ||
      item.description?.toLowerCase().includes(query) ||
      item.subcategory?.toLowerCase().includes(query) ||
      item.color?.toLowerCase().includes(query) ||
      item.style?.toLowerCase().includes(query);
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="main-content">
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: '2.2rem', marginBottom: '0.4rem' }}>
            My Digital Wardrobe
          </h1>
          <p style={{ color: 'var(--text-secondary)' }}>
            All clothes and accessories Maya knows you actually own ({items.length} items total).
          </p>
        </div>
        <Link to="/upload" className="btn btn-primary">
          <UploadCloud size={18} />
          <span>Upload New Photos</span>
        </Link>
      </div>

      {error && <div className="alert-error">{error}</div>}

      {/* Filter and Search Bar */}
      <div className="card" style={{ marginBottom: '2rem', padding: '1.25rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ position: 'relative', flex: '1', minWidth: '220px' }}>
            <Search size={18} style={{ position: 'absolute', left: '0.9rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-control"
              style={{ paddingLeft: '2.6rem' }}
              placeholder="Search by description, color, subcategory, style..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {/* Category Pills */}
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`btn btn-sm ${selectedCategory === cat ? 'btn-primary' : 'btn-secondary'}`}
              style={{ textTransform: 'capitalize' }}
            >
              {cat === 'ALL' ? 'All Items' : cat.toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Item Grid */}
      {loading ? (
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '3rem' }}>
          Loading your wardrobe...
        </p>
      ) : filteredItems.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '4rem 1.5rem' }}>
          <Shirt size={48} color="var(--text-muted)" style={{ marginBottom: '1rem' }} />
          <h3 style={{ marginBottom: '0.5rem' }}>No items found</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
            {items.length === 0
              ? 'You haven\'t uploaded any wardrobe photos yet. Upload photos of outfits or clothes to begin!'
              : 'No items match your filter criteria.'}
          </p>
          {items.length === 0 && (
            <Link to="/upload" className="btn btn-primary">
              <UploadCloud size={18} />
              <span>Upload Photos Now</span>
            </Link>
          )}
        </div>
      ) : (
        <div className="wardrobe-grid">
          {filteredItems.map((item) => (
            <div key={item.id} className="item-card">
              <div style={{ position: 'relative' }}>
                <AuthedImage
                  src={item.hasImage ? `/api/wardrobe/images/${item.id}` : null}
                  alt={item.description}
                  className="item-thumb"
                  fallback="https://placehold.co/400x400/faf8f5/c98a7d?text=Wardrobe+Item"
                />
                <span
                  className="badge badge-primary"
                  style={{ position: 'absolute', top: '0.6rem', left: '0.6rem', boxShadow: '0 2px 6px rgba(0,0,0,0.1)' }}
                >
                  {item.category}
                </span>
              </div>

              <div className="item-content">
                <div>
                  <h4 style={{ fontSize: '0.98rem', fontWeight: '600', color: 'var(--text-primary)', marginBottom: '0.4rem' }}>
                    {item.subcategory || item.category}
                  </h4>
                  <p style={{ fontSize: '0.86rem', color: 'var(--text-secondary)', marginBottom: '0.8rem', lineHeight: '1.4' }}>
                    {item.description}
                  </p>
                </div>

                <div>
                  <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', marginBottom: '0.8rem' }}>
                    {item.color && <span className="badge badge-secondary">{item.color}</span>}
                    {item.style && <span className="badge badge-secondary">{item.style}</span>}
                    {item.formality && <span className="badge badge-secondary">{item.formality}</span>}
                    {item.season && <span className="badge badge-secondary">{item.season}</span>}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '0.6rem' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      ID: #{item.id}
                    </span>
                    <button
                      onClick={() => handleDelete(item.id)}
                      className="btn btn-sm"
                      style={{ color: 'var(--danger)', padding: '0.2rem 0.4rem' }}
                      title="Remove item"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
