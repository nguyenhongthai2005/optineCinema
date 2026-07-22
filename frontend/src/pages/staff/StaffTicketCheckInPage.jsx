import React, { useEffect, useRef, useState } from 'react';
import { Html5Qrcode } from 'html5-qrcode';
import staffService from '../../services/staff.service';
import { darkButton, fmtDateTime, greenButton, input, page, primaryButton, section, statusLabel, subtitle, title } from './staffUi';

export default function StaffTicketCheckInPage() {
  const scannerRef = useRef(null);
  const scannerId = useRef(`staff-ticket-scanner-${Math.random().toString(36).slice(2)}`);
  const [qrCode, setQrCode] = useState('');
  const [ticket, setTicket] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [cameraActive, setCameraActive] = useState(false);

  useEffect(() => () => stopCamera(), []);

  const stopCamera = () => {
    const scanner = scannerRef.current;
    scannerRef.current = null;
    setCameraActive(false);
    if (!scanner) return;
    Promise.resolve(scanner.isScanning ? scanner.stop().then(() => scanner.clear()) : scanner.clear()).catch(() => {});
  };

  const startCamera = async () => {
    setError('');
    if (!window.isSecureContext) {
      setError('Camera chỉ hoạt động trên HTTPS hoặc localhost.');
      return;
    }
    try {
      const cameras = await Html5Qrcode.getCameras();
      if (!cameras?.length) throw new Error('Không tìm thấy camera.');
      const scanner = new Html5Qrcode(scannerId.current, { verbose: false });
      scannerRef.current = scanner;
      await scanner.start(cameras[0].id, { fps: 10, qrbox: { width: 250, height: 250 } }, async (decodedText) => {
        setQrCode(decodedText);
        stopCamera();
        await verify(decodedText);
      });
      setCameraActive(true);
    } catch (err) {
      setError(err.message || 'Không mở được camera.');
    }
  };

  const verify = async (value = qrCode) => {
    if (!value.trim()) return setError('Vui lòng nhập mã QR.');
    setLoading(true);
    setError('');
    setMessage('');
    setTicket(null);
    try {
      const res = await staffService.verifyTicket(value.trim());
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
      const res = await staffService.checkInTicket(qrCode.trim());
      setTicket(res.data);
      setMessage(res.data.message || 'Check-in thành công.');
    } catch (err) {
      setError(err.response?.data || 'Check-in thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={page}>
      <h1 style={title}>Soát vé</h1>
      <p style={subtitle}>Quét QR trên e-ticket hoặc nhập mã thủ công để xác minh và check-in.</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 420px) 1fr', gap: 16, marginTop: 18 }}>
        <section style={section}>
          <div style={{ aspectRatio: '4 / 3', background: '#111827', borderRadius: 8, overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--color-text-muted)' }}>
            <div id={scannerId.current} style={{ width: '100%', height: '100%' }} />
            {!cameraActive && <span>Camera scanner</span>}
          </div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button onClick={startCamera} disabled={cameraActive} style={darkButton}>Mở camera</button>
            <button onClick={stopCamera} disabled={!cameraActive} style={primaryButton}>Dừng</button>
          </div>
          <textarea style={{ ...input, minHeight: 110, marginTop: 14, fontFamily: 'monospace' }} value={qrCode} onChange={(e) => setQrCode(e.target.value)} placeholder="Dán chuỗi QR code..." />
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button onClick={() => verify()} disabled={loading} style={primaryButton}>Xác minh</button>
            <button onClick={checkIn} disabled={loading || !ticket || ticket.status !== 'VALID' || ticket.canCheckIn === false} style={greenButton}>Xác nhận vào rạp</button>
          </div>
        </section>
        <section style={section}>
          {message && <div style={success}>{message}</div>}
          {error && <div style={danger}>{error}</div>}
          {!ticket ? <div style={{ color: 'var(--color-text-muted)' }}>Thông tin vé sẽ hiển thị sau khi xác minh.</div> : (
            <div>
              <h2 style={{ marginTop: 0 }}>Ticket #{ticket.ticketId}</h2>
              <Info label="Trạng thái" value={statusLabel(ticket.status)} />
              <Info label="Khách hàng" value={`${ticket.customerName || '-'} (${ticket.customerEmail || '-'})`} />
              <Info label="Phim" value={ticket.movieTitle} />
              <Info label="Suất chiếu" value={fmtDateTime(ticket.startTime)} />
              <Info label="Phòng" value={ticket.roomName} />
              <Info label="Ghế" value={ticket.seatLabel} />
              <Info label="Booking" value={`#${ticket.bookingId} - ${statusLabel(ticket.bookingStatus)}`} />
              <Info label="Giờ mở soát vé" value={fmtDateTime(ticket.checkInAvailableFrom)} />
              <Info label="Lý do chặn" value={ticket.blockedReason || '-'} />
              <Info label="Đã check-in lúc" value={fmtDateTime(ticket.checkedInAt)} />
              <Info label="Nhân viên" value={ticket.checkedInBy || '-'} />
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Info({ label, value }) {
  return <div style={{ display: 'grid', gridTemplateColumns: '140px 1fr', gap: 12, padding: '9px 0', borderTop: '1px solid var(--color-border)' }}><span style={{ color: 'var(--color-text-muted)' }}>{label}</span><strong>{value || '-'}</strong></div>;
}

const success = { background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', padding: 12, borderRadius: 8, marginBottom: 12 };
const danger = { background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8, marginBottom: 12 };
