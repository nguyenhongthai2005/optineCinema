import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import adminService from '../../services/admin.service'

const today = new Date()
const isoDate = (date) => date.toISOString().split('T')[0]
const nextWeek = new Date(today.getTime() + 6 * 24 * 60 * 60 * 1000)

const initialForm = {
  fromDate: isoDate(today),
  toDate: isoDate(nextWeek),
  mode: 'MAX_REVENUE',
  openingTime: '08:00',
  closingTime: '23:30',
  cleaningMinutes: 15,
  overwriteExisting: false,
}

export default function AutoSchedule() {
  const [form, setForm] = useState(initialForm)
  const [plan, setPlan] = useState(null)
  const [loading, setLoading] = useState(false)
  const [applying, setApplying] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const update = (key) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const generate = async () => {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const payload = {
        ...form,
        cleaningMinutes: Number(form.cleaningMinutes || 15),
      }
      const res = await adminService.generateSchedulePlan(payload)
      setPlan(res.data)
      setMessage(`Đã tạo draft plan #${res.data.planId}`)
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không tạo được lịch tối ưu.')
    } finally {
      setLoading(false)
    }
  }

  const apply = async () => {
    if (!plan?.planId) return
    setApplying(true)
    setError('')
    setMessage('')
    try {
      const res = await adminService.applySchedulePlan(plan.planId)
      setPlan((prev) => ({ ...prev, status: 'APPLIED' }))
      setMessage(`Đã áp dụng ${res.data?.length || 0} suất chiếu. Bạn có thể xem trong Quản lý Giờ chiếu.`)
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không áp dụng được lịch chiếu.')
    } finally {
      setApplying(false)
    }
  }

  return (
    <div style={S.page}>
      <div style={{ marginBottom: 24 }}>
        <h1 style={S.title}>Tối ưu lịch chiếu</h1>
        <p style={S.subtitle}>Opticine tự động gợi ý lịch chiếu theo doanh thu kỳ vọng, nhu cầu phim và độ phủ phòng.</p>
      </div>

      <section style={S.card}>
        <div style={S.formGrid}>
          <Field label="Từ ngày"><input type="date" value={form.fromDate} onChange={update('fromDate')} style={S.input} /></Field>
          <Field label="Đến ngày"><input type="date" value={form.toDate} onChange={update('toDate')} style={S.input} /></Field>
          <Field label="Chế độ">
            <select value={form.mode} onChange={update('mode')} style={S.input}>
              <option value="MAX_REVENUE">MAX_REVENUE</option>
              <option value="BALANCED">BALANCED</option>
              <option value="MAX_UTILIZATION">MAX_UTILIZATION</option>
            </select>
          </Field>
          <Field label="Mở cửa"><input type="time" value={form.openingTime} onChange={update('openingTime')} style={S.input} /></Field>
          <Field label="Đóng cửa"><input type="time" value={form.closingTime} onChange={update('closingTime')} style={S.input} /></Field>
          <Field label="Dọn phòng (phút)"><input type="number" min="0" value={form.cleaningMinutes} onChange={update('cleaningMinutes')} style={S.input} /></Field>
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--color-text-muted)', fontWeight: 700, fontSize: 13, marginTop: 14 }}>
          <input type="checkbox" checked={form.overwriteExisting} onChange={update('overwriteExisting')} />
          Cho phép cân nhắc ghi đè lịch trống
        </label>
        <div style={{ marginTop: 18, display: 'flex', gap: 10, alignItems: 'center' }}>
          <button onClick={generate} disabled={loading} style={S.primaryButton}>
            {loading ? 'Đang tạo...' : 'Generate schedule'}
          </button>
          {plan && plan.status === 'DRAFT' && (
            <button onClick={apply} disabled={applying} style={S.greenButton}>
              {applying ? 'Đang áp dụng...' : 'Áp dụng lịch chiếu'}
            </button>
          )}
          {plan?.status === 'APPLIED' && (
            <button onClick={() => navigate('/admin/showtimes')} style={S.secondaryButton}>Xem suất chiếu</button>
          )}
        </div>
      </section>

      {message && <div style={S.success}>{message}</div>}
      {error && <div style={S.error}>{error}</div>}

      {plan && (
        <>
          <div style={S.statsGrid}>
            <Stat label="Estimated revenue" value={money(plan.estimatedRevenue)} />
            <Stat label="Expected tickets" value={plan.estimatedTickets || 0} />
            <Stat label="Room utilization" value={`${Number(plan.roomUtilization || 0).toFixed(2)}%`} />
            <Stat label="Status" value={plan.status} />
          </div>

          <section style={S.tableCard}>
            <div style={{ padding: '18px 20px', borderBottom: '1px solid var(--color-border)' }}>
              <h2 style={{ margin: 0, fontSize: 18, color: 'var(--color-text)' }}>Proposed showtimes</h2>
              <p style={{ margin: '4px 0 0', color: 'var(--color-text-muted)', fontSize: 13 }}>Plan #{plan.planId} • {plan.mode}</p>
            </div>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 1050 }}>
                <thead>
                  <tr>
                    {['Ngày', 'Giờ', 'Phim', 'Phòng', 'Vé dự kiến', 'Doanh thu', 'Score', 'Giải thích'].map((head) => (
                      <th key={head} style={S.th}>{head}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(plan.items || []).length === 0 ? (
                    <tr><td colSpan={8} style={{ ...S.td, textAlign: 'center', color: 'var(--color-text-muted)', padding: 32 }}>Không có đề xuất phù hợp.</td></tr>
                  ) : plan.items.map((item) => (
                    <tr key={item.id}>
                      <td style={S.td}>{dateText(item.startTime)}</td>
                      <td style={S.td}><strong>{timeText(item.startTime)}</strong> - {timeText(item.endTime)}</td>
                      <td style={S.td}>
                        <div style={{ fontWeight: 800, color: 'var(--color-text)' }}>{item.movieTitle}</div>
                        <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>Popularity {item.moviePopularity}</div>
                      </td>
                      <td style={S.td}>{item.roomName} <span style={{ color: 'var(--color-text-muted)' }}>{item.screenType || ''}</span></td>
                      <td style={S.td}>{item.expectedTickets}</td>
                      <td style={{ ...S.td, fontWeight: 800, color: '#15803d' }}>{money(item.expectedRevenue)}</td>
                      <td style={S.td}>{Number(item.score || 0).toFixed(2)}</td>
                      <td style={{ ...S.td, color: 'var(--color-text-muted)', whiteSpace: 'normal', minWidth: 260 }}>{item.reason}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  )
}

function Field({ label, children }) {
  return (
    <label>
      <div style={S.label}>{label}</div>
      {children}
    </label>
  )
}

function Stat({ label, value }) {
  return (
    <div style={S.statCard}>
      <div style={{ color: 'var(--color-text-muted)', fontSize: 12, fontWeight: 700, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ color: 'var(--color-text)', fontSize: 22, fontWeight: 900, marginTop: 6 }}>{value}</div>
    </div>
  )
}

const money = (value) => `${Number(value || 0).toLocaleString('vi-VN')} VND`
const dateText = (value) => value ? new Date(value).toLocaleDateString('vi-VN') : '-'
const timeText = (value) => value ? new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '-'

const S = {
  page: { padding: '24px', minHeight: '100vh', background: 'transparent', fontFamily: 'Inter, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%' },
  title: { fontSize: 26, fontWeight: 800, color: 'var(--color-text)', margin: 0 },
  subtitle: { fontSize: 14, color: 'var(--color-text-muted)', margin: '4px 0 0' },
  card: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 20, boxShadow: 'var(--shadow-card)' },
  formGrid: { display: 'grid', gridTemplateColumns: 'repeat(3, minmax(160px, 1fr))', gap: 14 },
  label: { color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 700, marginBottom: 6 },
  input: { width: '100%', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 12px', color: 'var(--color-text)', background: 'var(--surface-container-low)', boxSizing: 'border-box' },
  primaryButton: { background: 'var(--color-primary)', color: 'var(--on-secondary-container)', border: 'none', borderRadius: 8, padding: '10px 16px', fontWeight: 800, cursor: 'pointer' },
  greenButton: { background: '#16a34a', color: 'white', border: 'none', borderRadius: 8, padding: '10px 16px', fontWeight: 800, cursor: 'pointer' },
  secondaryButton: { background: 'var(--surface-container-high)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 16px', fontWeight: 800, cursor: 'pointer' },
  success: { marginTop: 16, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', borderRadius: 8, padding: 12 },
  error: { marginTop: 16, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', borderRadius: 8, padding: 12 },
  statsGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14, margin: '20px 0' },
  statCard: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: 'var(--shadow-card)' },
  tableCard: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, boxShadow: 'var(--shadow-card)', overflow: 'hidden' },
  th: { padding: '12px 14px', textAlign: 'left', fontSize: 11, fontWeight: 800, color: 'var(--color-text-muted)', textTransform: 'uppercase', background: 'var(--surface-container-high)', borderBottom: '1px solid var(--color-border)' },
  td: { padding: '13px 14px', fontSize: 14, color: 'var(--color-text)', borderBottom: '1px solid var(--color-border)', verticalAlign: 'top', whiteSpace: 'nowrap' },
}
