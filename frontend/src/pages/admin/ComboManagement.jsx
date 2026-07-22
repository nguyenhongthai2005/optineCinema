import { useEffect, useState } from 'react';
import adminService from '../../services/admin.service';

const emptyForm = { name: '', description: '', imageUrl: '', category: 'COMBO', price: 0, stockQuantity: 0, status: 'ACTIVE' };

export default function ComboManagement() {
  const [combos, setCombos] = useState([]);
  const [filters, setFilters] = useState({ keyword: '', status: '', category: '' });
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { loadCombos(); }, []);

  const loadCombos = async (nextFilters = filters) => {
    setLoading(true);
    setError('');
    try {
      const res = await adminService.getCombos(nextFilters);
      setCombos(res.data || []);
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không tải được danh sách combo.');
    } finally {
      setLoading(false);
    }
  };

  const submit = async (event) => {
    event.preventDefault();
    setMessage('');
    setError('');
    if (!form.name.trim() || Number(form.price) < 0) {
      setError('Vui lòng nhập tên combo và giá hợp lệ.');
      return;
    }
    try {
      const payload = { ...form, price: Number(form.price), stockQuantity: Number(form.stockQuantity || 0) };
      if (editingId) {
        await adminService.updateCombo(editingId, payload);
        setMessage('Đã cập nhật combo.');
      } else {
        await adminService.createCombo(payload);
        setMessage('Đã tạo combo.');
      }
      setForm(emptyForm);
      setEditingId(null);
      loadCombos();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không lưu được combo.');
    }
  };

  const edit = (combo) => {
    setEditingId(combo.id);
    setForm({
      name: combo.name || '',
      description: combo.description || '',
      imageUrl: combo.imageUrl || '',
      category: combo.category || 'COMBO',
      price: combo.price || 0,
      stockQuantity: combo.stockQuantity || 0,
      status: combo.status || 'ACTIVE',
    });
  };

  const changeStatus = async (combo) => {
    try {
      await adminService.updateComboStatus(combo.id, combo.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE');
      loadCombos();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không đổi được trạng thái combo.');
    }
  };

  const remove = async (combo) => {
    if (!window.confirm(`Ngừng bán ${combo.name}?`)) return;
    await adminService.deleteCombo(combo.id);
    loadCombos();
  };

  const updateFilter = (field, value) => {
    const next = { ...filters, [field]: value };
    setFilters(next);
    loadCombos(next);
  };

  return (
    <div style={{ padding: 24, maxWidth: 1400, margin: '0 auto', color: 'var(--color-text)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'end', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 850 }}>Quản lý combo</h1>
          <p style={{ margin: '6px 0 0', color: 'var(--color-text-muted)' }}>Bắp nước, đồ uống và combo bán kèm vé.</p>
        </div>
      </div>

      {message && <div style={success}>{message}</div>}
      {error && <div style={danger}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 420px) 1fr', gap: 16, marginTop: 18, alignItems: 'start' }}>
        <form onSubmit={submit} style={section}>
          <h2 style={panelTitle}>{editingId ? 'Cập nhật combo' : 'Tạo combo'}</h2>
          <label style={label}>Tên combo</label>
          <input style={input} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <label style={label}>Mô tả</label>
          <textarea style={{ ...input, minHeight: 84 }} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <label style={label}>Ảnh</label>
          <input style={input} value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://..." />
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <label style={label}>Loại</label>
              <select style={input} value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                <option value="COMBO">Combo</option>
                <option value="POPCORN">Bắp</option>
                <option value="DRINK">Nước</option>
                <option value="POPCORN_DRINK">Bắp + nước</option>
              </select>
            </div>
            <div>
              <label style={label}>Trạng thái</label>
              <select style={input} value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Đang bán</option>
                <option value="INACTIVE">Ngừng bán</option>
              </select>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <label style={label}>Giá</label>
              <input type="number" min="0" style={input} value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
            </div>
            <div>
              <label style={label}>Tồn kho</label>
              <input type="number" min="0" style={input} value={form.stockQuantity} onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
            <button type="submit" style={primaryButton}>{editingId ? 'Lưu thay đổi' : 'Tạo combo'}</button>
            {editingId && <button type="button" style={mutedButton} onClick={() => { setEditingId(null); setForm(emptyForm); }}>Hủy</button>}
          </div>
        </form>

        <section style={section}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 160px 160px', gap: 10, marginBottom: 14 }}>
            <input style={input} value={filters.keyword} onChange={(e) => updateFilter('keyword', e.target.value)} placeholder="Tìm theo tên hoặc mô tả" />
            <select style={input} value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang bán</option>
              <option value="INACTIVE">Ngừng bán</option>
            </select>
            <input style={input} value={filters.category} onChange={(e) => updateFilter('category', e.target.value)} placeholder="Loại" />
          </div>
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 820 }}>
              <thead>
                <tr>
                  {['Combo', 'Loại', 'Giá', 'Tồn', 'Trạng thái', ''].map((head) => <th key={head} style={th}>{head}</th>)}
                </tr>
              </thead>
              <tbody>
                {combos.map((combo) => (
                  <tr key={combo.id}>
                    <td style={td}>
                      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                        {combo.imageUrl && <img src={combo.imageUrl} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 6 }} />}
                        <div>
                          <strong>{combo.name}</strong>
                          <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{combo.description}</div>
                        </div>
                      </div>
                    </td>
                    <td style={td}>{combo.category || '-'}</td>
                    <td style={td}>{Number(combo.price || 0).toLocaleString('vi-VN')} VND</td>
                    <td style={td}>{combo.stockQuantity || 0}</td>
                    <td style={td}>{combo.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</td>
                    <td style={td}>
                      <button style={miniButton} onClick={() => edit(combo)}>Sửa</button>
                      <button style={miniButton} onClick={() => changeStatus(combo)}>{combo.status === 'ACTIVE' ? 'Ẩn' : 'Bán'}</button>
                      <button style={miniDanger} onClick={() => remove(combo)}>Xóa</button>
                    </td>
                  </tr>
                ))}
                {!loading && combos.length === 0 && <tr><td style={td} colSpan={6}>Chưa có combo.</td></tr>}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>
  );
}

const section = { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: 'var(--shadow-card)' };
const panelTitle = { margin: '0 0 12px', fontSize: 18, fontWeight: 850 };
const label = { display: 'block', marginTop: 12, marginBottom: 6, color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 800 };
const input = { background: 'var(--surface-container-low)', border: '1px solid var(--color-border)', borderRadius: 8, color: 'var(--color-text)', padding: '10px 11px', fontSize: 14, width: '100%' };
const primaryButton = { border: 'none', borderRadius: 8, padding: '10px 14px', background: 'var(--color-primary)', color: 'var(--on-secondary-container)', fontWeight: 800, cursor: 'pointer' };
const mutedButton = { border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 14px', background: 'var(--surface-container-high)', color: 'var(--color-text)', fontWeight: 800, cursor: 'pointer' };
const th = { padding: '12px 14px', textAlign: 'left', fontSize: 12, color: 'var(--color-text-muted)', background: 'var(--surface-container-high)', textTransform: 'uppercase', letterSpacing: 0 };
const td = { padding: '13px 14px', borderTop: '1px solid var(--color-border)', color: 'var(--color-text)', fontSize: 14, verticalAlign: 'top' };
const miniButton = { ...mutedButton, padding: '7px 9px', marginRight: 6, fontSize: 12 };
const miniDanger = { ...miniButton, background: '#7f1d1d', color: '#fecaca' };
const success = { marginTop: 14, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', padding: 12, borderRadius: 8 };
const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
