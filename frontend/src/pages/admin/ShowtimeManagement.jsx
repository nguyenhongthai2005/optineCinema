import { useState, useEffect, useCallback } from 'react'
import adminService from '../../services/admin.service'

const today = new Date()
const fmtDate = (d) => d.toISOString().split('T')[0]

// Parse ISO datetime → date part "YYYY-MM-DD"
const toDateStr = (isoStr) => isoStr ? isoStr.split('T')[0] : ''
// Parse ISO datetime → time part "HH:MM"
const toTimeStr = (isoStr) => {
  if (!isoStr) return ''
  const t = isoStr.split('T')[1] || ''
  return t.slice(0, 5)
}
// Combine date + time → "YYYY-MM-DDTHH:mm:ss"
const toISO = (date, time) => date && time ? `${date}T${time}:00` : ''

const EMPTY_FORM = {
  movieId: '',
  roomId: '',
  timeSlotId: '',
  date: fmtDate(today),
  startTime: '09:00',
  endTime: '11:30',
  status: 'SCHEDULED',
}

const STATUS_MAP = {
  UPCOMING:   { label: 'Sắp chiếu',  color: '#60a5fa', bg: 'rgba(96, 165, 250, 0.16)' },
  SCHEDULED:  { label: 'Sắp chiếu',  color: '#60a5fa', bg: 'rgba(96, 165, 250, 0.16)' },
  OPEN:       { label: 'Sắp chiếu',  color: '#60a5fa', bg: 'rgba(96, 165, 250, 0.16)' },
  ACTIVE:     { label: 'Sắp chiếu',  color: '#60a5fa', bg: 'rgba(96, 165, 250, 0.16)' },
  PLAYING:    { label: 'Đang chiếu', color: '#22c55e', bg: 'rgba(34, 197, 94, 0.16)' },
  NOW_SHOWING:{ label: 'Đang chiếu', color: '#22c55e', bg: 'rgba(34, 197, 94, 0.16)' },
  ENDED:      { label: 'Đã qua',     color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.14)' },
  PAST:       { label: 'Đã qua',     color: '#94a3b8', bg: 'rgba(148, 163, 184, 0.14)' },
  CANCELLED:  { label: 'Đã hủy',     color: '#f87171', bg: 'rgba(248, 113, 113, 0.16)' },
}

