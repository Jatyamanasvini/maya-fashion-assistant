import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { UploadCloud, CheckCircle2, Sparkles, AlertCircle } from 'lucide-react';

export const UploadPhotos = () => {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const [uploading, setUploading] = useState(false);
  const [normalizing, setNormalizing] = useState(false);
  const [extractedItems, setExtractedItems] = useState(null);
  const [error, setError] = useState('');

  const navigate = useNavigate();

  /**
   * The backend vision pipeline needs a JPEG/PNG. Modern phones and downloads often
   * produce WebP/HEIC, which Java's ImageIO cannot decode. We normalize whatever the
   * browser can decode into a downscaled JPEG here (also speeds up local inference).
   */
  const normalizeToJpeg = (file, maxDim = 1024) =>
    new Promise((resolve, reject) => {
      const url = URL.createObjectURL(file);
      const img = new Image();
      img.onload = () => {
        URL.revokeObjectURL(url);
        const scale = Math.min(1, maxDim / Math.max(img.width, img.height));
        const width = Math.max(1, Math.round(img.width * scale));
        const height = Math.max(1, Math.round(img.height * scale));
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        canvas.getContext('2d').drawImage(img, 0, 0, width, height);
        canvas.toBlob(
          (blob) => (blob ? resolve(blob) : reject(new Error('encode-failed'))),
          'image/jpeg',
          0.9
        );
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('decode-failed'));
      };
      img.src = url;
    });

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setError('Please select a valid image file (JPEG, PNG, WEBP, HEIC).');
      return;
    }

    if (file.size > 25 * 1024 * 1024) {
      setError('File size must be less than 25MB.');
      return;
    }

    setError('');
    setNormalizing(true);
    try {
      const blob = await normalizeToJpeg(file);
      const jpegFile = new File([blob], 'upload.jpg', { type: 'image/jpeg' });
      setSelectedFile(jpegFile);
      setPreviewUrl(URL.createObjectURL(jpegFile));
      setExtractedItems(null);
    } catch (err) {
      setError(
        "Maya couldn't read this image format in your browser. Please use a JPEG or PNG " +
        '(for iPhone HEIC photos, export/share as JPEG first).'
      );
      setSelectedFile(null);
      setPreviewUrl('');
    } finally {
      setNormalizing(false);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!selectedFile) {
      setError('Please select a photo to upload.');
      return;
    }

    setError('');
    setUploading(true);

    const formData = new FormData();
    formData.append('photo', selectedFile);

    try {
      const response = await api.post('/wardrobe/photos', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      setExtractedItems(response.data.items || []);
    } catch (err) {
      console.error('Photo analysis error:', err);
      const msg = err.response?.data?.message || 'Maya could not analyze this photo right now. Please try again.';
      setError(msg);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="main-content" style={{ maxWidth: '800px' }}>
      <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
        <h1 style={{ fontFamily: 'var(--font-serif)', fontSize: '2.4rem', marginBottom: '0.5rem' }}>
          Add Photos to Your Wardrobe
        </h1>
        <p style={{ color: 'var(--text-secondary)', maxWidth: '580px', margin: '0 auto' }}>
          Upload mirror selfies, outfit pictures, vacation snapshots, or clothing photos. Maya analyzes them to identify every item you own.
        </p>
      </div>

      {error && (
        <div className="alert-error" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Upload Zone */}
      {!extractedItems && (
        <div className="card" style={{ padding: '2.5rem', textAlign: 'center' }}>
          <div
            style={{
              border: '2px dashed var(--border-color)',
              borderRadius: 'var(--radius-md)',
              padding: '3rem 1.5rem',
              backgroundColor: 'var(--bg-main)',
              cursor: 'pointer',
              marginBottom: '1.5rem',
            }}
            onClick={() => document.getElementById('photoInput').click()}
          >
            <input
              id="photoInput"
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />

            {previewUrl ? (
              <div>
                <img
                  src={previewUrl}
                  alt="Preview"
                  style={{
                    maxHeight: '300px',
                    maxWidth: '100%',
                    borderRadius: 'var(--radius-sm)',
                    objectFit: 'contain',
                    marginBottom: '1rem',
                  }}
                />
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  Click to choose a different photo
                </p>
              </div>
            ) : (
              <div>
                <UploadCloud size={48} color="var(--primary)" style={{ marginBottom: '1rem' }} />
                <h3 style={{ fontSize: '1.2rem', marginBottom: '0.4rem' }}>
                  Click to upload or drag photo here
                </h3>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>
                  Supports JPEG, PNG, WEBP, HEIC — up to 25MB (converted & optimized automatically)
                </p>
              </div>
            )}
          </div>

          <button
            onClick={handleUpload}
            className="btn btn-primary"
            style={{ minWidth: '220px' }}
            disabled={!selectedFile || uploading || normalizing}
          >
            {normalizing ? (
              <>
                <Sparkles size={18} />
                <span>Preparing image...</span>
              </>
            ) : uploading ? (
              <>
                <Sparkles size={18} />
                <span>Analyzing with Vision AI...</span>
              </>
            ) : (
              <>
                <Sparkles size={18} />
                <span>Analyze & Extract Items</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Extracted Items Success Card */}
      {extractedItems && (
        <div className="card" style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <CheckCircle2 size={28} color="var(--success)" />
            <div>
              <h2 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.6rem' }}>
                Wardrobe Items Extracted!
              </h2>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem' }}>
                Maya discovered {extractedItems.length} item{extractedItems.length === 1 ? '' : 's'} from your photo.
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '2rem' }}>
            {extractedItems.map((item, idx) => (
              <div
                key={idx}
                style={{
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '1rem',
                  backgroundColor: 'var(--bg-main)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.3rem' }}>
                    <span className="badge badge-primary">{item.category}</span>
                    <strong style={{ fontSize: '1rem' }}>{item.subcategory || item.description}</strong>
                  </div>
                  <p style={{ fontSize: '0.86rem', color: 'var(--text-secondary)' }}>{item.description}</p>
                </div>
                <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                  {item.color && <span className="badge badge-secondary">{item.color}</span>}
                  {item.style && <span className="badge badge-secondary">{item.style}</span>}
                </div>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
            <button
              onClick={() => {
                setExtractedItems(null);
                setSelectedFile(null);
                setPreviewUrl('');
              }}
              className="btn btn-secondary"
            >
              Upload Another Photo
            </button>
            <button
              onClick={() => navigate('/wardrobe')}
              className="btn btn-primary"
            >
              View Full Wardrobe
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
