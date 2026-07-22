import { useEffect, useState } from 'react';
import staffService from '../../services/staff.service';
import { dangerButton, fmtDateTime, greenButton, mutedButton, page, section, statusLabel, subtitle, table, td, th, title, vnd } from './staffUi';

export default function StaffPendingPaymentsPage() {
  const [items, setItems] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);

  const load = async () => {
    const res = await staffService.getPendingPayments();
    setItems(res.data || []);
  };

  useEffect(() => { load().catch(() => setError('Không tải được thanh toán chờ xác nhận.')); }, []);

  const act = async (bookingId, action) => {
    setBusyId(bookingId);
    setMessage('');
    setError('');
    try {
      action === 'confirm'
        ? await staffService.confirmVietQrPayment(bookingId)
        : await staffService.rejectVietQrPayment(bookingId);
      setMessage(action === 'confirm' ? 'Đã xác nhận thanh toán.' : 'Đã từ chối thanh toán.');
      await load();
    } catch (err) {
      setError(err.response?.data || 'Không cập nhật được thanh toán.');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div style={page}>
      <h1 style={title}>Thanh toán chờ xác nhận</h1>
      <p style={subtitle}>Đối chiếu giao dịch VietQR trước khi xác nhận hoặc từ chối.</p>
      {message && <div style={success}>{message}</div>}
      {error && <div style={danger}>{error}</div>}
      <section style={{ ...section, marginTop: 18, overflowX: 'auto' }}>
        <button onClick={load} style={{ ...mutedButton, marginBottom: 12 }}>Làm mới</button>
        <table style={table}>
          <thead>
            <tr>{['Booking', 'Khách', 'Phim', 'Suất chiếu', 'Ghế', 'Số tiền', 'Nội dung CK', 'Trạng thái', 'Thao tác'].map((h) => <th key={h} style={th}>{h}</th>)}</tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.bookingId}>
                <td style={td}>#{item.bookingId}</td>
                <td style={td}>{item.customerName}<div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{item.customerEmail}</div></td>
                <td style={td}>{item.movieTitle}</td>
                <td style={td}>{fmtDateTime(item.showtimeStartTime)}<div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{item.roomName}</div></td>
                <td style={td}>{(item.seats || []).join(', ')}</td>
                <td style={{ ...td, fontWeight: 800, color: '#22c55e' }}>{vnd(item.amount)}</td>
                <td style={{ ...td, fontFamily: 'monospace', fontWeight: 800 }}>{item.transferContent}</td>
                <td style={td}>{statusLabel(item.paymentStatus)}</td>
                <td style={td}>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button disabled={busyId === item.bookingId} onClick={() => act(item.bookingId, 'confirm')} style={greenButton}>Confirm</button>
                    <button disabled={busyId === item.bookingId} onClick={() => act(item.bookingId, 'reject')} style={dangerButton}>Reject</button>
                  </div>
                </td>
              </tr>
            ))}
            {items.length === 0 && <tr><td style={td} colSpan={9}>Không có thanh toán VietQR đang chờ xác nhận.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const success = { marginTop: 14, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', padding: 12, borderRadius: 8 };
const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
