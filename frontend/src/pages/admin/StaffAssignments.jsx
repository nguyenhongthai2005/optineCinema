import { useEffect, useState } from 'react';
import AdminLayout from './AdminLayout';
import adminService from '../../services/admin.service';

const assignmentLabels = { TICKET_CHECKING: 'Soát vé', COUNTER_SALES: 'Bán tại quầy' };
const today = new Date().toISOString().slice(0, 10);

export default function StaffAssignments() {
  const [form, setForm] = useState({ assignmentType: 'COUNTER_SALES', workDate: today, startTime: '08:00', endTime: '12:00', staffId: '', note: '' });
  const [assignments, setAssignments] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadAssignments = async () => {
    const res = await adminService.getStaffAssignments({ date: form.workDate });
    setAssignments(res.data || []);
  };

  const loadSuggestions = async () => {
    setError('');
    try {
      const res = await adminService.getStaffAssignmentSuggestions({
        date: form.workDate,
        assignmentType: form.assignmentType,
        startTime: form.startTime,
        endTime: form.endTime,
      });
      setCandidates(res.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Không lấy được danh sách gợi ý.');
    }
  };

  useEffect(() => { loadAssignments(); loadSuggestions(); }, []);

  const create = async () => {
    setError('');
    setMessage('');
    if (!form.workDate || !form.startTime || !form.endTime || !form.assignmentType || !form.staffId) {
      setError(form.staffId
        ? 'Vui lòng nhập đầy đủ ngày, giờ, loại phân công và nhân viên.'
        : 'Vui lòng chọn nhân viên để phân công.');
      return;
    }

    try {
      await adminService.createStaffAssignment({
        staffId: Number(form.staffId),
        assignmentType: form.assignmentType,
        workDate: form.workDate,
        startTime: form.startTime,
        endTime: form.endTime,
        showtimeId: form.showtimeId || null,
        roomId: form.roomId || null,
        note: form.note || '',
      });
      setMessage('Đã tạo phân công.');
      setForm({ ...form, staffId: '' });
      await loadAssignments();
      await loadSuggestions();
    } catch (err) {
      setError(err.response?.data?.message || 'Không tạo được phân công.');
    }
  };

  const cancel = async (id) => {
    if (!window.confirm('Hủy phân công này?')) return;
    await adminService.cancelStaffAssignment(id);
    await loadAssignments();
  };

  return (
    <AdminLayout title="Phân công nhân viên">
      {message && <div className="admin-alert success">{message}</div>}
      {error && <div className="admin-alert">{error}</div>}
      <section className="admin-form-panel">
        <h2>Tạo phân công</h2>
        <div className="admin-grid-form">
          <input type="date" value={form.workDate} onChange={(e) => setForm({ ...form, workDate: e.target.value })} />
          <select value={form.assignmentType} onChange={(e) => setForm({ ...form, assignmentType: e.target.value, staffId: '' })}>
            <option value="TICKET_CHECKING">Soát vé</option>
            <option value="COUNTER_SALES">Bán tại quầy</option>
          </select>
          <input type="time" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
          <input type="time" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
          <input placeholder="Ghi chú" value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} />
          <button type="button" className="admin-button secondary" onClick={loadSuggestions}>Gợi ý nhân viên</button>
        </div>
      </section>

      <section className="admin-form-panel">
        <h2>Ứng viên phù hợp</h2>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Chọn</th><th>Nhân viên</th><th>Tài khoản</th><th>Vị trí</th><th>Hợp đồng</th><th>Khả dụng</th><th>Trạng thái</th><th>Lý do</th></tr></thead>
            <tbody>
              {candidates.map((item) => (
                <tr
                  key={item.staffId}
                  onClick={() => item.selectable && setForm({ ...form, staffId: item.staffId })}
                  style={{
                    cursor: item.selectable ? 'pointer' : 'not-allowed',
                    background: String(form.staffId) === String(item.staffId) ? 'rgba(123,208,255,0.12)' : undefined,
                  }}
                >
                  <td><input type="radio" disabled={!item.selectable} checked={String(form.staffId) === String(item.staffId)} onChange={() => setForm({ ...form, staffId: item.staffId })} /></td>
                  <td>{item.staffName}</td>
                  <td>{item.username}</td>
                  <td>{item.positionLabel}</td>
                  <td>{item.contractTypeLabel}</td>
                  <td>{item.availabilityMatched ? 'Có' : 'Không'}</td>
                  <td>{item.alreadyAssigned ? 'Đã phân công' : item.staffStatus}</td>
                  <td>{item.reason}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-note">{form.staffId ? `Đã chọn nhân viên #${form.staffId}` : 'Vui lòng chọn nhân viên để phân công.'}</div>
        <button type="button" className="admin-button" onClick={create}>Tạo phân công</button>
      </section>

      <section className="admin-form-panel">
        <h2>Lịch phân công</h2>
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Ngày</th><th>Giờ</th><th>Nhân viên</th><th>Vị trí</th><th>Nhiệm vụ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
              {assignments.map((item) => (
                <tr key={item.assignmentId}>
                  <td>{item.workDate}</td>
                  <td>{item.startTime} - {item.endTime}</td>
                  <td>{item.staffName} ({item.username})</td>
                  <td>{item.positionLabel}</td>
                  <td>{assignmentLabels[item.assignmentType] || item.assignmentType}</td>
                  <td>{item.status}</td>
                  <td><button onClick={() => cancel(item.assignmentId)} title="Hủy"><span className="material-symbols-outlined">cancel</span></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </AdminLayout>
  );
}
