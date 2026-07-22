import { useState, useEffect, useCallback } from 'react'
import adminService from '../../services/admin.service'

const SCREEN_TYPES = ['2D', '3D', 'IMAX', '4DX', 'VIP']

const EMPTY_FORM = {
  name: '',
  totalRows: '',
  totalColumns: '',
  screenType: '2D',
  priceMultiplier: '1.0',
  status: 'ACTIVE',
}

// ── Styles ─────────────────────────────────
const S = {
  page: { padding: '24px', minHeight: '100vh', background: 'transparent', fontFamily: 'Inter, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%' },
  header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 28 },
  title: { fontSize: 26, fontWeight: 700, color: 'var(--color-text)', margin: 0 },
  subtitle: { fontSize: 14, color: 'var(--color-text-muted)', margin: '4px 0 0' },
  btnPrimary: {
    background: 'var(--color-primary)', color: 'var(--on-secondary-container)', border: 'none',
    borderRadius: 8, padding: '10px 20px', fontWeight: 700, fontSize: 14,
    cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8,
    transition: 'filter 0.15s',
  },
  card: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, boxShadow: 'var(--shadow-card)', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { padding: '13px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.07em', background: 'var(--surface-container-high)', borderBottom: '1px solid var(--color-border)' },
  td: { padding: '14px 16px', fontSize: 14, color: 'var(--color-text)', borderBottom: '1px solid var(--color-border)', verticalAlign: 'middle' },
  badge: (active) => ({
    display: 'inline-flex', alignItems: 'center', gap: 5,
    padding: '3px 10px', borderRadius: 999, fontSize: 12, fontWeight: 700,
    background: active ? 'rgba(22, 163, 74, 0.18)' : 'var(--surface-container-high)',
    color: active ? '#22c55e' : 'var(--color-text-muted)',
  }),
  iconBtn: (color) => ({
    background: 'transparent', border: 'none', cursor: 'pointer',
    padding: '6px 8px', borderRadius: 7, transition: 'background 0.15s',
    color, fontSize: 18,
  }),
  overlay: { position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.55)', backdropFilter: 'blur(3px)', zIndex: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' },
  modal: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '36px', width: 480, maxWidth: '95vw', boxShadow: '0 25px 60px rgba(0,0,0,0.35)' },
  modalTitle: { fontSize: 20, fontWeight: 700, color: 'var(--color-text)', margin: '0 0 24px' },
  label: { display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6 },
  input: { width: '100%', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 14px', fontSize: 14, color: 'var(--color-text)', background: 'var(--surface-container-low)', outline: 'none', boxSizing: 'border-box', fontFamily: 'Inter, sans-serif' },
  formGroup: { marginBottom: 18 },
  row2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 },
  btnRow: { display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 28 },
  btnSecondary: { background: 'var(--surface-container-high)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 20px', fontWeight: 700, fontSize: 14, cursor: 'pointer' },
  errorBox: { background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: '10px 14px', marginBottom: 16, fontSize: 13 },
  loadingBox: { textAlign: 'center', padding: 60, color: 'var(--color-text-muted)', fontSize: 15 },
}

