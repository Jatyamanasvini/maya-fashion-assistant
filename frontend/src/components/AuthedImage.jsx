import React, { useEffect, useState } from 'react';

/**
 * Loads an image from a JWT-protected endpoint.
 *
 * A native <img src> cannot send the Authorization header, so protected images
 * (wardrobe items, generated outfit visualizations) would return 401. This component
 * fetches the image with the stored token and renders an object URL instead.
 */
export const AuthedImage = ({ src, alt, className, style, fallback }) => {
  const [objectUrl, setObjectUrl] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    let createdUrl = null;

    if (!src) {
      setObjectUrl(null);
      return undefined;
    }

    setFailed(false);
    const token = localStorage.getItem('maya_token');

    fetch(src, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.blob();
      })
      .then((blob) => {
        if (!active) return;
        createdUrl = URL.createObjectURL(blob);
        setObjectUrl(createdUrl);
      })
      .catch(() => {
        if (active) setFailed(true);
      });

    return () => {
      active = false;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [src]);

  if (objectUrl) {
    return <img src={objectUrl} alt={alt} className={className} style={style} />;
  }
  if (fallback) {
    return <img src={fallback} alt={alt} className={className} style={style} />;
  }
  return null;
};