const fmtDateVN = (isoStr) => {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  return d.toLocaleDateString('vi-VN', { weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric' })
}

const S = {
  page:    { padding: '24px', minHeight: '100vh', background: 'transparent', fontFamily: 'Inter, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%' },
  title:   { fontSize: 26, fontWeight: 700, color: 'var(--color-text)', margin: 0 },
  subtitle:{ fontSize: 14, color: 'var(--color-text-muted)', margin: '4px 0 0' },
  card:    { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, boxShadow: 'var(--shadow-card)', overflow: 'hidden' },
  th:      { padding: '12px 16px', textAlign: 'left', fontSize: 11, fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.07em', background: 'var(--surface-container-high)', borderBottom: '1px solid var(--color-border)' },
  td:      { padding: '13px 16px', fontSize: 14, color: 'var(--color-text)', borderBottom: '1px solid var(--color-border)', verticalAlign: 'middle' },
  btnPrimary: { background: 'var(--color-primary)', color: 'var(--on-secondary-container)', border: 'none', borderRadius: 8, padding: '10px 20px', fontWeight: 700, fontSize: 14, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 8, transition: 'filter 0.15s' },
  input:   { width: '100%', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 14px', fontSize: 14, color: 'var(--color-text)', outline: 'none', boxSizing: 'border-box', fontFamily: 'Inter, sans-serif', background: 'var(--surface-container-low)' },
  label:   { display: 'block', fontSize: 13, fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: 6 },
  formGrp: { marginBottom: 16 },
  grid2:   { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 },
  grid3:   { display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 14 },
  overlay: { position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.55)', backdropFilter: 'blur(3px)', zIndex: 200, display: 'flex', alignItems: 'center', justifyContent: 'center' },
  modal:   { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '36px', width: 560, maxWidth: '95vw', boxShadow: '0 25px 60px rgba(0,0,0,0.35)', maxHeight: '90vh', overflowY: 'auto' },
  iconBtn: (c) => ({ background: 'transparent', border: 'none', cursor: 'pointer', padding: '6px 8px', borderRadius: 7, color: c, fontSize: 18, transition: 'background 0.15s' }),
  btnSecondary: { background: 'var(--surface-container-high)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 20px', fontWeight: 700, fontSize: 14, cursor: 'pointer' },
  errorBox: { background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: '10px 14px', marginBottom: 16, fontSize: 13 },
  loadingBox: { textAlign: 'center', padding: 60, color: 'var(--color-text-muted)', fontSize: 15 },
}

export default function ShowtimeManagement() {
  const [showtimes, setShowtimes] = useState([])
  const [movies, setMovies]       = useState([])
  const [rooms, setRooms]         = useState([])
  const [timeslots, setTimeslots] = useState([])

  const [loading, setLoading]   = useState(true)
  const [saving, setSaving]     = useState(false)
  const [error, setError]       = useState(null)

  const [modal, setModal]       = useState(null)   // null | 'add' | 'edit'
  const [form, setForm]         = useState(EMPTY_FORM)
  const [editId, setEditId]     = useState(null)
  const [statusTarget, setStatusTarget] = useState(null) // { id, currentStatus }

  const [filterDate, setFilterDate]     = useState(fmtDate(today))
  const [filterMovieId, setFilterMovieId] = useState('')

  // ── Fetch lookups once ───────────────────────────────────────────
  useEffect(() => {
    Promise.all([
      adminService.getMovies({ status: 'NOW_SHOWING' }),
      adminService.getRooms(),
      adminService.getTimeslots(),
    ]).then(([mRes, rRes, tRes]) => {
      setMovies(mRes.data || [])
      setRooms((rRes.data || []).filter(r => r.status === 'ACTIVE'))
      setTimeslots(tRes.data || [])
    }).catch(() => setError('Không thể tải dữ liệu danh mục (phim / phòng / ca chiếu).'))
  }, [])

  // ── Fetch showtimes ──────────────────────────────────────────────
  const fetchShowtimes = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {}
      if (filterMovieId) params.movieId = filterMovieId
      if (filterDate)    params.date    = filterDate
      const res = await adminService.getShowtimes(params)
      setShowtimes(res.data || [])
    } catch {
      setError('Không thể tải danh sách suất chiếu. Kiểm tra kết nối backend.')
    } finally {
      setLoading(false)
    }
  }, [filterDate, filterMovieId])

  useEffect(() => {
    const t = setTimeout(fetchShowtimes, 300)
    return () => clearTimeout(t)
  }, [fetchShowtimes])

  // ── Helpers ──────────────────────────────────────────────────────
  const f = (k) => (e) => setForm(p => ({ ...p, [k]: e.target.value }))

  const openAdd = () => {
    setForm({ ...EMPTY_FORM, date: filterDate || fmtDate(today) })
    setEditId(null)
    setModal('add')
  }

  const openEdit = (s) => {
    setForm({
      movieId:    String(s.movieId || ''),
      roomId:     String(s.roomId || ''),
      timeSlotId: String(s.timeSlotId || ''),
      date:       toDateStr(s.startTime),
      startTime:  toTimeStr(s.startTime),
      endTime:    toTimeStr(s.endTime),
      status:     s.status === 'CANCELLED' ? 'CANCELLED' : 'SCHEDULED',
    })
    setEditId(s.id)
    setModal('edit')
  }

  const closeModal = () => setModal(null)

  // ── Save (create / update) ───────────────────────────────────────
  const handleSave = async () => {
    if (!form.movieId || !form.roomId || !form.date || !form.startTime || !form.endTime) {
      alert('Vui lòng điền đầy đủ: Phim, Phòng chiếu, Ngày, Giờ bắt đầu, Giờ kết thúc.')
      return
    }
    setSaving(true)
    try {
      const payload = {
        movieId:    Number(form.movieId),
        roomId:     Number(form.roomId),
        timeSlotId: form.timeSlotId ? Number(form.timeSlotId) : null,
        startTime:  toISO(form.date, form.startTime),
        endTime:    toISO(form.date, form.endTime),
        status:     form.status,
      }
      if (modal === 'add') {
        await adminService.createShowtime(payload)
      } else {
        await adminService.updateShowtime(editId, payload)
      }
      closeModal()
      fetchShowtimes()
    } catch (e) {
      alert('Lưu thất bại: ' + (e.response?.data?.message || e.message))
    } finally {
      setSaving(false)
    }
  }

  // ── Toggle status ────────────────────────────────────────────────
  const handleToggleStatus = async (id, newStatus) => {
    try {
      await adminService.updateShowtimeStatus(id, newStatus)
      setStatusTarget(null)
      fetchShowtimes()
    } catch (e) {
      alert('Cập nhật trạng thái thất bại: ' + (e.response?.data?.message || e.message))
    }
  }

  // ── Derived ──────────────────────────────────────────────────────
  const totalToday = showtimes.filter(s => toDateStr(s.startTime) === fmtDate(today)).length

  return (
    <div style={S.page}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 28 }}>
        <div>
          <h1 style={S.title}>🕐 Quản lý Suất chiếu</h1>
          <p style={S.subtitle}>Thiết lập và quản lý lịch chiếu phim theo ngày và phòng</p>
        </div>
        <button style={S.btnPrimary} onClick={openAdd}
          onMouseEnter={e => e.currentTarget.style.filter = 'brightness(1.1)'}
          onMouseLeave={e => e.currentTarget.style.filter = 'none'}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
          Tạo Suất chiếu
        </button>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        {[
          { label: 'Tổng suất chiếu', value: showtimes.length,                                              icon: 'schedule',    color: 'var(--color-accent)' },
          { label: 'Hôm nay',          value: totalToday,                                                    icon: 'today',       color: '#0284c7' },
          { label: 'Sắp chiếu',        value: showtimes.filter(s => ['UPCOMING', 'SCHEDULED', 'OPEN', 'ACTIVE'].includes(s.displayStatus || s.status)).length, icon: 'play_circle', color: '#60a5fa' },
          { label: 'Đã hủy',           value: showtimes.filter(s => s.status === 'CANCELLED').length,        icon: 'cancel',      color: '#dc2626' },
        ].map(({ label, value, icon, color }) => (
          <div key={label} style={{ ...S.card, padding: '18px 20px', display: 'flex', alignItems: 'center', gap: 14 }}>
            <div style={{ width: 42, height: 42, borderRadius: 10, background: color + '15', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <span className="material-symbols-outlined" style={{ color, fontSize: 20 }}>{icon}</span>
            </div>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: 'var(--color-text)' }}>{value}</div>
              <div style={{ fontSize: 12, color: 'var(--color-text-muted)', fontWeight: 600 }}>{label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div>
          <label style={{ ...S.label, fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.07em' }}>Lọc theo ngày</label>
          <input type="date" style={{ ...S.input, width: 180 }} value={filterDate} onChange={e => setFilterDate(e.target.value)} />
        </div>
        <div>
          <label style={{ ...S.label, fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.07em' }}>Lọc theo Phim</label>
          <select style={{ ...S.input, width: 240 }} value={filterMovieId} onChange={e => setFilterMovieId(e.target.value)}>
            <option value="">-- Tất cả phim --</option>
            {movies.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
          </select>
        </div>
        {(filterDate || filterMovieId) && (
          <button onClick={() => { setFilterDate(''); setFilterMovieId('') }}
            style={{ alignSelf: 'flex-end', padding: '10px 14px', borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--surface-container-high)', color: 'var(--color-text)', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}>
            Xóa bộ lọc ✕
          </button>
        )}
        <div style={{ marginLeft: 'auto', alignSelf: 'flex-end', fontSize: 13, color: 'var(--color-text-muted)', fontWeight: 600 }}>
          {showtimes.length} suất chiếu
        </div>
      </div>

      {/* Error */}
      {error && <div style={S.errorBox}>⚠️ {error}</div>}

      {/* Table */}
      <div style={S.card}>
        {loading ? (
          <div style={S.loadingBox}>
            <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', marginBottom: 8, opacity: 0.3 }}>hourglass_empty</span>
            Đang tải...
          </div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                {['STT', 'Phim', 'Phòng chiếu', 'Ngày chiếu', 'Giờ chiếu', 'Ca chiếu', 'Ghế trống / Tổng', 'Trạng thái', 'Hành động'].map(h => (
                  <th key={h} style={S.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {showtimes.length === 0 ? (
                <tr><td colSpan={9} style={{ ...S.td, textAlign: 'center', color: 'var(--color-text-muted)', padding: 56 }}>
                  <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', marginBottom: 8 }}>event_busy</span>
                  Không có suất chiếu nào
                </td></tr>
              ) : showtimes.map((s, i) => {
                const displayStatus = s.displayStatus || s.status || 'SCHEDULED'
                const st = STATUS_MAP[displayStatus] || STATUS_MAP.SCHEDULED
                return (
                  <tr key={s.id}
                    onMouseEnter={e => e.currentTarget.style.background = 'var(--surface-container-low)'}
                    onMouseLeave={e => e.currentTarget.style.background = ''}
                  >
                    <td style={{ ...S.td, color: 'var(--color-text-muted)', fontWeight: 700, width: 48 }}>{i + 1}</td>
                    <td style={S.td}>
                      <div style={{ fontWeight: 700, color: 'var(--color-text)', maxWidth: 200 }}>{s.movieTitle}</div>
                      <div style={{ fontSize: 11, color: 'var(--color-text-muted)' }}>ID: {s.movieId}</div>
                    </td>
                    <td style={S.td}>
                      <div style={{ fontWeight: 600, color: 'var(--color-text)', fontSize: 13 }}>{s.roomName}</div>
                      {s.screenType && (
                        <span style={{ padding: '2px 8px', borderRadius: 999, fontSize: 10, fontWeight: 700, background: 'rgba(2, 132, 199, 0.18)', color: '#38bdf8' }}>
                          {s.screenType}
                        </span>
                      )}
                    </td>
                    <td style={S.td}>
                      <div style={{ fontWeight: 600 }}>{fmtDateVN(s.startTime)}</div>
                    </td>
                    <td style={S.td}>
                      <div style={{ fontWeight: 800, color: 'var(--color-text)', fontSize: 15 }}>{toTimeStr(s.startTime)}</div>
                      <div style={{ fontSize: 11, color: 'var(--color-text-muted)' }}>→ {toTimeStr(s.endTime)}</div>
                    </td>
                    <td style={S.td}>
                      {s.timeSlotName
                        ? <span style={{ padding: '3px 10px', borderRadius: 999, fontSize: 11, fontWeight: 700, background: 'rgba(124, 58, 237, 0.18)', color: '#c4b5fd' }}>{s.timeSlotName}</span>
                        : <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>—</span>}
                    </td>
                    <td style={S.td}>
                      <span style={{ fontWeight: 700, color: '#16a34a' }}>{s.availableSeats ?? '—'}</span>
                      <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}> / {s.totalSeats ?? '—'}</span>
                    </td>
                    <td style={S.td}>
                      <span style={{ padding: '3px 10px', borderRadius: 999, fontSize: 12, fontWeight: 700, background: st.bg, color: st.color }}>
                        {s.displayStatusLabel || st.label}
                      </span>
                    </td>
                    <td style={S.td}>
                      <div style={{ display: 'flex', gap: 4 }}>
                        {/* Edit */}
                        <button style={S.iconBtn('#3b82f6')} title="Sửa" onClick={() => openEdit(s)}
                          onMouseEnter={e => e.currentTarget.style.background = 'rgba(59, 130, 246, 0.14)'}
                          onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                        ><span className="material-symbols-outlined" style={{ fontSize: 18 }}>edit</span></button>

                        {/* Cancel / Restore */}
                        {s.status !== 'CANCELLED' ? (
                          <button style={S.iconBtn('#ef4444')} title="Hủy suất chiếu"
                            onClick={() => setStatusTarget({ id: s.id, newStatus: 'CANCELLED', label: 'Hủy suất chiếu này?' })}
                            onMouseEnter={e => e.currentTarget.style.background = 'rgba(239, 68, 68, 0.14)'}
                            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                          ><span className="material-symbols-outlined" style={{ fontSize: 18 }}>cancel</span></button>
                        ) : (
                          <button style={S.iconBtn('#16a34a')} title="Khôi phục"
                            onClick={() => setStatusTarget({ id: s.id, newStatus: 'SCHEDULED', label: 'Khôi phục suất chiếu này?' })}
                            onMouseEnter={e => e.currentTarget.style.background = 'rgba(22, 163, 74, 0.14)'}
                            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                          ><span className="material-symbols-outlined" style={{ fontSize: 18 }}>restart_alt</span></button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Add / Edit Modal */}
      {modal && (
        <div style={S.overlay} onClick={closeModal}>
          <div style={S.modal} onClick={e => e.stopPropagation()}>
            <h2 style={{ fontSize: 20, fontWeight: 700, color: 'var(--color-text)', margin: '0 0 24px' }}>
              {modal === 'add' ? '➕ Tạo Suất chiếu mới' : '✏️ Sửa Suất chiếu'}
            </h2>

            {/* Movie */}
            <div style={S.formGrp}>
              <label style={S.label}>Phim *</label>
              <select style={S.input} value={form.movieId} onChange={f('movieId')}>
                <option value="">-- Chọn phim --</option>
                {movies.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
              </select>
            </div>

            {/* Room */}
            <div style={S.formGrp}>
              <label style={S.label}>Phòng chiếu *</label>
              <select style={S.input} value={form.roomId} onChange={f('roomId')}>
                <option value="">-- Chọn phòng --</option>
                {rooms.map(r => <option key={r.id} value={r.id}>{r.name} ({r.screenType || 'Standard'})</option>)}
              </select>
            </div>

            {/* TimeSlot */}
            <div style={S.formGrp}>
              <label style={S.label}>Ca chiếu (tuỳ chọn)</label>
              <select style={S.input} value={form.timeSlotId} onChange={f('timeSlotId')}>
                <option value="">-- Không chọn ca --</option>
                {timeslots.map(t => (
                  <option key={t.id} value={t.id}>
                    {t.name} ({t.startTime} – {t.endTime})
                  </option>
                ))}
              </select>
            </div>

            {/* Date + Times */}
            <div style={S.grid3}>
              <div style={S.formGrp}>
                <label style={S.label}>Ngày chiếu *</label>
                <input type="date" style={S.input} value={form.date} onChange={f('date')} />
              </div>
              <div style={S.formGrp}>
                <label style={S.label}>Giờ bắt đầu *</label>
                <input type="time" style={S.input} value={form.startTime} onChange={f('startTime')} />
              </div>
              <div style={S.formGrp}>
                <label style={S.label}>Giờ kết thúc *</label>
                <input type="time" style={S.input} value={form.endTime} onChange={f('endTime')} />
              </div>
            </div>

            {/* Status */}
            <div style={S.formGrp}>
              <label style={S.label}>Trạng thái</label>
              <select style={S.input} value={form.status} onChange={f('status')}>
                <option value="SCHEDULED">Sắp chiếu</option>
                <option value="CANCELLED">Đã hủy</option>
              </select>
              <div style={{ color: 'var(--color-text-muted)', fontSize: 12, marginTop: 6 }}>
                Trạng thái Đang chiếu và Đã qua được hệ thống tự động xác định theo thời gian.
              </div>
            </div>

            <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', marginTop: 8 }}>
              <button style={S.btnSecondary} onClick={closeModal}>Hủy</button>
              <button style={{ ...S.btnPrimary, opacity: saving ? 0.7 : 1 }} onClick={handleSave} disabled={saving}>
                <span className="material-symbols-outlined" style={{ fontSize: 16 }}>save</span>
                {saving ? 'Đang lưu...' : (modal === 'add' ? 'Tạo Suất chiếu' : 'Lưu thay đổi')}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Status confirm dialog */}
      {statusTarget && (
        <div style={S.overlay} onClick={() => setStatusTarget(null)}>
          <div style={{ ...S.modal, width: 380, textAlign: 'center' }} onClick={e => e.stopPropagation()}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>
              {statusTarget.newStatus === 'CANCELLED' ? '🚫' : '🔄'}
            </div>
            <h3 style={{ fontSize: 18, fontWeight: 700, color: 'var(--color-text)', margin: '0 0 8px' }}>{statusTarget.label}</h3>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 14, margin: '0 0 24px' }}>
              {statusTarget.newStatus === 'CANCELLED'
                ? 'Suất chiếu sẽ bị hủy và khách hàng không thể đặt vé.'
                : 'Suất chiếu sẽ được mở lại với trạng thái Sắp chiếu.'}
            </p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
              <button style={S.btnSecondary} onClick={() => setStatusTarget(null)}>Hủy bỏ</button>
              <button
                style={{ ...S.btnPrimary, background: statusTarget.newStatus === 'CANCELLED' ? '#ef4444' : '#16a34a' }}
                onClick={() => handleToggleStatus(statusTarget.id, statusTarget.newStatus)}
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
