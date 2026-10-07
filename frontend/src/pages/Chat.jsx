import React, { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import api from '../services/api';
import { AuthedImage } from '../components/AuthedImage';
import {
  Sparkles,
  Send,
  Plus,
  MessageSquare,
  Shirt,
  Scissors,
  CloudSun,
  MapPin,
  RefreshCw,
  Image as ImageIcon,
} from 'lucide-react';

export const Chat = () => {
  const location = useLocation();
  const [conversations, setConversations] = useState([]);
  const [currentConvId, setCurrentConvId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [includeLocation, setIncludeLocation] = useState(false);
  const [coords, setCoords] = useState(null);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    fetchConversations();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  useEffect(() => {
    if (location.state?.initialPrompt) {
      setInputMessage(location.state.initialPrompt);
    }
  }, [location.state]);

  const fetchConversations = async () => {
    try {
      const res = await api.get('/chat/conversations');
      setConversations(res.data || []);
      if (res.data?.length > 0 && !currentConvId) {
        selectConversation(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load conversations:', err);
    }
  };

  const selectConversation = async (id) => {
    setCurrentConvId(id);
    try {
      const res = await api.get(`/chat/conversations/${id}`);
      setMessages(res.data.messages || []);
    } catch (err) {
      console.error('Failed to load messages:', err);
    }
  };

  const startNewConversation = async () => {
    try {
      const res = await api.post('/chat/conversations', { title: 'New Styling Session' });
      setConversations([res.data, ...conversations]);
      setCurrentConvId(res.data.id);
      setMessages([]);
    } catch (err) {
      console.error('Failed to create conversation:', err);
    }
  };

  const handleToggleLocation = () => {
    if (!includeLocation) {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            setCoords({
              latitude: pos.coords.latitude,
              longitude: pos.coords.longitude,
            });
            setIncludeLocation(true);
          },
          (err) => {
            console.warn('Geolocation denied or unavailable:', err);
            setIncludeLocation(false);
          }
        );
      }
    } else {
      setIncludeLocation(false);
      setCoords(null);
    }
  };

  const handleSendMessage = async (e) => {
    e?.preventDefault();
    if (!inputMessage.trim() || loading) return;

    const userText = inputMessage.trim();
    setInputMessage('');

    let convId = currentConvId;
    if (!convId) {
      try {
        const newConv = await api.post('/chat/conversations', {
          title: userText.slice(0, 30) + '...',
        });
        convId = newConv.data.id;
        setConversations([newConv.data, ...conversations]);
        setCurrentConvId(convId);
      } catch (err) {
        console.error('Error creating conversation:', err);
        return;
      }
    }

    // Append optimistic user message
    const tempUserMsg = {
      id: Date.now(),
      role: 'USER',
      content: userText,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);
    setLoading(true);

    try {
      const payload = {
        message: userText,
        latitude: coords?.latitude,
        longitude: coords?.longitude,
      };

      const res = await api.post(`/chat/conversations/${convId}/messages`, payload);

      // Assistant message + outfit recommendation
      const assistantMsg = {
        id: Date.now() + 1,
        role: 'ASSISTANT',
        content: res.data.reply,
        outfit: res.data.outfit,
        weather: res.data.weather,
        createdAt: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err) {
      console.error('Chat error:', err);
      const errorMsg = {
        id: Date.now() + 1,
        role: 'ASSISTANT',
        content: 'I had trouble checking your wardrobe for this request. Please try again!',
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const sendQuickPrompt = (prompt) => {
    setInputMessage(prompt);
  };

  return (
    <div className="main-content" style={{ padding: '1rem 1.5rem' }}>
      <div className="chat-container">
        {/* Sidebar */}
        <div
          className="card"
          style={{
            display: 'flex',
            flexDirection: 'column',
            padding: '1.25rem',
            overflowY: 'auto',
          }}
        >
          <button
            onClick={startNewConversation}
            className="btn btn-primary btn-sm"
            style={{ width: '100%', marginBottom: '1.25rem', justifyContent: 'center' }}
          >
            <Plus size={16} />
            <span>New Styling Session</span>
          </button>

          <div style={{ fontSize: '0.8rem', fontWeight: '700', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '0.75rem', letterSpacing: '0.05em' }}>
            Styling History
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1, overflowY: 'auto' }}>
            {conversations.length === 0 ? (
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>No previous chats.</p>
            ) : (
              conversations.map((c) => (
                <button
                  key={c.id}
                  onClick={() => selectConversation(c.id)}
                  style={{
                    padding: '0.65rem 0.8rem',
                    borderRadius: 'var(--radius-sm)',
                    textAlign: 'left',
                    fontSize: '0.88rem',
                    backgroundColor: currentConvId === c.id ? 'var(--primary-light)' : 'transparent',
                    color: currentConvId === c.id ? 'var(--primary)' : 'var(--text-primary)',
                    fontWeight: currentConvId === c.id ? '600' : '400',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <MessageSquare size={15} />
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {c.title || `Session #${c.id}`}
                  </span>
                </button>
              ))
            )}
          </div>
        </div>

        {/* Chat Main Area */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', padding: '0', overflow: 'hidden' }}>
          {/* Chat Header */}
          <div
            style={{
              padding: '1rem 1.5rem',
              borderBottom: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backgroundColor: 'var(--bg-main)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div
                style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--primary-light)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--primary)',
                }}
              >
                <Sparkles size={18} />
              </div>
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: '700' }}>Maya – Fashion Stylist</h3>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  Grounded in your real wardrobe
                </span>
              </div>
            </div>

            {/* Location context toggle */}
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.82rem', color: 'var(--text-secondary)', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={includeLocation}
                onChange={handleToggleLocation}
              />
              <MapPin size={14} color={includeLocation ? 'var(--primary)' : 'var(--text-muted)'} />
              <span>Use local weather</span>
            </label>
          </div>

          {/* Messages Thread */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {messages.length === 0 && (
              <div style={{ textAlign: 'center', margin: 'auto', maxWidth: '480px' }}>
                <Sparkles size={36} color="var(--primary)" style={{ marginBottom: '1rem' }} />
                <h2 style={{ fontFamily: 'var(--font-serif)', fontSize: '1.6rem', marginBottom: '0.5rem' }}>
                  What are we wearing today?
                </h2>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.92rem', marginBottom: '1.5rem' }}>
                  Ask Maya for outfit advice for any occasion. Maya picks combinations strictly from your uploaded clothes!
                </p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {[
                    'Pick something cute for brunch with my girlies tomorrow.',
                    'What should I wear for a family dinner?',
                    'Make this outfit more casual.',
                    'I don’t want sneakers.',
                  ].map((p, i) => (
                    <button
                      key={i}
                      onClick={() => sendQuickPrompt(p)}
                      className="btn btn-secondary btn-sm"
                      style={{ justifyContent: 'flex-start' }}
                    >
                      <span>"{p}"</span>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: msg.role === 'USER' ? 'flex-end' : 'flex-start',
                }}
              >
                {/* Bubble */}
                <div
                  style={{
                    maxWidth: '80%',
                    padding: '0.9rem 1.2rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: msg.role === 'USER' ? 'var(--primary)' : 'var(--bg-main)',
                    color: msg.role === 'USER' ? '#ffffff' : 'var(--text-primary)',
                    border: msg.role === 'USER' ? 'none' : '1px solid var(--border-color)',
                    fontSize: '0.95rem',
                    lineHeight: '1.5',
                  }}
                >
                  {msg.content}
                </div>

                {/* Optional Weather Context tag */}
                {msg.weather && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      fontSize: '0.78rem',
                      color: 'var(--text-secondary)',
                      marginTop: '0.4rem',
                      marginLeft: '0.5rem',
                    }}
                  >
                    <CloudSun size={14} color="var(--accent)" />
                    <span>
                      Weather: {msg.weather.temperature}°C, {msg.weather.condition}
                    </span>
                  </div>
                )}

                {/* Recommended Outfit Card */}
                {msg.outfit && (
                  <div
                    className="card"
                    style={{
                      marginTop: '0.8rem',
                      maxWidth: '85%',
                      borderColor: '#ebd8d4',
                      backgroundColor: '#fffcfb',
                      padding: '1.25rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                      <span className="badge badge-primary">Curated Look #{msg.outfit.id}</span>
                      {msg.outfit.hairstyle && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                          <Scissors size={14} color="var(--primary)" />
                          <strong>Hairstyle:</strong> {msg.outfit.hairstyle}
                        </div>
                      )}
                    </div>

                    <p style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
                      {msg.outfit.reason}
                    </p>

                    {/* Outfit items list */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', marginBottom: '1rem' }}>
                      {msg.outfit.items?.map((it, idx) => (
                        <div
                          key={idx}
                          style={{
                            border: '1px solid var(--border-color)',
                            borderRadius: 'var(--radius-sm)',
                            padding: '0.6rem 0.8rem',
                            backgroundColor: '#ffffff',
                            display: 'flex',
                            alignItems: 'center',
                            gap: '0.6rem',
                          }}
                        >
                          <Shirt size={20} color="var(--primary)" />
                          <div>
                            <div style={{ fontSize: '0.72rem', textTransform: 'uppercase', color: 'var(--text-muted)', fontWeight: '700' }}>
                              {it.role}
                            </div>
                            <div style={{ fontSize: '0.85rem', fontWeight: '600' }}>
                              {it.description || `Item #${it.wardrobeItemId}`}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Outfit Visualization (Image Generation) */}
                    {msg.outfit.generatedImageUrl && (
                      <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem' }}>
                        <div style={{ fontSize: '0.8rem', fontWeight: '600', color: 'var(--text-secondary)', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <ImageIcon size={14} color="var(--primary)" />
                          <span>Complete Look Visualization</span>
                        </div>
                        <AuthedImage
                          src={msg.outfit.generatedImageUrl}
                          alt="Curated look visualization"
                          style={{
                            maxWidth: '100%',
                            maxHeight: '320px',
                            borderRadius: 'var(--radius-sm)',
                            objectFit: 'contain',
                            border: '1px solid var(--border-color)',
                          }}
                        />
                      </div>
                    )}

                    {/* Quick refinement pills */}
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.75rem' }}>
                      <button
                        onClick={() => sendQuickPrompt('Make it more casual')}
                        className="btn btn-secondary btn-sm"
                        style={{ fontSize: '0.75rem' }}
                      >
                        More casual
                      </button>
                      <button
                        onClick={() => sendQuickPrompt('Make this outfit more feminine')}
                        className="btn btn-secondary btn-sm"
                        style={{ fontSize: '0.75rem' }}
                      >
                        More feminine
                      </button>
                      <button
                        onClick={() => sendQuickPrompt('Give me another option')}
                        className="btn btn-secondary btn-sm"
                        style={{ fontSize: '0.75rem' }}
                      >
                        Another option
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                <Sparkles size={18} color="var(--primary)" className="animate-pulse" />
                <span>Maya is reasoning and styling your look...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Footer */}
          <form
            onSubmit={handleSendMessage}
            style={{
              padding: '1rem 1.5rem',
              borderTop: '1px solid var(--border-color)',
              display: 'flex',
              gap: '0.75rem',
              backgroundColor: '#ffffff',
            }}
          >
            <input
              type="text"
              className="form-control"
              placeholder="Ask Maya what to wear, refine a style, or change an item..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              disabled={loading}
            />
            <button
              type="submit"
              className="btn btn-primary"
              disabled={!inputMessage.trim() || loading}
            >
              <Send size={18} />
              <span>Send</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
