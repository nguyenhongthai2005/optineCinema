import React, { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import adminService from '../../services/admin.service';

export default function TicketCheckIn() {
  const scannerRef = useRef(null);
  const scannerRegionIdRef = useRef(`ticket-qr-scanner-${Math.random().toString(36).slice(2)}`);
  const [qrCode, setQrCode] = useState('');
  const [ticket, setTicket] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState('');

  useEffect(() => () => {
    stopCamera();
  }, []);

  const stopCamera = () => {
    const scanner = scannerRef.current;
    scannerRef.current = null;
    setCameraActive(false);
    if (!scanner) return;

    const stopPromise = scanner.isScanning
      ? scanner.stop().then(() => scanner.clear())
      : scanner.clear();

    Promise.resolve(stopPromise).catch(() => {
      // Camera may already be stopped by the browser or route change.
    });
  };

  const startCamera = async () => {
    setCameraError('');
    setError('');
    setMessage('');

    if (!window.isSecureContext) {
      setCameraError('Camera chỉ hoạt động trên HTTPS hoặc localhost. Vui lòng mở bằng http://localhost:5173 hoặc HTTPS.');
      return;
    }
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setCameraError('Trình duyệt không cung cấp API camera. Vui lòng thử Chrome/Edge mới hơn hoặc nhập mã thủ công.');
      return;
    }

    try {
      const cameras = await Html5Qrcode.getCameras();
      if (!cameras || cameras.length === 0) {
        setCameraError('Không tìm thấy camera.');
        return;
      }

      const camera = cameras.find((item) => /back|rear|environment/i.test(item.label)) || cameras[0];
      const scanner = new Html5Qrcode(scannerRegionIdRef.current, { verbose: false });
      scannerRef.current = scanner;

      await scanner.start(
        camera.id,
        { fps: 10, qrbox: { width: 250, height: 250 }, aspectRatio: 1.333 },
        async (decodedText) => {
          setQrCode(decodedText);
          stopCamera();
          await verify(decodedText);
        },
        () => {
          // Ignore unreadable frames; the scanner keeps trying.
        }
      );
      setCameraActive(true);
    } catch (err) {
      scannerRef.current = null;
      setCameraActive(false);
      setCameraError(cameraErrorMessage(err));
    }
  };

  const verify = async (value = qrCode) => {
    if (!value.trim()) {
      setError('Vui lòng nhập hoặc quét mã QR.');
      return;
    }
    setLoading(true);
    setError('');
    setMessage('');
    setTicket(null);
    try {
      const res = await adminService.verifyTicket(value.trim());
      setTicket(res.data);
      setMessage(res.data.canCheckIn === false && res.data.checkInAvailableFrom
        ? `Chưa đến giờ soát vé. Có thể soát vé từ ${new Date(res.data.checkInAvailableFrom).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false })}.`
        : res.data.blockedReason || res.data.message || 'Vé hợp lệ.');
    } catch (err) {
      setError(err.response?.data || 'Không xác minh được vé.');
    } finally {
      setLoading(false);
    }
  };

  const checkIn = async () => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const res = await adminService.checkInTicket(qrCode.trim());
      setTicket(res.data);
      setMessage(res.data.message || 'Check-in thành công.');
    } catch (err) {
      setError(err.response?.data || 'Check-in thất bại.');
    } finally {
      setLoading(false);
    }
  };

  const fmt = (value) => value ? new Date(value).toLocaleString('vi-VN') : '-';

  return (
    <div style={S.page}>
      <div style={{ marginBottom: 24 }}>
        <h1 style={S.title}>Soát vé</h1>
        <p style={S.subtitle}>Quét QR trên e-ticket hoặc nhập mã thủ công.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(280px, 420px) 1fr', gap: 20 }}>
        <section style={S.card}>
          <div style={{ position: 'relative', aspectRatio: '4 / 3', background: '#111827', borderRadius: 8, overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
            <div id={scannerRegionIdRef.current} style={{ width: '100%', height: '100%' }} />
            {!cameraActive && !scannerRef.current && <span style={{ color: 'var(--color-text-muted)', position: 'absolute' }}>Camera scanner</span>}
          </div>
          {cameraError && <div style={{ color: '#fecaca', fontSize: 13, marginBottom: 12 }}>{cameraError}</div>}
          <div style={{ display: 'flex', gap: 8, marginBottom: 18 }}>
            <button onClick={startCamera} disabled={cameraActive} style={buttonStyle('var(--color-primary)')}>Mở camera</button>
            <button onClick={stopCamera} disabled={!cameraActive} style={buttonStyle('var(--surface-container-high)', 'var(--color-text)')}>Dừng</button>
          </div>

          <label style={S.label}>Mã QR</label>
          <textarea value={qrCode} onChange={(e) => setQrCode(e.target.value)} rows={4} placeholder="Dán chuỗi QR code..." style={S.textarea} />
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button onClick={() => verify()} disabled={loading} style={buttonStyle('#2563eb')}>Xác minh</button>
            <button onClick={checkIn} disabled={loading || !ticket || ticket.status !== 'VALID' || ticket.canCheckIn === false} style={buttonStyle('#16a34a')}>Xác nhận vào rạp</button>
          </div>
        </section>

        <section style={S.card}>
          {message && <div style={S.success}>{message}</div>}
          {error && <div style={S.error}>{error}</div>}
          {!ticket ? (
            <div style={{ color: 'var(--color-text-muted)' }}>Thông tin vé sẽ hiển thị sau khi xác minh.</div>
          ) : (
            <div>
              <h2 style={{ marginTop: 0, color: 'var(--color-text)' }}>Ticket #{ticket.ticketId}</h2>
              <Info label="Trạng thái" value={ticket.status} />
              <Info label="Khách hàng" value={`${ticket.customerName || '-'} (${ticket.customerEmail || '-'})`} />
              <Info label="Phim" value={ticket.movieTitle} />
              <Info label="Suất chiếu" value={fmt(ticket.startTime)} />
              <Info label="Phòng" value={ticket.roomName} />
              <Info label="Ghế" value={ticket.seatLabel} />
              <Info label="Booking" value={`#${ticket.bookingId} - ${ticket.bookingStatus}`} />
              <Info label="Giờ mở soát vé" value={fmt(ticket.checkInAvailableFrom)} />
              <Info label="Lý do chặn" value={ticket.blockedReason || '-'} />
              <Info label="Đã check-in lúc" value={fmt(ticket.checkedInAt)} />
              <Info label="Nhân viên" value={ticket.checkedInBy || '-'} />
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Info({ label, value }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 12, padding: '9px 0', borderBottom: '1px solid var(--color-border)' }}>
      <span style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{label}</span>
      <strong style={{ color: 'var(--color-text)', fontSize: 14 }}>{value || '-'}</strong>
    </div>
  );
}

const S = {
  page: { padding: '24px', minHeight: '100vh', background: 'transparent', fontFamily: 'Inter, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%' },
  title: { fontSize: 26, fontWeight: 700, color: 'var(--color-text)', margin: 0 },
  subtitle: { fontSize: 14, color: 'var(--color-text-muted)', margin: '4px 0 0' },
  card: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 20, boxShadow: 'var(--shadow-card)' },
  label: { display: 'block', fontSize: 13, fontWeight: 700, color: 'var(--color-text-muted)', marginBottom: 6 },
  textarea: { width: '100%', border: '1px solid var(--color-border)', borderRadius: 8, padding: 10, resize: 'vertical', fontFamily: 'monospace', color: 'var(--color-text)', background: 'var(--surface-container-low)' },
  success: { background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', borderRadius: 8, padding: 12, marginBottom: 16 },
  error: { background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: 12, marginBottom: 16 },
}

function buttonStyle(background, color = 'var(--on-secondary-container)') {
  return {
    background,
    color,
    border: 'none',
    borderRadius: 8,
    padding: '10px 14px',
    fontWeight: 700,
    cursor: 'pointer',
  };
}

function cameraErrorMessage(err) {
  const name = err?.name || '';
  const message = String(err?.message || err || '');
  const combined = `${name} ${message}`.toLowerCase();

  if (combined.includes('notallowed') || combined.includes('permission') || combined.includes('denied')) {
    return 'Bạn đã chặn quyền camera. Hãy cấp quyền camera cho trang này trong trình duyệt.';
  }
  if (combined.includes('notfound') || combined.includes('not found') || combined.includes('overconstrained')) {
    return 'Không tìm thấy camera.';
  }
  if (combined.includes('notreadable') || combined.includes('could not start') || combined.includes('in use')) {
    return 'Không mở được camera. Camera có thể đang được ứng dụng khác sử dụng.';
  }
  return 'Không mở được trình quét QR. Vui lòng thử lại hoặc nhập mã thủ công.';
}
