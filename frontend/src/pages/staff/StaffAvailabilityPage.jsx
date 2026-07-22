import { useEffect, useState } from 'react';
import staffService from '../../services/staff.service';
import { dangerButton, input, mutedButton, page, primaryButton, section, subtitle, table, td, th, title } from './staffUi';

const dayLabels = {
  MONDAY: 'Thứ hai',
  TUESDAY: 'Thứ ba',
  WEDNESDAY: 'Thứ tư',
  THURSDAY: 'Thứ năm',
  FRIDAY: 'Thứ sáu',
  SATURDAY: 'Thứ bảy',
  SUNDAY: 'Chủ nhật',
};

const emptyForm = { dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '12:00', note: '' };

export default function StaffAvailabilityPage() {
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = async () => {
    const res = await staffService.getAvailability();
    setRows(res.data || []);
  };

  useEffect(() => { load().catch(() => setError('Không tải được thời gian làm việc.')); }, []);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');
    try {
      editing ? await staffService.updateAvailability(editing.id, form) : await staffService.createAvailability(form);
      setForm(emptyForm);
      setEditing(null);
      setMessage('Đã lưu thời gian làm việc.');
      await load();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không lưu được thời gian làm việc.');
    }
  };

  const edit = (row) => {
    setEditing(row);
    setForm({ dayOfWeek: row.dayOfWeek, startTime: row.startTime, endTime: row.endTime, note: row.note || '' });
  };

  const remove = async (row) => {
    if (!window.confirm('Xóa khung giờ này?')) return;
    await staffService.deleteAvailability(row.id);
    await load();
  };

  return (
    <div style={page}>
      <h1 style={title}>Thời gian làm việc</h1>
      <p style={subtitle}>Đăng ký các khung giờ bạn có thể làm việc hằng tuần.</p>
      {message && <div style={success}>{message}</div>}
      {error && <div style={danger}>{error}</div>}

      <section style={{ ...section, marginTop: 18 }}>
        <form onSubmit={submit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 12 }}>
          <select style={input} value={form.dayOfWeek} onChange={(e) => setForm({ ...form, dayOfWeek: e.target.value })}>
            {Object.entries(dayLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          <input style={input} type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} required />
          <input style={input} type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} required />
          <input style={input} placeholder="Ghi chú" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
          <button style={primaryButton}>{editing ? 'Cập nhật' : 'Thêm khung giờ'}</button>
        </form>
      </section>

      <section style={{ ...section, marginTop: 16, overflowX: 'auto' }}>
        <table style={table}>
          <thead><tr>{['Thứ', 'Bắt đầu', 'Kết thúc', 'Ghi chú', 'Thao tác'].map((h) => <th key={h} style={th}>{h}</th>)}</tr></thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td style={td}>{dayLabels[row.dayOfWeek] || row.dayOfWeek}</td>
                <td style={td}>{row.startTime}</td>
                <td style={td}>{row.endTime}</td>
                <td style={td}>{row.note || '-'}</td>
                <td style={td}>
                  <button style={mutedButton} onClick={() => edit(row)}>Sửa</button>
                  <button style={{ ...dangerButton, marginLeft: 8 }} onClick={() => remove(row)}>Xóa</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}

const success = { marginTop: 14, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', padding: 12, borderRadius: 8 };
const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
