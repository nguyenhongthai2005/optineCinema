import React, { createContext, useCallback, useContext, useRef, useState } from 'react';

export const ToastContext = createContext(null);

let idCounter = 0;
const nextId = () => ++idCounter;

/**
 * Provider đặt ở root app. Cung cấp hàm `showToast(message, type)`.
 * type: 'success' | 'error' | 'warning' | 'info'
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const timersRef = useRef({});

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    clearTimeout(timersRef.current[id]);
    delete timersRef.current[id];
  }, []);

  const showToast = useCallback((message, type = 'info', duration = 3500) => {
    const id = nextId();
    setToasts((prev) => [...prev, { id, message, type }]);
    timersRef.current[id] = setTimeout(() => dismiss(id), duration);
    return id;
  }, [dismiss]);

  return (
    <ToastContext.Provider value={{ showToast, dismiss }}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

/** Hook tiện lợi để dùng trong mọi component */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast phải dùng bên trong <ToastProvider>');
  const { showToast, dismiss } = ctx;
  return {
    success: (msg, dur) => showToast(msg, 'success', dur),
    error:   (msg, dur) => showToast(msg, 'error',   dur),
    warning: (msg, dur) => showToast(msg, 'warning', dur),
    info:    (msg, dur) => showToast(msg, 'info',    dur),
    dismiss,
  };
}

// ─── Styles nội tuyến ────────────────────────────────────────────────────────

const TYPE_CONFIG = {
  success: {
    icon: '✓',
    bg: 'rgba(34,197,94,0.12)',
    border: 'rgba(34,197,94,0.4)',
    iconColor: '#22c55e',
    barColor: '#22c55e',
  },
  error: {
    icon: '✕',
    bg: 'rgba(239,68,68,0.12)',
    border: 'rgba(239,68,68,0.4)',
    iconColor: '#ef4444',
    barColor: '#ef4444',
  },
  warning: {
    icon: '!',
    bg: 'rgba(234,179,8,0.12)',
    border: 'rgba(234,179,8,0.4)',
    iconColor: '#eab308',
    barColor: '#eab308',
  },
  info: {
    icon: 'i',
    bg: 'rgba(99,102,241,0.12)',
    border: 'rgba(99,102,241,0.4)',
    iconColor: '#818cf8',
    barColor: '#818cf8',
  },
};

const containerStyle = {
  position: 'fixed',
  bottom: '1.5rem',
  right: '1.5rem',
  zIndex: 99999,
  display: 'flex',
  flexDirection: 'column',
  gap: '0.65rem',
  pointerEvents: 'none',
  maxWidth: '360px',
  width: '90vw',
};

function ToastContainer({ toasts, onDismiss }) {
  return (
    <div style={containerStyle} aria-live="polite" aria-atomic="false">
      {toasts.map((t) => (
        <ToastItem key={t.id} toast={t} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

function ToastItem({ toast, onDismiss }) {
  const cfg = TYPE_CONFIG[toast.type] || TYPE_CONFIG.info;

  const itemStyle = {
    pointerEvents: 'all',
    display: 'flex',
    alignItems: 'flex-start',
    gap: '0.75rem',
    background: 'rgba(15,23,42,0.92)',
    backdropFilter: 'blur(12px)',
    border: `1px solid ${cfg.border}`,
    borderRadius: '0.875rem',
    padding: '0.9rem 1rem 0.9rem 0.85rem',
    boxShadow: '0 8px 32px rgba(0,0,0,0.45)',
    animation: 'toastSlideIn 0.28s cubic-bezier(0.34,1.56,0.64,1) both',
    overflow: 'hidden',
    position: 'relative',
  };

  const iconStyle = {
    flexShrink: 0,
    width: '1.6rem',
    height: '1.6rem',
    borderRadius: '50%',
    background: cfg.bg,
    border: `1px solid ${cfg.border}`,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: cfg.iconColor,
    fontWeight: 700,
    fontSize: '0.8rem',
    marginTop: '0.05rem',
  };

  const messageStyle = {
    flex: 1,
    color: '#e2e8f0',
    fontSize: '0.875rem',
    lineHeight: '1.45',
    wordBreak: 'break-word',
  };

  const closeStyle = {
    flexShrink: 0,
    background: 'none',
    border: 'none',
    color: '#64748b',
    cursor: 'pointer',
    fontSize: '1rem',
    lineHeight: 1,
    padding: '0 0.1rem',
    marginTop: '-0.05rem',
  };

  const barStyle = {
    position: 'absolute',
    bottom: 0,
    left: 0,
    height: '2px',
    background: cfg.barColor,
    opacity: 0.6,
    width: '100%',
    transformOrigin: 'left',
    animation: 'toastBar 3.5s linear forwards',
  };

  return (
    <>
      <style>{`
        @keyframes toastSlideIn {
          from { opacity: 0; transform: translateX(120%) scale(0.9); }
          to   { opacity: 1; transform: translateX(0)   scale(1);   }
        }
        @keyframes toastBar {
          from { transform: scaleX(1); }
          to   { transform: scaleX(0); }
        }
      `}</style>
      <div style={itemStyle} role="alert">
        <div style={iconStyle}>{cfg.icon}</div>
        <p style={messageStyle}>{toast.message}</p>
        <button style={closeStyle} onClick={() => onDismiss(toast.id)} aria-label="Đóng thông báo">×</button>
        <div style={barStyle} />
      </div>
    </>
  );
}
