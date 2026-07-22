import { useCallback, useEffect, useMemo, useState } from 'react'
import adminService from '../../services/admin.service'

const iso = date => date.toISOString().slice(0, 10)
const defaults = () => {
  const to = new Date(), from = new Date(); from.setDate(from.getDate() - 29)
  return { from: iso(from), to: iso(to), paymentMethod: '', bookingStatus: '' }
}
const money = value => `${Number(value || 0).toLocaleString('vi-VN')} ₫`
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '—'
const differs = (left, right) => Math.abs(Number(left || 0) - Number(right || 0)) >= 1

export default function BookingRevenueReport() {
  const [filters, setFilters] = useState(defaults)
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [exporting, setExporting] = useState('')
  const params = useMemo(() => Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '')), [filters])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setRows((await adminService.getBookingReport(params)).data || []) }
    catch (err) { setError(err.response?.data?.message || err.response?.data || 'Không tải được báo cáo.') }
    finally { setLoading(false) }
  }, [params])
  useEffect(() => { load() }, [load])
  const change = event => setFilters(current => ({ ...current, [event.target.name]: event.target.value }))
  const download = async type => {
    setExporting(type); setError('')
    try {
      const response = type === 'bookings' ? await adminService.exportBookingReport(params) : await adminService.exportRevenueReport(params)
      const url = URL.createObjectURL(new Blob([response.data], { type: 'text/csv;charset=utf-8' }))
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = `opticine-${type}-${filters.from}_${filters.to}.csv`
      document.body.appendChild(anchor); anchor.click(); anchor.remove(); URL.revokeObjectURL(url)
    } catch (err) { setError(err.response?.data?.message || 'Không xuất được báo cáo.') }
    finally { setExporting('') }
  }
  const invoiceTotal = rows.reduce((sum, row) => sum + Number(row.invoiceAmount || 0), 0)
  const paidTotal = rows.reduce((sum, row) => sum + Number(row.paidAmount || 0), 0)
  const hasDemoDifference = differs(invoiceTotal, paidTotal)

  return <div className="admin-content">
    <div style={{ marginBottom: 20 }}><h1 style={styles.title}>Báo cáo booking & doanh thu</h1><p style={styles.subtitle}>Lọc trên màn hình và xuất đúng tập dữ liệu đó ra CSV.</p></div>
    <div style={styles.filters}>
      <Filter label="Từ ngày"><input style={styles.input} type="date" name="from" value={filters.from} onChange={change} /></Filter>
      <Filter label="Đến ngày"><input style={styles.input} type="date" name="to" value={filters.to} onChange={change} /></Filter>
      <Filter label="Thanh toán"><select style={styles.input} name="paymentMethod" value={filters.paymentMethod} onChange={change}><option value="">Tất cả</option><option>VNPAY</option><option>VIETQR</option><option value="CASH">Tiền mặt</option></select></Filter>
      <Filter label="Trạng thái"><select style={styles.input} name="bookingStatus" value={filters.bookingStatus} onChange={change}><option value="">Tất cả</option><option value="CONFIRMED">Đã xác nhận</option><option value="PENDING_PAYMENT">Chờ thanh toán</option><option value="WAITING_CONFIRMATION">Chờ xác nhận</option><option value="CANCELLED">Đã hủy</option></select></Filter>
      <button style={styles.button} disabled={!!exporting} onClick={() => download('bookings')}>{exporting === 'bookings' ? 'Đang xuất...' : 'Xuất booking'}</button>
      <button style={{ ...styles.button, background: '#15803d' }} disabled={!!exporting} onClick={() => download('revenue')}>{exporting === 'revenue' ? 'Đang xuất...' : 'Xuất doanh thu'}</button>
    </div>
    {error && <div style={styles.error}>{String(error)}</div>}
    {hasDemoDifference && <div style={styles.notice}>VietQR đang bật chế độ demo nên số tiền thực chuyển có thể nhỏ hơn giá trị đơn hàng. Doanh thu đơn hàng dùng giá trị hóa đơn/finalAmount.</div>}
    <div style={styles.summary}>
      <Summary label="Số booking" value={rows.length.toLocaleString('vi-VN')} />
      <Summary label="Tổng giá trị đơn hàng" value={money(invoiceTotal)} />
      <Summary label="Thực thu / demo" value={money(paidTotal)} />
    </div>
    <div style={styles.tableWrap}>{loading ? <div style={{ padding: 22 }}>Đang tải...</div> : rows.length === 0 ? <div style={{ padding: 22 }}>Không có booking trong khoảng đã chọn.</div> :
      <table style={styles.table}><thead><tr>{['Booking','Ngày tạo','Khách hàng','Phim / Suất chiếu','Phòng','Ghế / Combo','Hóa đơn','Thực thu','Thanh toán','Trạng thái'].map(head => <th key={head} style={styles.th}>{head}</th>)}</tr></thead>
        <tbody>{rows.map(row => <tr key={row.bookingId}>
          <td style={styles.td}><b>#{row.bookingId}</b></td><td style={styles.td}>{dateTime(row.createdAt)}</td>
          <td style={styles.td}><Line main={row.customerName || '—'} sub={row.customerEmail || row.customerPhone} /></td>
          <td style={styles.td}><Line main={row.movieTitle} sub={dateTime(row.showtimeStart)} /></td><td style={styles.td}>{row.roomName}</td>
          <td style={styles.td}><Line main={row.seats || '—'} sub={row.combos} /></td><td style={styles.td}>{money(row.invoiceAmount)}</td>
          <td style={{ ...styles.td, color: '#15803d', fontWeight: 900 }}><Line main={money(row.paidAmount)} sub={differs(row.invoiceAmount, row.paidAmount) ? 'Demo / số tiền test' : ''} /></td>
          <td style={styles.td}><Line main={row.paymentMethod || '—'} sub={row.paymentStatus} /></td><td style={styles.td}><span style={styles.badge}>{row.bookingStatus}</span></td>
        </tr>)}</tbody></table>}
    </div>
  </div>
}

