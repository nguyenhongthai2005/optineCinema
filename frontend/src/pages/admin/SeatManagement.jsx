import { useState, useEffect, useCallback } from 'react'
import adminService from '../../services/admin.service'

// Seat type config (label, color, bg)
const SEAT_TYPE_CONFIG = {
  NORMAL:  { label: 'Thường',  color: '#60a5fa', bg: '#172554', price: '' },
  VIP:     { label: 'VIP',     color: '#c084fc', bg: '#2e1065', price: '' },
  COUPLE:  { label: 'Đôi',     color: '#fb7185', bg: '#4c0519', price: '' },
  MAINTENANCE:{ label: 'Bảo trì', color: '#f87171', bg: '#450a0a', price: '' },
  INACTIVE:{ label: 'Ngừng hoạt động', color: 'var(--color-text-muted)', bg: 'var(--surface-container-high)', price: '' },
}

const S = {
  page:     { padding: '24px', minHeight: '100vh', background: 'transparent', fontFamily: 'Inter, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%' },
  title:    { fontSize: 26, fontWeight: 700, color: 'var(--color-text)', margin: 0 },
  subtitle: { fontSize: 14, color: 'var(--color-text-muted)', margin: '4px 0 0' },
  card:     { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, boxShadow: 'var(--shadow-card)', padding: 20 },
  select:   { border: '1px solid var(--color-border)', borderRadius: 8, padding: '9px 14px', fontSize: 14, color: 'var(--color-text)', outline: 'none', background: 'var(--surface-container-low)', cursor: 'pointer', fontFamily: 'Inter, sans-serif', minWidth: 260 },
  label:    { fontSize: 12, fontWeight: 700, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: 6, display: 'block' },
  btnType:  (active, color) => ({
    padding: '7px 16px', borderRadius: 8, border: `2px solid ${active ? color : 'var(--color-border)'}`,
    background: active ? color + '20' : 'var(--surface-container-low)', color: active ? color : 'var(--color-text-muted)',
    fontWeight: 700, fontSize: 13, cursor: 'pointer', transition: 'all 0.15s',
  }),
  seatBtn:  (type, status, selected) => {
    const normalizedType = status === 'MAINTENANCE' ? 'MAINTENANCE' : status === 'INACTIVE' ? 'INACTIVE' : (type?.toUpperCase() || 'NORMAL')
    const t = SEAT_TYPE_CONFIG[normalizedType] || SEAT_TYPE_CONFIG.NORMAL
    return {
      width: 34, height: 34,
      borderRadius: normalizedType === 'COUPLE' ? 8 : 6,
      border: `2px solid ${selected ? 'var(--color-accent)' : t.color}`,
      background: selected ? 'var(--color-accent)' : t.bg,
      color: selected ? '#111827' : t.color,
      fontSize: 10, fontWeight: 700,
      cursor: status === 'INACTIVE' ? 'not-allowed' : 'pointer',
      transition: 'all 0.12s',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      opacity: status === 'INACTIVE' ? 0.4 : 1,
    }
  },
  errorBox:   { background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: '10px 14px', marginBottom: 16, fontSize: 13 },
  loadingBox: { textAlign: 'center', padding: 60, color: 'var(--color-text-muted)', fontSize: 15 },
}

