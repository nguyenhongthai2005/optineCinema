import React, { useEffect, useState } from 'react';
import AdminLayout from './AdminLayout';
import adminService from '../../services/admin.service';

const today = new Date().toISOString().slice(0, 10);
const emptyForm = { staffId: '', workDate: today, checkInTime: '', checkOutTime: '', status: 'MANUAL_ADJUSTED', note: '' };

const toLocalDateTime = (value) => (value ? `${value}:00` : null);

export default function AttendanceManagement() {
  const [records, setRecords] = useState([]);
  const [staff, setStaff] = useState([]);
  const [summary, setSummary] = useState(null);
  const [filters, setFilters] = useState({ staffId: '', fromDate: today.slice(0, 8) + '01', toDate: today, status: '' });
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const params = Object.fromEntries(Object.entries(filters).filter(([, value]) => value));
      const [attendanceRes, summaryRes, staffRes] = await Promise.all([
        adminService.getAttendance(params),
        adminService.getAttendanceSummary({
          month: Number((filters.fromDate || today).slice(5, 7)),
          year: Number((filters.fromDate || today).slice(0, 4)),
          staffId: filters.staffId || undefined,
        }),
        adminService.getStaff(''),
      ]);
      setRecords(attendanceRes.data);
      setSummary(summaryRes.data);
      setStaff(staffRes.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải dữ liệu chấm công.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const submit = async (event) => {
    event.preventDefault();
    const payload = {
      staffId: Number(form.staffId),
      workDate: form.workDate,
      checkInTime: toLocalDateTime(form.checkInTime),
      checkOutTime: toLocalDateTime(form.checkOutTime),
      status: form.status,
      note: form.note,
    };
    try {
      if (editing) {
        await adminService.updateAttendance(editing.id, payload);
      } else {
        await adminService.createAttendance(payload);
      }
      setEditing(null);
      setForm(emptyForm);
      loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể lưu dữ liệu chấm công.');
    }
  };

  const openEdit = (record) => {
    setEditing(record);
    setForm({
      staffId: record.staffId,
      workDate: record.workDate,
      checkInTime: record.checkInTime ? record.checkInTime.slice(0, 16) : '',
      checkOutTime: record.checkOutTime ? record.checkOutTime.slice(0, 16) : '',
      status: record.status || 'MANUAL_ADJUSTED',
      note: record.note || '',
    });
  };

  const remove = async (record) => {
    if (!window.confirm(`Xóa bản ghi chấm công của ${record.staffName} ngày ${record.workDate}?`)) return;
    try {
      await adminService.deleteAttendance(record.id);
      await loadData();
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể xóa dữ liệu chấm công.');
    }
  };

  return (
    <AdminLayout title="Quản lý chấm công">
      <section className="summary-grid">
        {Object.entries({ present: 'Có mặt', absent: 'Vắng mặt', late: 'Đi trễ', leaveDays: 'Nghỉ phép' }).map(([key, label]) => (
          <div className="summary-tile" key={key}>
            <span>{label}</span>
            <strong>{summary?.[key] ?? 0}</strong>
          </div>
        ))}
      </section>

      <section className="admin-toolbar">
        <select value={filters.staffId} onChange={(e) => setFilters({ ...filters, staffId: e.target.value })}>
          <option value="">Tất cả nhân viên</option>
          {staff.map((item) => <option key={item.id} value={item.id}>{item.fullName}</option>)}
        </select>
        <input type="date" value={filters.fromDate} onChange={(e) => setFilters({ ...filters, fromDate: e.target.value })} />
        <input type="date" value={filters.toDate} onChange={(e) => setFilters({ ...filters, toDate: e.target.value })} />
        <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
          <option value="">Tất cả trạng thái</option>
          <option value="CHECKED_IN">Đang trong ca</option>
          <option value="COMPLETED">Hoàn thành ca</option>
          <option value="LATE">Đi trễ</option>
          <option value="EARLY_LEAVE">Về sớm</option>
          <option value="ABSENT">Vắng mặt</option>
          <option value="LEAVE">Nghỉ phép</option>
          <option value="MANUAL_ADJUSTED">Điều chỉnh thủ công</option>
        </select>
        <button className="admin-button secondary" onClick={loadData}>Áp dụng bộ lọc</button>
      </section>

      {error && <div className="admin-alert">{error}</div>}
      {loading ? <div className="admin-empty">Đang tải dữ liệu chấm công...</div> : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Nhân viên</th><th>Ngày</th><th>Check-in</th><th>Vị trí check-in</th><th>Check-out</th><th>Vị trí check-out</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{record.staffName}</td>
                  <td>{record.workDate}</td>
                  <td>{record.checkInTime?.replace('T', ' ') || '-'}</td>
                  <td><LocationCell record={record} type="checkIn" /></td>
                  <td>{record.checkOutTime?.replace('T', ' ') || '-'}</td>
                  <td><LocationCell record={record} type="checkOut" /></td>
                  <td><span className={`status-pill ${record.status?.toLowerCase()}`}>{attendanceStatus(record.status)}</span></td>
                  <td className="admin-actions">
                    <button onClick={() => openEdit(record)} title="Chỉnh sửa"><span className="material-symbols-outlined">edit</span></button>
                    <button onClick={() => remove(record)} title="Xóa"><span className="material-symbols-outlined">delete</span></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className="admin-form-panel">
        <h2>{editing ? 'Điều chỉnh chấm công' : 'Tạo bản ghi chấm công'}</h2>
        <form className="admin-grid-form" onSubmit={submit}>
          <select required value={form.staffId} onChange={(e) => setForm({ ...form, staffId: e.target.value })}>
            <option value="">Chọn nhân viên</option>
            {staff.map((item) => <option key={item.id} value={item.id}>{item.fullName}</option>)}
          </select>
          <input required type="date" value={form.workDate} onChange={(e) => setForm({ ...form, workDate: e.target.value })} />
          <input type="datetime-local" value={form.checkInTime} onChange={(e) => setForm({ ...form, checkInTime: e.target.value })} />
          <input type="datetime-local" value={form.checkOutTime} onChange={(e) => setForm({ ...form, checkOutTime: e.target.value })} />
          <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
            <option value="MANUAL_ADJUSTED">Điều chỉnh thủ công</option>
            <option value="COMPLETED">Hoàn thành ca</option>
            <option value="LATE">Đi trễ</option>
            <option value="EARLY_LEAVE">Về sớm</option>
            <option value="ABSENT">Vắng mặt</option>
            <option value="LEAVE">Nghỉ phép</option>
          </select>
          <input placeholder="Ghi chú điều chỉnh" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
          <button className="admin-button" type="submit">{editing ? 'Cập nhật' : 'Tạo bản ghi'}</button>
        </form>
      </section>
    </AdminLayout>
  );
}

function attendanceStatus(status) {
  return {
    PRESENT: 'Có mặt', CHECKED_IN: 'Đang trong ca', COMPLETED: 'Hoàn thành ca',
    LATE: 'Đi trễ', EARLY_LEAVE: 'Về sớm', ABSENT: 'Vắng mặt',
    LEAVE: 'Nghỉ phép', MANUAL_ADJUSTED: 'Điều chỉnh thủ công',
  }[status] || status || '-';
}

function LocationCell({ record, type }) {
  const lat = record[`${type}Latitude`];
  const lng = record[`${type}Longitude`];
  const distance = record[`${type}DistanceMeters`];
  const valid = record[`${type}LocationValid`];
  const label = valid === true ? 'Hợp lệ' : valid === false ? 'Không hợp lệ' : 'Chưa có dữ liệu';
  const color = valid === true ? '#166534' : valid === false ? '#991b1b' : '#64748b';

  return (
    <div style={{ minWidth: 130 }}>
      <div style={{ color, fontWeight: 700 }}>{label}</div>
      <div style={{ color: '#64748b', fontSize: 12 }}>{distance == null ? '-' : `${Math.round(Number(distance))}m`}</div>
      {lat != null && lng != null && (
        <a href={`https://www.google.com/maps?q=${lat},${lng}`} target="_blank" rel="noreferrer" style={{ fontSize: 12 }}>
          Mở bản đồ
        </a>
      )}
    </div>
  );
}