const Filter = ({ label, children }) => <label style={styles.label}>{label}{children}</label>
const Summary = ({ label, value }) => <div style={styles.summaryCard}><small>{label}</small><strong style={{ fontSize: 21 }}>{value}</strong></div>
const Line = ({ main, sub }) => <div><b>{main}</b>{sub && <small style={{ display: 'block', marginTop: 4, color: '#64748b' }}>{sub}</small>}</div>
const styles = {
  title: { margin: 0, color: 'var(--color-text)', fontSize: 27, fontWeight: 900 }, subtitle: { margin: '6px 0 0', color: 'var(--color-text-muted)' },
  filters: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 10, padding: 16, marginBottom: 16, display: 'flex', gap: 12, alignItems: 'end', flexWrap: 'wrap' },
  label: { display: 'flex', flexDirection: 'column', gap: 6, fontSize: 12, fontWeight: 800, color: 'var(--color-text-muted)' },
  input: { minHeight: 40, border: '1px solid #cbd5e1', borderRadius: 7, padding: '7px 9px', background: '#fff', color: '#0f172a' },
  button: { minHeight: 40, padding: '0 14px', border: 0, borderRadius: 7, color: '#fff', background: '#ea580c', fontWeight: 800, cursor: 'pointer' },
  notice: { padding: 13, borderRadius: 8, background: '#fff7ed', border: '1px solid #fed7aa', color: '#9a3412', marginBottom: 16, fontWeight: 700 },
  summary: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit,minmax(180px,1fr))', gap: 12, marginBottom: 16 },
  summaryCard: { background: 'var(--color-surface)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 9, padding: 16, display: 'flex', flexDirection: 'column', gap: 6 },
  tableWrap: { background: '#fff', color: '#334155', border: '1px solid #e2e8f0', borderRadius: 10, overflowX: 'auto' }, table: { width: '100%', borderCollapse: 'collapse', minWidth: 1280 },
  th: { padding: '12px 13px', background: '#f8fafc', color: '#475569', fontSize: 11, textAlign: 'left', textTransform: 'uppercase', whiteSpace: 'nowrap' },
  td: { padding: '12px 13px', borderTop: '1px solid #e2e8f0', verticalAlign: 'top', whiteSpace: 'nowrap', fontSize: 13 }, badge: { padding: '4px 7px', borderRadius: 999, background: '#e2e8f0', fontSize: 11, fontWeight: 800 },
  error: { padding: 13, borderRadius: 8, background: '#fee2e2', border: '1px solid #fecaca', color: '#991b1b', marginBottom: 16 },
}