export default function SeatManagement() {
  const [rooms, setRooms]       = useState([])
  const [roomId, setRoomId]     = useState(null)
  const [seats, setSeats]       = useState([])
  const [selected, setSelected] = useState([])
  const [changeTo, setChangeTo] = useState('VIP')
  const [loadingRooms, setLoadingRooms] = useState(true)
  const [loadingSeats, setLoadingSeats] = useState(false)
  const [saving, setSaving]     = useState(false)
  const [error, setError]       = useState(null)
  const [message, setMessage]   = useState('')

  // Load danh sách phòng
  useEffect(() => {
    adminService.getRooms()
      .then(res => {
        setRooms(res.data || [])
        if ((res.data || []).length > 0) setRoomId(res.data[0].id)
      })
      .catch(() => setError('Không thể tải danh sách phòng chiếu.'))
      .finally(() => setLoadingRooms(false))
  }, [])

  // Load ghế khi chọn phòng
  const fetchSeats = useCallback(async (rid) => {
    if (!rid) return
    setLoadingSeats(true)
    setSelected([])
    setError(null)
    try {
      const res = await adminService.getSeatsByRoom(rid)
      setSeats(res.data)
    } catch {
      setError('Không thể tải danh sách ghế.')
    } finally {
      setLoadingSeats(false)
    }
  }, [])

  useEffect(() => {
    if (roomId) fetchSeats(roomId)
  }, [roomId, fetchSeats])

  const handleRoomChange = (rid) => {
    setRoomId(Number(rid))
  }

  const toggleSelectedSeat = (s) => {
    if (s.status === 'INACTIVE') return
    setSelected(p => p.includes(s.id) ? p.filter(x => x !== s.id) : [...p, s.id])
  }

  const handleSeatClick = async (seat, event) => {
    if (seat.status === 'INACTIVE') return
    if (event.ctrlKey || event.metaKey) {
      toggleSelectedSeat(seat)
      return
    }
    await cycleSeatType(seat)
  }

  const cycleSeatType = async (seat) => {
    const order = ['NORMAL', 'VIP', 'COUPLE']
    const current = seat.seatType?.toUpperCase() || 'NORMAL'
    const next = order[(order.indexOf(current) + 1) % order.length] || 'NORMAL'
    setSaving(true)
    setError(null)
    setMessage('')
    try {
      const res = await adminService.updateSeatType(seat.id, next)
      setSeats((prev) => prev.map((item) => item.id === seat.id ? res.data : item))
      setMessage('Đã cập nhật loại ghế.')
    } catch (e) {
      setError(e.response?.data?.message || e.response?.data || 'Cập nhật loại ghế thất bại.')
    } finally {
      setSaving(false)
    }
  }

  // Đổi loại ghế cho các ghế đã chọn
  const applyChange = async () => {
    if (selected.length === 0) return
    setSaving(true)
    try {
      const res = await adminService.updateSeatTypes(selected, changeTo)
      const updated = new Map((res.data || []).map((seat) => [seat.id, seat]))
      setSeats((prev) => prev.map((seat) => updated.get(seat.id) || seat))
      setSelected([])
      setMessage('Đã cập nhật loại ghế.')
    } catch (e) {
      setError('Cập nhật ghế thất bại: ' + (e.response?.data?.message || e.message))
    } finally {
      setSaving(false)
    }
  }

  const toggleSeatGroup = (ids) => {
    if (!ids.length) return
    setSelected((prev) => {
      const allSelected = ids.every((id) => prev.includes(id))
      return allSelected
        ? prev.filter((id) => !ids.includes(id))
        : [...new Set([...prev, ...ids])]
    })
  }

  const rowSeatIds = (row) => seats
    .filter((seat) => seat.rowLabel === row && seat.status !== 'INACTIVE')
    .map((seat) => seat.id)

  const columnSeatIds = (column) => seats
    .filter((seat) => seat.columnNumber === column && seat.status !== 'INACTIVE')
    .map((seat) => seat.id)

  const isGroupSelected = (ids) => ids.length > 0 && ids.every((id) => selected.includes(id))

  const applyMaintenance = async (maintenance) => {
    if (selected.length === 0) return
    const ok = window.confirm(maintenance
      ? 'Bạn có chắc chắn muốn chuyển các ghế đã chọn sang trạng thái bảo trì không?\nKhách hàng sẽ không thể chọn các ghế này.'
      : 'Bạn có chắc chắn muốn khôi phục các ghế đã chọn về trạng thái hoạt động không?')
    if (!ok) return
    setSaving(true)
    setError(null)
    setMessage('')
    try {
      const res = await adminService.updateSeatMaintenanceBulk(selected, maintenance)
      const updated = new Map((res.data || []).map((seat) => [seat.id, seat]))
      setSeats((prev) => prev.map((seat) => updated.get(seat.id) || seat))
      setSelected([])
      setMessage(maintenance ? 'Đã chuyển ghế sang bảo trì.' : 'Đã khôi phục ghế hoạt động.')
    } catch (e) {
      setError(e.response?.data?.message || e.response?.data || 'Cập nhật trạng thái bảo trì thất bại.')
    } finally {
      setSaving(false)
    }
  }

  const generateSeats = async () => {
    const room = rooms.find(r => r.id === roomId)
    if (!room) return
    setLoadingSeats(true)
    setError(null)
    setMessage('')
    try {
      await adminService.generateRoomSeats(room.id, room.totalRows || room.rowCount, room.totalColumns || room.columnCount)
      await fetchSeats(room.id)
      setMessage('Đã tạo sơ đồ ghế cho phòng.')
    } catch (e) {
      setError(e.response?.data?.message || e.response?.data || 'Không thể tạo sơ đồ ghế.')
    } finally {
      setLoadingSeats(false)
    }
  }

  // Build grid từ dữ liệu thực tế
  const rows    = [...new Set(seats.map(s => s.rowLabel))].sort()
  const cols    = [...new Set(seats.map(s => s.columnNumber))].sort((a, b) => a - b)
  const seatMap = {}
  seats.forEach(s => { seatMap[`${s.rowLabel}-${s.columnNumber}`] = s })

  // Count by type
  const countByType = {}
  seats.forEach(s => {
    const k = s.status === 'MAINTENANCE' ? 'MAINTENANCE' : s.status === 'INACTIVE' ? 'INACTIVE' : (s.seatType?.toUpperCase() || 'NORMAL')
    countByType[k] = (countByType[k] || 0) + 1
  })
  const selectedRoom = rooms.find(r => r.id === roomId)
  const activeSeats = seats.filter(s => s.status !== 'INACTIVE').length
  const selectedLabels = seats
    .filter((seat) => selected.includes(seat.id))
    .map((seat) => seat.seatLabel || `${seat.rowLabel}${seat.columnNumber}`)
  const selectedPreview = selectedLabels.length > 8
    ? `${selectedLabels.slice(0, 8).join(', ')}...`
    : selectedLabels.join(', ')

  return (
    <div style={S.page}>
      {/* Header */}
      <div style={{ marginBottom: 28 }}>
        <h1 style={S.title}>Quản lý ghế</h1>
        <p style={S.subtitle}>Quản lý sơ đồ ghế theo từng phòng chiếu</p>
      </div>

      {error && <div style={S.errorBox}>⚠️ {error}</div>}
      {message && <div style={{ ...S.errorBox, background: 'rgba(22,163,74,0.16)', color: '#bbf7d0', borderColor: 'rgba(34,197,94,0.28)' }}>✅ {message}</div>}

      <div style={{ ...S.card, marginBottom: 16 }}>
        <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--color-text)', marginBottom: 8 }}>Hướng dẫn thao tác:</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 8, color: 'var(--color-text-muted)', fontSize: 13 }}>
          <div>Nhấp vào ghế để đổi nhanh loại ghế.</div>
          <div>Giữ Ctrl và nhấp để chọn nhiều ghế cụ thể.</div>
          <div>Nhấn nút Chọn ở đầu hàng để chọn toàn bộ hàng.</div>
          <div>Nhấn số cột để chọn toàn bộ cột.</div>
          <div>Sau khi chọn ghế, chọn loại ghế trong bảng thao tác rồi nhấn Áp dụng.</div>
          <div>Dùng Bảo trì để đánh dấu ghế không thể bán.</div>
        </div>
      </div>

      {/* Room selector */}
      <div style={{ ...S.card, display: 'flex', gap: 16, marginBottom: 20, flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between' }}>
        <div>
          <label style={S.label}>Chọn phòng chiếu</label>
          {loadingRooms ? (
            <div style={{ ...S.select, color: 'var(--color-text-muted)' }}>Đang tải...</div>
          ) : (
            <select style={S.select} value={roomId || ''} onChange={e => handleRoomChange(e.target.value)}>
              {rooms.length === 0
                ? <option>Không có phòng nào</option>
                : rooms.map(r => <option key={r.id} value={r.id}>{r.name}</option>)
              }
            </select>
          )}
        </div>
        <div style={{ display: 'flex', gap: 14, alignItems: 'center', flexWrap: 'wrap' }}>
          {selectedRoom && (
            <div style={{ fontSize: 13, color: 'var(--color-text-muted)' }}>
              <strong style={{ color: 'var(--color-text)' }}>{selectedRoom.name}</strong> · {selectedRoom.screenType || selectedRoom.roomType || '2D'} · {selectedRoom.totalRows} hàng × {selectedRoom.totalColumns} cột · {seats.length} ghế
            </div>
          )}
          <button onClick={() => fetchSeats(roomId)} disabled={!roomId || loadingSeats} style={{ background: 'var(--surface-container-high)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '9px 12px', fontWeight: 700, cursor: roomId ? 'pointer' : 'not-allowed' }}>
            Làm mới
          </button>
        </div>
      </div>

      {!roomId ? (
        <div style={S.card}>
          <div style={S.loadingBox}>Vui lòng chọn phòng để xem sơ đồ ghế.</div>
        </div>
      ) : (
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 320px', gap: 20, alignItems: 'start' }}>

        {/* Seat Map */}
        <div style={{ ...S.card, minWidth: 0 }}>
          {loadingSeats ? (
            <div style={S.loadingBox}>
              <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', marginBottom: 8, opacity: 0.3 }}>hourglass_empty</span>
              Đang tải sơ đồ ghế...
            </div>
          ) : seats.length === 0 ? (
            <div style={S.loadingBox}>
              <span className="material-symbols-outlined" style={{ fontSize: 40, display: 'block', marginBottom: 8, opacity: 0.3 }}>chair</span>
              Phòng này chưa có sơ đồ ghế.
              {selectedRoom && (
                <div style={{ marginTop: 14 }}>
                  <button onClick={generateSeats} style={{ background: 'var(--color-primary)', color: 'var(--on-secondary-container)', border: 'none', borderRadius: 8, padding: '10px 14px', fontWeight: 800, cursor: 'pointer' }}>
                    Tạo sơ đồ ghế
                  </button>
                </div>
              )}
            </div>
          ) : (
            <>
              <div style={{ textAlign: 'center', marginBottom: 24 }}>
                <div style={{ height: 8, background: 'linear-gradient(90deg,transparent,var(--color-accent),transparent)', borderRadius: 4, maxWidth: 420, margin: '0 auto 8px' }} />
                <span style={{ fontSize: 11, fontWeight: 800, color: 'var(--color-text-muted)', letterSpacing: '0.16em' }}>MÀN HÌNH</span>
              </div>

              <div style={{ overflowX: 'auto', paddingBottom: 8 }}>
                <div style={{ width: 'max-content', minWidth: '100%', display: 'grid', justifyContent: 'center' }}>
                  {rows.map(row => (
                    <div key={row} style={{ display: 'flex', alignItems: 'center', marginBottom: 6, gap: 5 }}>
                      <span style={{ width: 22, fontSize: 11, fontWeight: 800, color: 'var(--color-text-muted)', textAlign: 'right', flexShrink: 0 }}>{row}</span>
                      <button
                        type="button"
                        onClick={() => toggleSeatGroup(rowSeatIds(row))}
                        title={`Chọn hàng ${row}`}
                        style={{
                          width: 44,
                          height: 24,
                          borderRadius: 6,
                          border: isGroupSelected(rowSeatIds(row)) ? '1px solid var(--color-accent)' : '1px solid var(--color-border)',
                          background: isGroupSelected(rowSeatIds(row)) ? 'rgba(250,204,21,0.16)' : 'var(--surface-container-high)',
                          color: isGroupSelected(rowSeatIds(row)) ? '#fde68a' : 'var(--color-text-muted)',
                          fontSize: 10,
                          fontWeight: 800,
                          cursor: 'pointer',
                          flexShrink: 0,
                        }}
                      >
                        Chọn
                      </button>
                      <div style={{ display: 'flex', gap: 5, marginLeft: 8 }}>
                        {cols.map(col => {
                          const seat = seatMap[`${row}-${col}`]
                          if (!seat) return <div key={col} style={{ width: 34, height: 34 }} />
                          const sel = selected.includes(seat.id)
                          const typeKey = seat.status === 'INACTIVE' ? 'INACTIVE' : (seat.seatType?.toUpperCase() || 'NORMAL')
                          const cfg = SEAT_TYPE_CONFIG[typeKey] || SEAT_TYPE_CONFIG.NORMAL
                          return (
                            <button
                              key={seat.id}
                              style={S.seatBtn(seat.seatType, seat.status, sel)}
                              onClick={(event) => handleSeatClick(seat, event)}
                              title={`${seat.seatLabel || `${seat.rowLabel}${seat.columnNumber}`} — ${seat.seatTypeLabel || cfg.label}`}
                            >
                              {col}
                            </button>
                          )
                        })}
                      </div>
                    </div>
                  ))}
                  <div style={{ display: 'flex', gap: 5, marginLeft: 87, marginTop: 4 }}>
                    {cols.map(c => (
                      <button
                        key={c}
                        type="button"
                        onClick={() => toggleSeatGroup(columnSeatIds(c))}
                        title={`Chọn cột ${c}`}
                        style={{
                          width: 34,
                          height: 24,
                          borderRadius: 6,
                          border: isGroupSelected(columnSeatIds(c)) ? '1px solid var(--color-accent)' : '1px solid transparent',
                          background: isGroupSelected(columnSeatIds(c)) ? 'rgba(250,204,21,0.16)' : 'transparent',
                          color: isGroupSelected(columnSeatIds(c)) ? '#fde68a' : 'var(--color-text-muted)',
                          fontSize: 10,
                          fontWeight: 800,
                          cursor: 'pointer',
                        }}
                      >
                        {c}
                      </button>
                    ))}
                  </div>
                </div>
              </div>
              <div style={{
                marginTop: 18,
                paddingTop: 14,
                borderTop: '1px solid var(--color-border)',
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                flexWrap: 'wrap',
              }}>
                <div style={{ minWidth: 110, color: 'var(--color-text)', fontWeight: 800, fontSize: 13 }}>
                  Đã chọn: {selected.length} ghế
                </div>
                <div style={{ flex: '1 1 180px', minWidth: 0, color: 'var(--color-text-muted)', fontSize: 12, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                  {selected.length > 0 ? selectedPreview : 'Chọn ghế, hàng hoặc cột để thao tác.'}
                </div>
                <label style={{ color: 'var(--color-text-muted)', fontSize: 12, fontWeight: 800 }}>Loại ghế</label>
                <select value={changeTo} onChange={(e) => setChangeTo(e.target.value)} style={{ ...S.select, minWidth: 150, padding: '8px 10px' }}>
                  <option value="NORMAL">Ghế thường</option>
                  <option value="VIP">Ghế VIP</option>
                  <option value="COUPLE">Ghế đôi</option>
                </select>
                <button disabled={selected.length === 0 || saving} onClick={applyChange} style={compactButton(selected.length > 0 && !saving, 'var(--color-primary)', 'var(--on-secondary-container)')}>
                  Áp dụng loại ghế
                </button>
                <button disabled={selected.length === 0 || saving} onClick={() => applyMaintenance(true)} style={compactButton(selected.length > 0 && !saving, '#dc2626', '#fff')}>
                  Chuyển sang bảo trì
                </button>
                <button disabled={selected.length === 0 || saving} onClick={() => applyMaintenance(false)} style={compactButton(selected.length > 0 && !saving, 'rgba(34,197,94,0.12)', '#bbf7d0', 'rgba(34,197,94,0.35)')}>
                  Khôi phục hoạt động
                </button>
                <button disabled={selected.length === 0 || saving} onClick={() => setSelected([])} style={compactButton(selected.length > 0 && !saving, 'transparent', 'var(--color-text-muted)', 'var(--color-border)')}>
                  Bỏ chọn
                </button>
              </div>
            </>
          )}
        </div>

        {/* Control Panel */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16, minWidth: 0 }}>
          <div style={S.card}>
            <div style={{ fontSize: 14, fontWeight: 800, color: 'var(--color-text)', marginBottom: 12 }}>Thông tin phòng</div>
            {[
              { label: 'Phòng', value: selectedRoom?.name },
              { label: 'Loại phòng', value: selectedRoom?.screenType || selectedRoom?.roomType || '2D' },
              { label: 'Số hàng', value: selectedRoom?.totalRows || selectedRoom?.rowCount },
              { label: 'Số cột', value: selectedRoom?.totalColumns || selectedRoom?.columnCount },
              { label: 'Tổng ghế', value: seats.length },
            ].map(({ label, value }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: 'var(--color-text-muted)' }}>{label}</span>
                <strong style={{ color: 'var(--color-text)' }}>{value || '-'}</strong>
              </div>
            ))}
          </div>
          {/* Legend */}
          <div style={S.card}>
            <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-text)', marginBottom: 14 }}>Chú thích</div>
            {Object.entries(SEAT_TYPE_CONFIG).map(([k, v]) => (
              <div key={k} style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                <div style={{ width: 28, height: 22, borderRadius: 4, background: v.bg, border: `2px solid ${v.color}`, flexShrink: 0 }} />
                <div>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text)' }}>{v.label}</div>
                </div>
                <div style={{ marginLeft: 'auto', fontSize: 13, fontWeight: 700, color: 'var(--color-text)' }}>{countByType[k] || 0}</div>
              </div>
            ))}
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 10 }}>
              <div style={{ width: 28, height: 22, borderRadius: 4, background: 'transparent', border: '2px solid var(--color-accent)', flexShrink: 0 }} />
              <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-text)' }}>Đang chọn</div>
            </div>
          </div>

          {/* Stats */}
          <div style={S.card}>
            <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--color-text)', marginBottom: 12 }}>Thống kê phòng</div>
            {[
              { label: 'Tổng ghế', value: seats.length, color: 'var(--color-text)' },
              { label: 'Ghế thường', value: countByType['NORMAL'] || 0, color: '#3b82f6' },
              { label: 'Ghế VIP', value: countByType['VIP'] || 0, color: '#a855f7' },
              { label: 'Ghế đôi', value: countByType['COUPLE'] || 0, color: '#ec4899' },
              { label: 'Ghế hoạt động', value: activeSeats, color: '#22c55e' },
              { label: 'Ghế ngừng hoạt động', value: countByType['INACTIVE'] || 0, color: 'var(--color-text-muted)' },
            ].map(({ label, value, color }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, fontSize: 13 }}>
                <span style={{ color: 'var(--color-text-muted)' }}>{label}</span>
                <span style={{ fontWeight: 700, color }}>{value}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      )}
    </div>
  )
}

function compactButton(enabled, background, color, border = 'none') {
  return {
    background: enabled ? background : 'var(--surface-container-high)',
    color: enabled ? color : 'var(--color-text-muted)',
    border: enabled ? border : '1px solid var(--color-border)',
    borderRadius: 8,
    padding: '8px 10px',
    fontWeight: 800,
    fontSize: 12,
    cursor: enabled ? 'pointer' : 'not-allowed',
    whiteSpace: 'nowrap',
  }
}
