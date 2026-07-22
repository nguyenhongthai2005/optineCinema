import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import staffService from '../../services/staff.service';
import { darkButton, fmtDateTime, greenButton, mutedButton, page, primaryButton, section, statusLabel, subtitle, title } from './staffUi';
import { getAttendanceLocation } from '../../utils/geolocation';

export default function StaffDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [busyText, setBusyText] = useState('');

  const load = async () => {
    setError('');
    try {
      const res = await staffService.getDashboard();
      setData(res.data);
    } catch (err) {
      setError(errorMessage(err, 'Không tải được dashboard.'));
    }
  };

  useEffect(() => { load(); }, []);

  const attendanceAction = async (action) => {
    setBusy(true);
    setBusyText('Đang lấy vị trí...');
    setError('');
    try {
      const location = await getAttendanceLocation();
      setBusyText('Đang chấm công...');
      action === 'in' ? await staffService.checkIn(location) : await staffService.checkOut(location);
      await load();
    } catch (err) {
      setError(errorMessage(err, 'Không cập nhật được chấm công.'));
    } finally {
      setBusy(false);
      setBusyText('');
    }
  };

  const cards = [
    ['Suất chiếu hôm nay', data?.todayShowtimes ?? 0],
    ...(data?.position === 'TICKET_CHECKER' ? [['Vé đã check-in', data?.checkedInTicketsToday ?? 0]] : []),
    ...(data?.position === 'COUNTER_SALES' ? [['Vé đã bán hôm nay', data?.ticketsSoldToday ?? 0], ['Thanh toán chờ xác nhận', data?.pendingVietQrPayments ?? 0]] : []),
    ['Suất chiếu sắp tới', data?.upcomingShowtimesToday?.length ?? 0],
    ['Khung giờ đã đăng ký', data?.availabilityCount ?? 0],
    ['Trạng thái chấm công hôm nay', statusLabel(data?.todayAttendanceStatus)],
  ];

  return (
    <div style={page}>
      <h1 style={title}>Tổng quan</h1>
      <p style={subtitle}>Tổng quan vận hành trong ngày và thao tác nhanh tại quầy.</p>
      {error && <div style={alertStyle}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 14, marginTop: 20 }}>
        {cards.map(([label, value]) => (
          <section key={label} style={section}>
            <div style={{ color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 700 }}>{label}</div>
            <strong style={{ display: 'block', marginTop: 8, fontSize: 26, color: 'var(--color-text)' }}>{value}</strong>
          </section>
        ))}
      </div>

      <section style={{ ...section, marginTop: 16 }}>
        <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Thao tác nhanh</h2>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {data?.position === 'COUNTER_SALES' && <Link to="/staff/sell-ticket" style={linkButton(primaryButton)}>Bán vé tại quầy</Link>}
          {data?.position === 'TICKET_CHECKER' && <Link to="/staff/check-in" style={linkButton(darkButton)}>Soát vé</Link>}
          {data?.position === 'COUNTER_SALES' && <Link to="/staff/payments/pending" style={linkButton(mutedButton)}>Xác nhận thanh toán</Link>}
          <button disabled={busy || !data?.canCheckIn} onClick={() => attendanceAction('in')} style={greenButton}>Check in</button>
          <button disabled={busy || !data?.canCheckOut} onClick={() => attendanceAction('out')} style={darkButton}>Check out</button>
          <Link to="/staff/attendance" style={linkButton(mutedButton)}>Chi tiết chấm công</Link>
          <Link to="/staff/my-schedule" style={linkButton(mutedButton)}>Lịch làm việc</Link>
        </div>
        {busyText && <div style={{ marginTop: 10, color: 'var(--color-text-muted)', fontWeight: 800 }}>{busyText}</div>}
        <div style={{ marginTop: 12, color: 'var(--color-text-muted)', fontSize: 14 }}>
          Check-in: <strong>{fmtDateTime(data?.checkInTime)}</strong> · Check-out: <strong>{fmtDateTime(data?.checkOutTime)}</strong>
        </div>
        <div style={{ marginTop: 6, color: 'var(--color-text-muted)', fontSize: 13 }}>
          Ca làm: <strong>{data?.shiftStart || '-'}</strong> - <strong>{data?.shiftEnd || '-'}</strong>
          {data?.checkInAvailableFrom && <> · Có thể check-in từ <strong>{data.checkInAvailableFrom}</strong></>}
        </div>
        {data?.checkInBlockedReason && <div style={{ marginTop: 6, color: 'var(--color-warning)', fontWeight: 800 }}>{data.checkInBlockedReason}</div>}
        <div style={{ marginTop: 6, color: 'var(--color-text-muted)', fontSize: 13 }}>
          {data?.workplaceName || 'Opticine Cinema'} · phạm vi {data?.allowedRadiusMeters || 0}m · kiểm tra vị trí {data?.locationCheckEnabled ? 'đang bật' : 'đang tắt'}
        </div>
      </section>

      <section style={{ ...section, marginTop: 16 }}>
        <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Phân công hôm nay</h2>
        {(data?.todayAssignments || []).length === 0 && <div style={{ color: 'var(--color-text-muted)' }}>Chưa có phân công hôm nay.</div>}
        {(data?.todayAssignments || []).map((item) => (
          <div key={item.assignmentId} style={{ padding: '10px 0', borderTop: '1px solid var(--color-border)' }}>
            <strong>{item.assignmentTypeLabel}</strong>
            <div style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{item.startTime} - {item.endTime} · {item.room || ''} {item.movie || ''}</div>
          </div>
        ))}
        {data?.nextAssignment && <div style={{ marginTop: 10, color: 'var(--color-text-muted)', fontSize: 14 }}>Ca tiếp theo: <strong>{data.nextAssignment.workDate} {data.nextAssignment.startTime} - {data.nextAssignment.endTime}</strong></div>}
      </section>

      <section style={{ ...section, marginTop: 16 }}>
        <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Suất chiếu sắp tới</h2>
        {(data?.upcomingShowtimesToday || []).map((item) => (
          <div key={item.showtimeId} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, padding: '10px 0', borderTop: '1px solid var(--color-border)' }}>
            <div><strong>{item.movieTitle}</strong><div style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{item.room} · {fmtDateTime(item.startTime)}</div></div>
            {data?.position === 'COUNTER_SALES' && <Link to={`/staff/sell-ticket?showtimeId=${item.showtimeId}`} style={linkButton(mutedButton)}>Bán vé tại quầy</Link>}
          </div>
        ))}
      </section>
    </div>
  );
}

const alertStyle = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
const linkButton = (style) => ({ ...style, textDecoration: 'none', display: 'inline-flex', alignItems: 'center' });

function errorMessage(err, fallback) {
  const data = err.response?.data;
  if (typeof data === 'string') return data;
  if (data?.message) return data.message;
  return err.message || fallback;
}
