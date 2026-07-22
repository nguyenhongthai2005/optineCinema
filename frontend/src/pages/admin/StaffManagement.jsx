import React, { useEffect, useState } from 'react';
import AdminLayout from './AdminLayout';
import adminService from '../../services/admin.service';

const emptyForm = {
  fullName: '',
  email: '',
  phone: '',
  gender: '',
  dateOfBirth: '',
  address: '',
  status: 'ACTIVE',
  position: 'COUNTER_SALES',
  contractType: 'FULL_TIME',
};

const statusLabels = {
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Ngừng hoạt động',
  LOCKED: 'Đã khóa',
  BLOCKED: 'Đã khóa',
  REVOKED: 'Đã thu hồi',
};

const credentialWarning = 'Vui lòng lưu lại mật khẩu. Sau khi đóng cửa sổ này, hệ thống sẽ không hiển thị lại mật khẩu.';

const positionLabels = { TICKET_CHECKER: 'Soát vé', COUNTER_SALES: 'Bán tại quầy' };
const contractLabels = { SEASONAL: 'Thời vụ', FULL_TIME: 'Hợp đồng chính thức' };

export default function StaffManagement() {
  const [staff, setStaff] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [positionFilter, setPositionFilter] = useState('');
  const [contractFilter, setContractFilter] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(null);
  const [selected, setSelected] = useState(null);
  const [credentials, setCredentials] = useState(null);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [nextUsername, setNextUsername] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadStaff = async () => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const res = await adminService.getStaff(keyword, statusFilter || undefined, positionFilter || undefined, contractFilter || undefined);
      setStaff(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Không tải được danh sách nhân viên');
    } finally {
      setLoading(false);
    }
  };

  const loadNextUsername = async () => {
    try {
      const res = await adminService.getNextStaffUsername();
      setNextUsername(res.data?.nextUsername || '');
    } catch {
      setNextUsername('');
    }
  };

  useEffect(() => { loadStaff(); }, []);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    loadNextUsername();
  };

  const openEdit = (item) => {
    setEditing(item);
    setForm({
      fullName: item.fullName || '',
      email: item.email || '',
      phone: item.phone || '',
      gender: item.gender || '',
      dateOfBirth: item.dateOfBirth || '',
      address: item.address || '',
      status: item.status === 'BLOCKED' ? 'LOCKED' : item.status || 'ACTIVE',
      position: item.position || item.staffPosition || 'COUNTER_SALES',
      contractType: item.contractType || item.employmentType || 'FULL_TIME',
    });
  };

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setMessage('');
    try {
      if (editing) {
        await adminService.updateStaff(editing.id, form);
      } else {
        const res = await adminService.createStaff(form);
        setCredentials({ ...res.data, title: 'Tạo nhân viên thành công' });
      }
      setEditing(null);
      setForm(emptyForm);
      setNextUsername('');
      loadStaff();
    } catch (err) {
      setError(err.response?.data?.message || 'Lưu nhân viên thất bại');
    }
  };

  const changeStatus = async (item) => {
    const nextStatus = item.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    await adminService.updateStaffStatus(item.id, nextStatus);
    loadStaff();
  };

  const revokeStaff = async () => {
    if (!revokeTarget) return;
    setError('');
    try {
      await adminService.revokeStaff(revokeTarget.id);
      setRevokeTarget(null);
      setCredentials(null);
      await loadStaff();
      setMessage('Đã thu hồi tài khoản nhân viên.');
    } catch (err) {
      setError(err.response?.data?.message || 'Không thu hồi được tài khoản nhân viên');
    }
  };

  const copyText = async (text) => {
    await navigator.clipboard.writeText(text);
  };

  const copyBoth = async () => {
    await copyText(`Tài khoản: ${credentials.username}\nMật khẩu tạm thời: ${credentials.temporaryPassword}`);
  };

  return (
    <AdminLayout title="Nhân viên">
      <section className="admin-toolbar">
        <div className="admin-search">
          <span className="material-symbols-outlined">search</span>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && loadStaff()} placeholder="Tìm tên, email, SĐT, tài khoản" />
        </div>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
          <option value="LOCKED">Đã khóa</option>
        </select>
        <select value={positionFilter} onChange={(e) => setPositionFilter(e.target.value)}>
          <option value="">Tất cả vị trí</option>
          <option value="TICKET_CHECKER">Soát vé</option>
          <option value="COUNTER_SALES">Bán tại quầy</option>
        </select>
        <select value={contractFilter} onChange={(e) => setContractFilter(e.target.value)}>
          <option value="">Tất cả hợp đồng</option>
          <option value="SEASONAL">Thời vụ</option>
          <option value="FULL_TIME">Hợp đồng chính thức</option>
        </select>
        <button className="admin-button secondary" onClick={loadStaff}>Tìm kiếm</button>
        <button className="admin-button" onClick={openCreate}>Tạo nhân viên</button>
      </section>

      {error && <div className="admin-alert">{error}</div>}
      {message && <div className="admin-alert" style={{ borderColor: 'rgba(34,197,94,0.35)', color: '#bbf7d0' }}>{message}</div>}
      {loading ? <div className="admin-empty">Đang tải nhân viên...</div> : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Tài khoản</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Vị trí</th><th>Hợp đồng</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
              {staff.map((item) => (
                <tr key={item.id}>
                  <td>{item.username}</td>
                  <td>{item.fullName}</td>
                  <td>{item.email}</td>
                  <td>{item.phone}</td>
                  <td>{item.positionLabel || positionLabels[item.position] || item.staffPosition}</td>
                  <td>{item.contractTypeLabel || contractLabels[item.contractType] || item.employmentType}</td>
                  <td><span className={`status-pill ${(item.status === 'BLOCKED' ? 'locked' : item.status)?.toLowerCase()}`}>{statusLabels[item.status] || item.status}</span></td>
                  <td className="admin-actions">
                    <button onClick={() => setSelected(item)} title="Xem"><span className="material-symbols-outlined">visibility</span></button>
                    <button onClick={() => openEdit(item)} title="Sửa"><span className="material-symbols-outlined">edit</span></button>
                    <button onClick={() => changeStatus(item)} title={item.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa'}><span className="material-symbols-outlined">{item.status === 'ACTIVE' ? 'lock' : 'lock_open'}</span></button>
                    <button onClick={() => setRevokeTarget(item)} title="Thu hồi" style={{ color: '#ef4444' }}><span className="material-symbols-outlined">person_off</span></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <section className="admin-form-panel">
        <h2>{editing ? 'Cập nhật nhân viên' : 'Tạo nhân viên'}</h2>
        {!editing && <p className="admin-note">Tài khoản và mật khẩu sẽ được hệ thống tự động tạo. {nextUsername && `Tài khoản dự kiến: ${nextUsername}`}</p>}
        <form className="admin-grid-form" onSubmit={submit}>
          <input required placeholder="Họ tên" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
          <input type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          <input placeholder="Số điện thoại" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <select value={form.gender} onChange={(e) => setForm({ ...form, gender: e.target.value })}>
            <option value="">Giới tính</option>
            <option value="MALE">Nam</option>
            <option value="FEMALE">Nữ</option>
            <option value="OTHER">Khác</option>
          </select>
          <input type="date" value={form.dateOfBirth} onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
          <select required value={form.position} onChange={(e) => setForm({ ...form, position: e.target.value })}>
            <option value="TICKET_CHECKER">Soát vé</option>
            <option value="COUNTER_SALES">Bán tại quầy</option>
          </select>
          <select required value={form.contractType} onChange={(e) => setForm({ ...form, contractType: e.target.value })}>
            <option value="SEASONAL">Thời vụ</option>
            <option value="FULL_TIME">Hợp đồng chính thức</option>
          </select>
          <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
            <option value="LOCKED">Đã khóa</option>
          </select>
          <textarea placeholder="Địa chỉ" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          <button className="admin-button" type="submit">{editing ? 'Cập nhật' : 'Tạo nhân viên'}</button>
        </form>
      </section>

      {selected && (
        <div className="admin-modal-backdrop" onClick={() => setSelected(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <button className="admin-modal-close" onClick={() => setSelected(null)}><span className="material-symbols-outlined">close</span></button>
            <h2>{selected.fullName}</h2>
            <p>Tài khoản: {selected.username}</p>
            <p>Email: {selected.email || 'N/A'}</p>
            <p>SĐT: {selected.phone || 'N/A'}</p>
            <p>Giới tính: {selected.gender || 'N/A'}</p>
            <p>Ngày sinh: {selected.dateOfBirth || 'N/A'}</p>
            <p>Địa chỉ: {selected.address || 'N/A'}</p>
            <p>Vị trí: {selected.positionLabel || positionLabels[selected.position] || selected.staffPosition || 'N/A'}</p>
            <p>Loại hợp đồng: {selected.contractTypeLabel || contractLabels[selected.contractType] || selected.employmentType || 'N/A'}</p>
            <p>Vai trò: {selected.role}</p>
            <p>Trạng thái: {statusLabels[selected.status] || selected.status}</p>
            <p>Ngày tạo: {selected.createdAt || 'N/A'}</p>
          </div>
        </div>
      )}

      {credentials && (
        <div className="admin-modal-backdrop" onClick={() => setCredentials(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <button className="admin-modal-close" onClick={() => setCredentials(null)}><span className="material-symbols-outlined">close</span></button>
            <h2>{credentials.title || 'Thông tin đăng nhập'}</h2>
            {credentials.message && <p>{credentials.message}</p>}
            <div className="credential-box">
              <p><strong>Tài khoản:</strong> {credentials.username}</p>
              <p><strong>Mật khẩu tạm thời:</strong> {credentials.temporaryPassword}</p>
            </div>
            <p className="admin-warning">{credentialWarning}</p>
            <div className="admin-actions">
              <button onClick={() => copyText(credentials.username)} title="Copy username"><span className="material-symbols-outlined">person</span></button>
              <button onClick={() => copyText(credentials.temporaryPassword)} title="Copy password"><span className="material-symbols-outlined">password</span></button>
              <button onClick={copyBoth} title="Copy both"><span className="material-symbols-outlined">content_copy</span></button>
              <button className="admin-button" onClick={() => setCredentials(null)}>Đóng</button>
            </div>
          </div>
        </div>
      )}

      {revokeTarget && (
        <div className="admin-modal-backdrop" onClick={() => setRevokeTarget(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <button className="admin-modal-close" onClick={() => setRevokeTarget(null)}><span className="material-symbols-outlined">close</span></button>
            <h2>Thu hồi tài khoản</h2>
            <p>Bạn có chắc chắn muốn thu hồi tài khoản nhân viên này không?</p>
            <p className="admin-warning">Hành động này sẽ đặt lại mật khẩu, khóa tài khoản và làm sạch thông tin nhân viên hiện tại. Nhân viên sẽ không thể đăng nhập sau khi bị thu hồi.</p>
            <div className="credential-box">
              <p><strong>Tài khoản:</strong> {revokeTarget.username}</p>
              <p><strong>Nhân viên:</strong> {revokeTarget.fullName || '-'}</p>
            </div>
            <div className="admin-actions">
              <button className="admin-button secondary" onClick={() => setRevokeTarget(null)}>Hủy</button>
              <button className="admin-button" onClick={revokeStaff} style={{ background: '#dc2626' }}>Xác nhận thu hồi</button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
