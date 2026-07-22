import { useEffect, useMemo, useState } from 'react';
import staffService from '../../services/staff.service';
import { fmtDate, fmtDateTime, minutes, page, section, statusLabel, subtitle, table, td, th, title } from './staffUi';
import { getAttendanceLocation } from '../../utils/geolocation';
import { useToast } from '../../context/ToastContext';

export default function StaffAttendancePage() {
  const [today, setToday] = useState(null);
  const [history, setHistory] = useState([]);
  const [currentLocation, setCurrentLocation] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [action, setAction] = useState('');
  const toast = useToast();
  const demoLocationEnabled = import.meta.env.DEV || import.meta.env.VITE_ATTENDANCE_DEMO_LOCATION_ENABLED === 'true';

  const load = async () => {
    const [todayRes, historyRes] = await Promise.all([
      staffService.getAttendanceToday(),
      staffService.getAttendanceHistory(),
    ]);
    setToday(todayRes.data);
    setHistory(historyRes.data || []);
  };

  useEffect(() => {
    load()
      .catch((error) => toast.error(errorMessage(error, 'Không tải được dữ liệu chấm công.')))
      .finally(() => setInitialLoading(false));
  }, []);

  const state = useMemo(() => attendanceState(today), [today]);

  const submitAttendance = async (type, location) => {
    setAction(type);
    try {
      type === 'in' ? await staffService.checkIn(location) : await staffService.checkOut(location);
      toast.success(type === 'in' ? 'Check-in thành công.' : 'Check-out thành công.');
      await load();
    } catch (error) {
      toast.error(errorMessage(error, 'Không cập nhật được chấm công.'));
    } finally {
      setAction('');
    }
  };

  const useCurrentLocation = async (type) => {
    setAction('locating');
    try {
      const location = await getAttendanceLocation();
      setCurrentLocation(location);
      await submitAttendance(type, location);
    } catch (error) {
      toast.error(errorMessage(error, 'Không lấy được vị trí hiện tại.'));
      setAction('');
    }
  };

  const useDemoLocation = async (type) => {
    if (today?.workplaceLatitude == null || today?.workplaceLongitude == null) {
      toast.error('Backend chưa cấu hình tọa độ nơi làm việc.');
      return;
    }
    const location = {
      latitude: Number(today.workplaceLatitude),
      longitude: Number(today.workplaceLongitude),
      accuracyMeters: 20,
    };
    setCurrentLocation(location);
    await submitAttendance(type, location);
  };

  if (initialLoading) {
    return <div style={page}><div style={section}>Đang tải dữ liệu chấm công...</div></div>;
  }

  return (
    <div style={page}>
      <div style={headerRow}>
        <div>
          <h1 style={title}>Chấm công</h1>
          <p style={subtitle}>Ghi nhận thời gian làm việc theo ca và vị trí tại rạp.</p>
        </div>
        <div style={{ ...statusPill, ...state.pill }}>
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>{state.icon}</span>
          {state.label}
        </div>
      </div>

      <section style={{ ...section, marginTop: 18, padding: 0, overflow: 'hidden' }}>
        <div style={{ ...stateBanner, ...state.banner }}>
          <span className="material-symbols-outlined" style={{ fontSize: 28 }}>{state.icon}</span>
          <div>
            <strong style={{ display: 'block', fontSize: 16 }}>{state.title}</strong>
            <span style={{ fontSize: 13, opacity: 0.9 }}>{state.description}</span>
          </div>
        </div>

        <div style={attendanceGrid}>
          <div style={panel}>
            <PanelTitle icon="calendar_month">Ca làm hôm nay</PanelTitle>
            <InfoRow label="Ngày làm việc" value={fmtDate(today?.date)} />
            <InfoRow label="Nhiệm vụ" value={today?.assignmentTypeLabel || 'Chưa được phân ca'} />
            <InfoRow label="Thời gian" value={shiftLabel(today)} />
            <InfoRow label="Mở check-in" value={today?.checkInAvailableFrom || '-'} />
            {!today?.hasAssignment && <Hint tone="warning">Bạn cần được quản lý phân ca trước khi chấm công.</Hint>}
          </div>

          <div style={panel}>
            <PanelTitle icon="schedule">Thời gian đã ghi nhận</PanelTitle>
            <div style={timeCards}>
              <TimeCard label="Check-in" value={timeOnly(today?.checkInTime)} active={Boolean(today?.checkInTime)} />
              <TimeCard label="Check-out" value={timeOnly(today?.checkOutTime)} active={Boolean(today?.checkOutTime)} />
            </div>
            <div style={metricsRow}>
              <Metric label="Làm việc" value={minutes(today?.workingMinutes)} />
              <Metric label="Đi trễ" value={minutes(today?.lateMinutes)} warning={Number(today?.lateMinutes) > 0} />
              <Metric label="Về sớm" value={minutes(today?.earlyLeaveMinutes)} warning={Number(today?.earlyLeaveMinutes) > 0} />
            </div>
          </div>

          <div style={panel}>
            <PanelTitle icon="location_on">Xác nhận vị trí</PanelTitle>
            <InfoRow label="Nơi làm việc" value={today?.workplaceName || '-'} />
            <InfoRow label="Phạm vi" value={today?.locationCheckEnabled ? `Trong bán kính ${today?.allowedRadiusMeters || 0}m` : 'Không bắt buộc vị trí'} />
            {currentLocation && (
              <Hint tone="success">
                Đã lấy vị trí · độ chính xác {Math.round(Number(currentLocation.accuracyMeters || 0))}m
              </Hint>
            )}
            <details style={detailsStyle}>
              <summary style={{ cursor: 'pointer', fontWeight: 750 }}>Xem chi tiết vị trí đã ghi</summary>
              <div style={{ marginTop: 10, display: 'grid', gap: 7 }}>
                <InfoRow label="Check-in" value={locationSummary(today, 'checkIn')} />
                <InfoRow label="Check-out" value={locationSummary(today, 'checkOut')} />
              </div>
            </details>
          </div>
        </div>

        <div style={actionBar}>
          <button
            disabled={Boolean(action) || !today?.canCheckIn}
            onClick={() => useCurrentLocation('in')}
            style={actionButton('in', Boolean(action) || !today?.canCheckIn)}
          >
            <span className="material-symbols-outlined">login</span>
            {action === 'locating' || action === 'in' ? 'Đang check-in...' : 'Check-in bằng vị trí'}
          </button>
          <button
            disabled={Boolean(action) || !today?.canCheckOut}
            onClick={() => useCurrentLocation('out')}
            style={actionButton('out', Boolean(action) || !today?.canCheckOut)}
          >
            <span className="material-symbols-outlined">logout</span>
            {action === 'locating' || action === 'out' ? 'Đang check-out...' : 'Check-out bằng vị trí'}
          </button>
          {demoLocationEnabled && (
            <button
              disabled={Boolean(action) || (!today?.canCheckIn && !today?.canCheckOut)}
              onClick={() => useDemoLocation(today?.canCheckIn ? 'in' : 'out')}
              style={demoButton(Boolean(action) || (!today?.canCheckIn && !today?.canCheckOut))}
            >
              Demo tại vị trí rạp
            </button>
          )}
          {today?.checkInBlockedReason && !today?.canCheckOut && <span style={blockedText}>{today.checkInBlockedReason}</span>}
        </div>
      </section>

      <section style={{ ...section, marginTop: 16, padding: 0, overflow: 'hidden' }}>
        <div style={historyHeader}>
          <div>
            <h2 style={{ margin: 0, fontSize: 18 }}>Lịch sử 30 ngày</h2>
            <p style={{ ...subtitle, marginTop: 4 }}>Các lần chấm công gần nhất của bạn.</p>
          </div>
          <strong style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{history.length} bản ghi</strong>
        </div>
        {history.length === 0 ? (
          <div style={emptyState}>Chưa có dữ liệu chấm công.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ ...table, minWidth: 860 }}>
              <thead><tr>{['Ngày', 'Ca làm', 'Check-in', 'Check-out', 'Thời lượng', 'Trạng thái', 'Ghi chú'].map((heading) => <th key={heading} style={th}>{heading}</th>)}</tr></thead>
              <tbody>
                {history.map((row) => (
                  <tr key={row.id || row.date}>
                    <td style={td}><strong>{fmtDate(row.date)}</strong></td>
                    <td style={td}>{row.shiftName || '-'}</td>
                    <td style={td}>{timeOnly(row.checkInTime)}</td>
                    <td style={td}>{timeOnly(row.checkOutTime)}</td>
                    <td style={td}>{minutes(row.workingMinutes)}</td>
                    <td style={td}><AttendanceBadge status={row.status} /></td>
                    <td style={{ ...td, maxWidth: 260 }}>{row.note || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function attendanceState(today) {
  if (!today?.hasAssignment && !today?.checkInTime) return {
    label: 'Chưa có ca', icon: 'event_busy', title: 'Bạn chưa được phân ca hôm nay',
    description: 'Liên hệ quản lý hoặc kiểm tra trang Lịch làm việc trước khi chấm công.',
    pill: { background: 'rgba(245, 158, 11, .14)', color: '#f59e0b' },
    banner: { background: 'rgba(245, 158, 11, .1)', color: '#fbbf24' },
  };
  if (today?.checkOutTime) return {
    label: 'Đã hoàn thành', icon: 'task_alt', title: 'Ca làm đã hoàn thành',
    description: `Bạn đã check-out lúc ${timeOnly(today.checkOutTime)}.`,
    pill: { background: 'rgba(34, 197, 94, .14)', color: '#22c55e' },
    banner: { background: 'rgba(34, 197, 94, .1)', color: '#86efac' },
  };
  if (today?.checkInTime) return {
    label: 'Đang trong ca', icon: 'pending_actions', title: 'Bạn đang trong ca làm việc',
    description: `Đã check-in lúc ${timeOnly(today.checkInTime)}. Hãy check-out khi kết thúc ca.`,
    pill: { background: 'rgba(59, 130, 246, .14)', color: '#60a5fa' },
    banner: { background: 'rgba(59, 130, 246, .1)', color: '#93c5fd' },
  };
  if (!today?.canCheckIn) return {
    label: 'Chưa đến giờ', icon: 'hourglass_top', title: 'Chưa đến thời gian check-in',
    description: today?.checkInBlockedReason || 'Vui lòng quay lại gần thời gian bắt đầu ca.',
    pill: { background: 'rgba(245, 158, 11, .14)', color: '#f59e0b' },
    banner: { background: 'rgba(245, 158, 11, .1)', color: '#fbbf24' },
  };
  return {
    label: 'Sẵn sàng', icon: 'location_on', title: 'Sẵn sàng check-in',
    description: 'Bật quyền vị trí và thực hiện check-in khi bạn đã có mặt tại rạp.',
    pill: { background: 'rgba(34, 197, 94, .14)', color: '#22c55e' },
    banner: { background: 'rgba(34, 197, 94, .1)', color: '#86efac' },
  };
}

function PanelTitle({ icon, children }) {
  return <h2 style={panelTitle}><span className="material-symbols-outlined" style={{ fontSize: 20 }}>{icon}</span>{children}</h2>;
}

function InfoRow({ label, value }) {
  return <div style={infoRow}><span>{label}</span><strong>{value || '-'}</strong></div>;
}

function TimeCard({ label, value, active }) {
  return <div style={{ ...timeCard, borderColor: active ? 'rgba(34, 197, 94, .45)' : 'var(--color-border)' }}><span>{label}</span><strong style={{ display: 'block', marginTop: 5, color: 'var(--color-text)', fontSize: 18 }}>{value}</strong></div>;
}

function Metric({ label, value, warning }) {
  return <div><span style={metricLabel}>{label}</span><strong style={{ display: 'block', marginTop: 3, color: warning ? '#f59e0b' : 'var(--color-text)' }}>{value}</strong></div>;
}

function Hint({ children, tone }) {
  return <div style={{ ...hint, ...(tone === 'success' ? successHint : warningHint) }}>{children}</div>;
}

function AttendanceBadge({ status }) {
  const completed = status === 'COMPLETED';
  const warning = status === 'LATE' || status === 'EARLY_LEAVE';
  return <span style={{ ...badge, color: completed ? '#22c55e' : warning ? '#f59e0b' : 'var(--color-text-muted)' }}>{statusLabel(status)}</span>;
}

function shiftLabel(today) {
  return today?.shiftStart && today?.shiftEnd ? `${today.shiftStart} – ${today.shiftEnd}` : '-';
}

function timeOnly(value) {
  return value ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '--:--';
}

function locationSummary(today, prefix) {
  const status = today?.[`${prefix}LocationStatus`];
  const distance = today?.[`${prefix}DistanceMeters`];
  const accuracy = today?.[`${prefix}AccuracyMeters`];
  if (!status || status === 'NO_DATA') return 'Chưa có dữ liệu';
  const label = status === 'VALID' ? 'Hợp lệ' : status === 'LOW_ACCURACY' ? 'Độ chính xác thấp' : 'Ngoài phạm vi';
  return `${label} · cách rạp ${Math.round(Number(distance || 0))}m · sai số ${Math.round(Number(accuracy || 0))}m`;
}

function errorMessage(error, fallback) {
  const data = error.response?.data;
  if (typeof data === 'string') return data;
  return data?.message || error.message || fallback;
}

const headerRow = { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, flexWrap: 'wrap' };
const statusPill = { display: 'inline-flex', alignItems: 'center', gap: 7, padding: '8px 12px', borderRadius: 999, fontSize: 13, fontWeight: 800 };
const stateBanner = { display: 'flex', alignItems: 'center', gap: 12, padding: '16px 20px', borderBottom: '1px solid var(--color-border)' };
const attendanceGrid = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(270px, 1fr))' };
const panel = { padding: 20, minHeight: 210, borderRight: '1px solid var(--color-border)' };
const panelTitle = { display: 'flex', alignItems: 'center', gap: 8, margin: '0 0 16px', color: 'var(--color-text)', fontSize: 16 };
const infoRow = { display: 'flex', justifyContent: 'space-between', gap: 14, padding: '7px 0', color: 'var(--color-text-muted)', fontSize: 13 };
const timeCards = { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 };
const timeCard = { padding: 12, border: '1px solid var(--color-border)', borderRadius: 8, color: 'var(--color-text-muted)', fontSize: 12 };
const metricsRow = { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 18 };
const metricLabel = { color: 'var(--color-text-muted)', fontSize: 11, textTransform: 'uppercase', fontWeight: 750 };
const hint = { marginTop: 14, borderRadius: 8, padding: '9px 10px', fontSize: 12, lineHeight: 1.5 };
const warningHint = { background: 'rgba(245, 158, 11, .1)', color: '#fbbf24' };
const successHint = { background: 'rgba(34, 197, 94, .1)', color: '#86efac' };
const detailsStyle = { marginTop: 14, borderTop: '1px solid var(--color-border)', paddingTop: 12, color: 'var(--color-text-muted)', fontSize: 12 };
const actionBar = { display: 'flex', alignItems: 'center', gap: 10, padding: '16px 20px', borderTop: '1px solid var(--color-border)', flexWrap: 'wrap' };
const baseActionButton = { display: 'inline-flex', alignItems: 'center', gap: 8, border: 0, borderRadius: 8, padding: '11px 16px', color: '#fff', fontWeight: 800 };
const actionButton = (type, disabled) => ({ ...baseActionButton, background: type === 'in' ? '#16a34a' : '#334155', cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.45 : 1 });
const demoButton = (disabled) => ({ ...baseActionButton, color: 'var(--color-text)', background: 'var(--surface-container-high)', border: '1px solid var(--color-border)', cursor: disabled ? 'not-allowed' : 'pointer', opacity: disabled ? 0.45 : 1 });
const blockedText = { color: '#f59e0b', fontSize: 13, fontWeight: 750 };
const historyHeader = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, padding: '18px 20px', borderBottom: '1px solid var(--color-border)' };
const emptyState = { padding: 32, textAlign: 'center', color: 'var(--color-text-muted)' };
const badge = { display: 'inline-flex', border: '1px solid currentColor', borderRadius: 999, padding: '4px 8px', fontSize: 12, fontWeight: 800 };
