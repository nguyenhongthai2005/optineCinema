import { useContext, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import staffService from '../../services/staff.service';
import { fmtDateTime, mutedButton, page, section, table, td, th, title, subtitle } from './staffUi';

export default function StaffShowtimesPage() {
  const { user } = useContext(AuthContext);
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const canSell = user?.roles?.includes('ROLE_ADMIN') || user?.position === 'COUNTER_SALES';

  useEffect(() => {
    staffService.getTodayShowtimes()
      .then((res) => setItems(res.data || []))
      .catch(() => setError('Không tải được lịch chiếu hôm nay.'));
  }, []);

  return (
    <div style={page}>
      <h1 style={title}>Lịch chiếu hôm nay</h1>
      <p style={subtitle}>{canSell ? 'Danh sách suất chiếu đang mở cho nghiệp vụ bán vé tại quầy.' : 'Danh sách suất chiếu để tra cứu thông tin.'}</p>
      {error && <div style={danger}>{error}</div>}
      <section style={{ ...section, marginTop: 18, overflowX: 'auto' }}>
        <table style={table}>
          <thead><tr>{['Phim', 'Phòng', 'Thời gian', 'Ghế trống', 'Ghế đã bán', 'Trạng thái', 'Thao tác'].map((h) => <th key={h} style={th}>{h}</th>)}</tr></thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.showtimeId}>
                <td style={td}><strong>{item.movieTitle}</strong></td>
                <td style={td}>{item.room}</td>
                <td style={td}>{fmtDateTime(item.startTime)} - {fmtDateTime(item.endTime)}</td>
                <td style={td}>{item.availableSeats}</td>
                <td style={td}>{item.bookedSeats}</td>
                <td style={td}>{item.displayStatusLabel || item.statusLabel || item.status}</td>
                <td style={td}>{canSell ? <Link to={`/staff/sell-ticket?showtimeId=${item.showtimeId}`} style={{ ...mutedButton, textDecoration: 'none' }}>Bán vé tại quầy</Link> : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
