import { useEffect, useState } from 'react';
import staffService from '../../services/staff.service';
import { fmtDate, input, page, primaryButton, section, statusLabel, subtitle, table, td, th, title } from './staffUi';

export default function StaffMySchedulePage() {
  const [rows, setRows] = useState([]);
  const [filters, setFilters] = useState({ fromDate: '', toDate: '' });
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      const res = await staffService.getMyAssignments(filters);
      setRows(res.data || []);
    } catch {
      setError('Không tải được lịch làm việc.');
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div style={page}>
      <h1 style={title}>Lịch làm việc</h1>
      <p style={subtitle}>Xem các ca làm việc đã được admin phân công.</p>
      {error && <div style={danger}>{error}</div>}

      <section style={{ ...section, marginTop: 18 }}>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <input style={input} type="date" value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
          <input style={input} type="date" value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
          <button style={primaryButton} onClick={load}>Lọc</button>
        </div>
      </section>

      <section style={{ ...section, marginTop: 16, overflowX: 'auto' }}>
        <table style={table}>
          <thead><tr>{['Ngày', 'Bắt đầu', 'Kết thúc', 'Nhiệm vụ', 'Phòng / phim', 'Ghi chú', 'Trạng thái'].map((h) => <th key={h} style={th}>{h}</th>)}</tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.assignmentId}>
                <td style={td}>{fmtDate(row.workDate)}</td>
                <td style={td}>{row.startTime}</td>
                <td style={td}>{row.endTime}</td>
                <td style={td}>{row.assignmentTypeLabel}</td>
                <td style={td}>{[row.room, row.movie].filter(Boolean).join(' · ') || '-'}</td>
                <td style={td}>{row.note || '-'}</td>
                <td style={td}>{statusLabel(row.status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
