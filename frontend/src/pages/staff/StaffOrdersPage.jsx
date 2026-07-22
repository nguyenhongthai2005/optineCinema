import { useEffect, useState } from 'react';
import staffService from '../../services/staff.service';
import { fmtDateTime, input, mutedButton, page, section, statusLabel, subtitle, table, td, th, title, vnd } from './staffUi';

const today = new Date().toISOString().slice(0, 10);

export default function StaffOrdersPage() {
  const [filters, setFilters] = useState({ date: today, status: '', keyword: '' });
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
      const res = await staffService.getOrders(params);
      setOrders(res.data || []);
    } catch (err) {
      setError(err.response?.data || 'Không tải được danh sách đơn hàng.');
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div style={page}>
      <h1 style={title}>Vé / Đơn hàng</h1>
      <p style={subtitle}>Tra cứu các booking gần đây theo ngày, trạng thái hoặc thông tin khách hàng.</p>
      {error && <div style={danger}>{error}</div>}
      <section style={{ ...section, marginTop: 18 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '180px 200px 1fr auto', gap: 10 }}>
          <input type="date" style={input} value={filters.date} onChange={(e) => setFilters({ ...filters, date: e.target.value })} />
          <select style={input} value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
            <option value="">Tất cả trạng thái</option>
            <option value="CONFIRMED">Đã thanh toán</option>
            <option value="WAITING_CONFIRMATION">Chờ xác nhận</option>
            <option value="PENDING_PAYMENT">Chờ thanh toán</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
          <input style={input} value={filters.keyword} onChange={(e) => setFilters({ ...filters, keyword: e.target.value })} placeholder="Tìm khách, SĐT, booking id..." />
          <button onClick={load} style={mutedButton}>Lọc</button>
        </div>
      </section>
      <section style={{ ...section, marginTop: 14, overflowX: 'auto' }}>
        <table style={table}>
          <thead><tr>{['Booking', 'Khách', 'Phim', 'Suất chiếu', 'Ghế', 'Tổng tiền', 'Thanh toán', 'Trạng thái', 'Tạo lúc', 'Thao tác'].map((h) => <th key={h} style={th}>{h}</th>)}</tr></thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.bookingId}>
                <td style={td}>#{order.bookingId}</td>
                <td style={td}>{order.customer}<div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{order.phone || order.email}</div></td>
                <td style={td}>{order.movie}</td>
                <td style={td}>{fmtDateTime(order.showtime)}<div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{order.room}</div></td>
                <td style={td}>{(order.seats || []).join(', ')}</td>
                <td style={td}>{vnd(order.totalAmount)}</td>
                <td style={td}>{order.paymentMethod} · {statusLabel(order.paymentStatus)}</td>
                <td style={td}>{statusLabel(order.bookingStatus)}</td>
                <td style={td}>{fmtDateTime(order.createdAt)}</td>
                <td style={td}><button onClick={() => window.print()} style={mutedButton}>In vé</button></td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td style={td} colSpan={10}>Không có đơn phù hợp.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