// ── Component ──────────────────────────────
export default function TheaterManagement() {
  const [rooms, setRooms]         = useState([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState(null)
  const [saving, setSaving]       = useState(false)
  const [modal, setModal]         = useState(null)   // null | 'add' | 'edit'
  const [form, setForm]           = useState(EMPTY_FORM)
  const [message, setMessage]     = useState('')
  const [editId, setEditId]       = useState(null)
  const [search, setSearch]       = useState('')
  const [statusTarget, setStatusTarget] = useState(null)  // { id, currentStatus }

  const fetchRooms = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await adminService.getRooms(search || undefined)
      setRooms(res.data)
    } catch (e) {
      setError('Không thể tải danh sách phòng chiếu. Kiểm tra kết nối backend.')
    } finally {
      setLoading(false)
    }
  }, [search])

  useEffect(() => {
    const timer = setTimeout(() => fetchRooms(), 400)
    return () => clearTimeout(timer)
  }, [fetchRooms])

  const openAdd  = () => { setForm(EMPTY_FORM); setEditId(null); setModal('add') }
  const openEdit = (r) => {
    setForm({
      name: r.name,
      totalRows: String(r.totalRows),
      totalColumns: String(r.totalColumns),
      screenType: r.screenType || '2D',
      priceMultiplier: String(r.priceMultiplier || '1.0'),
      status: r.status,
    })
    setEditId(r.id)
    setModal('edit')
  }
  const closeModal = () => setModal(null)

  const handleSave = async () => {
    setError(null)
    setMessage('')
    if (!form.name.trim()) return setError('Tên phòng không được để trống.')
    if (Number(form.totalRows) <= 0) return setError('Số hàng ghế phải lớn hơn 0.')
    if (Number(form.totalColumns) <= 0) return setError('Số cột ghế phải lớn hơn 0.')
    if (modal === 'edit') {
      const current = rooms.find((room) => room.id === editId)
      const gridChanged = current && (Number(current.totalRows) !== Number(form.totalRows) || Number(current.totalColumns) !== Number(form.totalColumns))
      if (gridChanged && !window.confirm('Thay đổi số hàng/cột có thể làm thay đổi sơ đồ ghế. Bạn có chắc chắn muốn tiếp tục?')) return
    }
    setSaving(true)
    try {
      const payload = {
        name: form.name.trim(),
        totalRows: Number(form.totalRows),
        totalColumns: Number(form.totalColumns),
        screenType: form.screenType,
        priceMultiplier: Number(form.priceMultiplier),
        status: form.status,
      }
      if (modal === 'add') {
        await adminService.createRoom(payload)
        setMessage('Đã tạo phòng và sơ đồ ghế.')
      } else {
        await adminService.updateRoom(editId, payload)
        setMessage('Đã cập nhật phòng và sơ đồ ghế.')
      }
      closeModal()
      fetchRooms()
    } catch (e) {
      setError(e.response?.data?.message || e.response?.data || 'Lưu phòng thất bại.')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async () => {
    if (!statusTarget) return
    const newStatus = statusTarget.currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await adminService.updateRoomStatus(statusTarget.id, newStatus)
      setStatusTarget(null)
      fetchRooms()
    } catch (e) {
      alert('Cập nhật trạng thái thất bại: ' + (e.response?.data?.message || e.message))
    }
  }

  const f = (k) => (e) => setForm(p => ({ ...p, [k]: e.target.value }))

  const filtered = rooms.filter(r =>
    r.name?.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div style={S.page}>
      {/* Header */}
      <div style={S.header}>
        <div>
          <h1 style={S.title}>🏛️ Quản lý Phòng chiếu</h1>
          <p style={S.subtitle}>Quản lý danh sách phòng, cấu hình và trạng thái hoạt động</p>
        </div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div style={{ position: 'relative' }}>
            <span className="material-symbols-outlined" style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--color-text-muted)', fontSize: 18 }}>search</span>
            <input
              placeholder="Tìm phòng..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ ...S.input, paddingLeft: 40, width: 220 }}
            />
          </div>
          <button style={S.btnPrimary} onClick={openAdd}
            onMouseEnter={e => e.currentTarget.style.filter = 'brightness(1.1)'}
            onMouseLeave={e => e.currentTarget.style.filter = 'none'}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
            Thêm Phòng
          </button>
        </div>
      </div>

      {/* Stats row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        {[
          { label: 'Tổng phòng', value: rooms.length, icon: 'meeting_room', color: 'var(--color-accent)' },
          { label: 'Đang hoạt động', value: rooms.filter(r => r.status === 'ACTIVE').length, icon: 'check_circle', color: '#16a34a' },
          { label: 'Tổng ghế', value: rooms.reduce((s, r) => s + (r.totalSeats || 0), 0), icon: 'chair', color: '#e11d48' },
        ].map(({ label, value, icon, color }) => (
          <div key={label} style={{ ...S.card, padding: '20px 24px', display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ width: 44, height: 44, borderRadius: 10, background: color + '15', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <span className="material-symbols-outlined" style={{ color, fontSize: 22 }}>{icon}</span>
            </div>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, color: 'var(--color-text)' }}>{value}</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-muted)', fontWeight: 600 }}>{label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Error */}
      {error && <div style={S.errorBox}>⚠️ {error}</div>}
      {message && <div style={{ ...S.errorBox, background: 'rgba(22,163,74,0.16)', color: '#bbf7d0', borderColor: 'rgba(34,197,94,0.28)' }}>✅ {message}</div>}

      {/* Table */}
      <div style={S.card}>
        {loading ? (
          <div style={S.loadingBox}>
            <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', marginBottom: 8, opacity: 0.3 }}>hourglass_empty</span>
            Đang tải...
          </div>
        ) : (
          <table style={S.table}>
            <thead>
              <tr>
                {['STT', 'Tên Phòng', 'Hàng × Cột', 'Tổng ghế', 'Loại màn hình', 'Hệ số giá', 'Trạng thái', 'Hành động'].map(h => (
                  <th key={h} style={S.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan={8} style={{ ...S.td, textAlign: 'center', color: 'var(--color-text-muted)', padding: 48 }}>Không tìm thấy phòng nào</td></tr>
              ) : filtered.map((r, i) => (
                <tr key={r.id} style={{ transition: 'background 0.15s' }}
                  onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-container-low)'}
                  onMouseLeave={e => e.currentTarget.style.background = ''}
                >
                  <td style={{ ...S.td, color: 'var(--color-text-muted)', fontWeight: 700, width: 48 }}>{i + 1}</td>
                  <td style={S.td}>
                    <div style={{ fontWeight: 700, color: 'var(--color-text)' }}>{r.name}</div>
                    <div style={{ fontSize: 11, color: 'var(--color-text-muted)' }}>ID: {r.id}</div>
                  </td>
                  <td style={S.td}>{r.totalRows} × {r.totalColumns}</td>
                  <td style={S.td}>
                    <span style={{ fontWeight: 700, color: 'var(--color-text)' }}>{r.totalSeats}</span>
                    <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}> ghế</span>
                  </td>
                  <td style={S.td}>
                    <span style={{ padding: '3px 10px', borderRadius: 999, fontSize: 11, fontWeight: 700, background: 'rgba(2, 132, 199, 0.18)', color: '#38bdf8' }}>
                      {r.screenType || '—'}
                    </span>
                  </td>
                  <td style={S.td}>
                    <span style={{ fontWeight: 700, color: '#7c3aed' }}>×{r.priceMultiplier}</span>
                  </td>
                  <td style={S.td}>
                    <span style={S.badge(r.status === 'ACTIVE')}>
                      <span style={{ width: 6, height: 6, borderRadius: '50%', background: r.status === 'ACTIVE' ? '#22c55e' : 'var(--color-text-muted)', display: 'inline-block' }} />
                      {r.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm dừng'}
                    </span>
                  </td>
                  <td style={S.td}>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button style={S.iconBtn('#3b82f6')} title="Sửa" onClick={() => openEdit(r)}
                        onMouseEnter={e => e.currentTarget.style.background = 'rgba(59, 130, 246, 0.14)'}
                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>edit</span>
                      </button>
                      <button
                        style={S.iconBtn(r.status === 'ACTIVE' ? '#f59e0b' : '#16a34a')}
                        title={r.status === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'}
                        onClick={() => setStatusTarget({ id: r.id, currentStatus: r.status })}
                        onMouseEnter={e => e.currentTarget.style.background = 'rgba(245, 158, 11, 0.14)'}
                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                      >
                        <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                          {r.status === 'ACTIVE' ? 'pause_circle' : 'play_circle'}
                        </span>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Add / Edit Modal */}
      {modal && (
        <div style={S.overlay} onClick={closeModal}>
          <div style={S.modal} onClick={e => e.stopPropagation()}>
            <h2 style={S.modalTitle}>{modal === 'add' ? '➕ Thêm Phòng chiếu mới' : '✏️ Sửa thông tin Phòng'}</h2>

            <div style={S.formGroup}>
              <label style={S.label}>Tên Phòng *</label>
              <input style={S.input} value={form.name} onChange={f('name')} placeholder="VD: Phòng 1 - 2D" />
            </div>
            <div style={S.row2}>
              <div style={S.formGroup}>
                <label style={S.label}>Số hàng *</label>
                <input style={S.input} type="number" min="1" max="30" value={form.totalRows} onChange={f('totalRows')} placeholder="VD: 8" />
              </div>
              <div style={S.formGroup}>
                <label style={S.label}>Số cột *</label>
                <input style={S.input} type="number" min="1" max="50" value={form.totalColumns} onChange={f('totalColumns')} placeholder="VD: 12" />
              </div>
            </div>
            <div style={S.row2}>
              <div style={S.formGroup}>
                <label style={S.label}>Loại màn hình</label>
                <select style={S.input} value={form.screenType} onChange={f('screenType')}>
                  {SCREEN_TYPES.map(t => <option key={t}>{t}</option>)}
                </select>
              </div>
              <div style={S.formGroup}>
                <label style={S.label}>Hệ số giá</label>
                <input style={S.input} type="number" min="0.1" max="10" step="0.1" value={form.priceMultiplier} onChange={f('priceMultiplier')} />
              </div>
            </div>
            <div style={S.formGroup}>
              <label style={S.label}>Trạng thái</label>
              <select style={S.input} value={form.status} onChange={f('status')}>
                <option value="ACTIVE">Hoạt động</option>
                <option value="INACTIVE">Tạm dừng</option>
              </select>
            </div>

            <div style={S.btnRow}>
              <button style={S.btnSecondary} onClick={closeModal}>Hủy</button>
              <button style={{ ...S.btnPrimary, opacity: saving ? 0.7 : 1 }} onClick={handleSave} disabled={saving}>
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>save</span>
                {saving ? 'Đang lưu...' : (modal === 'add' ? 'Thêm Phòng' : 'Lưu thay đổi')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Toggle Status confirm */}
      {statusTarget && (
        <div style={S.overlay} onClick={() => setStatusTarget(null)}>
          <div style={{ ...S.modal, width: 380, textAlign: 'center' }} onClick={e => e.stopPropagation()}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>
              {statusTarget.currentStatus === 'ACTIVE' ? '⏸️' : '▶️'}
            </div>
            <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--color-text)', margin: '0 0 8px' }}>
              {statusTarget.currentStatus === 'ACTIVE' ? 'Tạm dừng phòng này?' : 'Kích hoạt phòng này?'}
            </h3>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 14, margin: '0 0 24px' }}>
              {statusTarget.currentStatus === 'ACTIVE'
                ? 'Phòng sẽ không nhận suất chiếu mới.'
                : 'Phòng sẽ có thể nhận suất chiếu mới.'}
            </p>
            <div style={S.btnRow}>
              <button style={S.btnSecondary} onClick={() => setStatusTarget(null)}>Hủy</button>
              <button
                style={{ ...S.btnPrimary, background: statusTarget.currentStatus === 'ACTIVE' ? '#f59e0b' : '#16a34a' }}
                onClick={handleToggleStatus}
              >
                {statusTarget.currentStatus === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
