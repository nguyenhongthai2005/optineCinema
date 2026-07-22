import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import adminService from '../../services/admin.service'

const iso = date => date.toISOString().slice(0, 10)
const initialFilters = () => {
  const to = new Date(), from = new Date()
  from.setDate(from.getDate() - 29)
  return { from: iso(from), to: iso(to), movieId: '', paymentMethod: '', bookingStatus: '' }
}
const money = value => `${Number(value || 0).toLocaleString('vi-VN')} ₫`
const number = value => Number(value || 0).toLocaleString('vi-VN')
const time = value => value ? new Date(value).toLocaleString('vi-VN') : '—'
const differs = (left, right) => Math.abs(Number(left || 0) - Number(right || 0)) >= 1

export default function AdminDashboard() {
  const [filters, setFilters] = useState(initialFilters)
  const [data, setData] = useState(null)
  const [movies, setMovies] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const params = useMemo(() => Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '')), [filters])

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try { setData((await adminService.getAnalyticsOverview(params)).data) }
    catch (err) { setError(err.response?.data?.message || err.response?.data || 'Không tải được dữ liệu phân tích.') }
    finally { setLoading(false) }
  }, [params])
  useEffect(() => { load() }, [load])
  useEffect(() => {
    adminService.getMovies({}).then(res => {
      const value = res.data?.content || res.data || []
      setMovies(Array.isArray(value) ? value : [])
    }).catch(() => setMovies([]))
  }, [])

  const change = event => setFilters(current => ({ ...current, [event.target.name]: event.target.value }))
  const setRange = days => {
    const to = new Date(), from = new Date(); from.setDate(from.getDate() - days + 1)
    setFilters(current => ({ ...current, from: iso(from), to: iso(to) }))
  }
  const maxRevenue = Math.max(...(data?.timeline || []).map(item => Number(item.revenue || 0)), 1)
  const orderRevenue = data?.orderRevenue ?? data?.revenue
  const actualPaidRevenue = data?.actualPaidRevenue ?? data?.revenue
  const hasDemoDifference = differs(orderRevenue, actualPaidRevenue)
  const cards = [
    ['payments', 'Doanh thu đơn hàng', money(orderRevenue), '#16a34a'],
    ['receipt', 'Thực thu / demo', money(actualPaidRevenue), '#0f766e'],
    ...(hasDemoDifference ? [['price_change', 'Chênh lệch demo', money(Number(orderRevenue || 0) - Number(actualPaidRevenue || 0)), '#d97706']] : []),
    ['confirmation_number', 'Vé đã bán', number(data?.ticketsSold), '#0284c7'],
    ['receipt_long', 'Tổng booking', number(data?.totalBookings), '#7c3aed'],
    ['check_circle', 'Booking đã trả', number(data?.paidBookings), '#059669'],
    ['schedule', 'Đang chờ', number(data?.pendingBookings), '#d97706'],
    ['cancel', 'Đã hủy', number(data?.cancelledBookings), '#dc2626'],
    ['monitoring', 'Giá trị trung bình', money(data?.averageBookingValue), '#0f766e'],
    ['percent', 'Tỷ lệ chuyển đổi', `${Number(data?.conversionRate || 0).toLocaleString('vi-VN')}%`, '#be185d'],
  ]

  return <div className="admin-content">
    <div style={styles.header}>
      <div><h1 style={styles.title}>Phân tích doanh thu & booking</h1><p style={styles.subtitle}>Số liệu lấy từ booking, hóa đơn và giao dịch đã thanh toán.</p></div>
      <Link to="/admin/reports" style={styles.link}>Xem và xuất báo cáo</Link>
    </div>
    <div style={styles.filters}>
      <div style={{ display: 'flex', gap: 7 }}><button style={styles.input} onClick={() => setRange(1)}>Hôm nay</button><button style={styles.input} onClick={() => setRange(7)}>7 ngày</button><button style={styles.input} onClick={() => setRange(30)}>30 ngày</button></div>
      <Filter label="Từ ngày"><input style={styles.input} type="date" name="from" value={filters.from} onChange={change} /></Filter>
      <Filter label="Đến ngày"><input style={styles.input} type="date" name="to" value={filters.to} onChange={change} /></Filter>
      <Filter label="Phim"><select style={styles.input} name="movieId" value={filters.movieId} onChange={change}><option value="">Tất cả</option>{movies.map(movie => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></Filter>
      <Filter label="Thanh toán"><select style={styles.input} name="paymentMethod" value={filters.paymentMethod} onChange={change}><option value="">Tất cả</option><option>VNPAY</option><option>VIETQR</option><option value="CASH">Tiền mặt</option></select></Filter>
      <Filter label="Booking"><select style={styles.input} name="bookingStatus" value={filters.bookingStatus} onChange={change}><option value="">Tất cả</option><option value="CONFIRMED">Đã xác nhận</option><option value="PENDING_PAYMENT">Chờ thanh toán</option><option value="WAITING_CONFIRMATION">Chờ xác nhận</option><option value="CANCELLED">Đã hủy</option></select></Filter>
    </div>
    {error && <div style={styles.error}>{String(error)}</div>}
    {loading ? <section style={styles.panel}>Đang tải dữ liệu...</section> : <>
      {hasDemoDifference && <div style={styles.notice}>VietQR đang bật chế độ demo nên số tiền thực chuyển có thể nhỏ hơn giá trị đơn hàng. Doanh thu đơn hàng dùng giá trị hóa đơn/finalAmount.</div>}
      <div style={styles.cardGrid}>{cards.map(([icon, label, value, color]) => <article key={label} style={styles.card}><span className="material-symbols-outlined" style={{ ...styles.icon, color, background: `${color}18` }}>{icon}</span><div><div style={styles.value}>{value}</div><div style={styles.cardLabel}>{label}</div></div></article>)}</div>
      <div style={styles.columns}>
        <section style={styles.panel}><h2 style={styles.sectionTitle}>Doanh thu theo ngày</h2>{(data?.timeline || []).length === 0 ? <p>Chưa có dữ liệu.</p> : <div style={styles.chart}>{data.timeline.map(item => <div key={item.date} style={styles.barColumn} title={`${item.date}: Giá trị đơn ${money(item.revenue)}${differs(item.revenue, item.actualPaidRevenue) ? ` · Thực thu demo ${money(item.actualPaidRevenue)}` : ''}`}><div style={{ ...styles.bar, height: `${Math.max(3, Number(item.revenue || 0) / maxRevenue * 170)}px` }} /><span style={styles.barLabel}>{item.date.slice(5)}</span></div>)}</div>}</section>
        <section style={styles.panel}><h2 style={styles.sectionTitle}>Theo phương thức thanh toán</h2>{(data?.paymentMethods || []).length === 0 ? <p>Chưa có giao dịch.</p> : data.paymentMethods.map(item => <Rank key={item.paymentMethod} name={item.paymentMethod} detail={`${number(item.transactions)} giao dịch`} value={money(item.revenue)} subValue={differs(item.revenue, item.actualPaidRevenue) ? `Thực thu demo: ${money(item.actualPaidRevenue)}` : ''} />)}</section>
      </div>
      <div style={styles.columns}>
        <section style={styles.panel}><h2 style={styles.sectionTitle}>Top phim theo doanh thu</h2>{(data?.topMovies || []).length === 0 ? <p>Chưa có dữ liệu.</p> : data.topMovies.map((item, index) => <Rank key={item.movieId} name={`#${index + 1} ${item.movieTitle}`} detail={`${number(item.tickets)} vé`} value={money(item.revenue)} />)}</section>
        <section style={styles.panel}><h2 style={styles.sectionTitle}>Booking gần nhất</h2>{(data?.recentBookings || []).length === 0 ? <p>Chưa có booking.</p> : data.recentBookings.map(item => <div key={item.bookingId} style={styles.booking}><div><b>#{item.bookingId} · {item.movieTitle}</b><small>{item.customerName || 'Khách hàng'} · {time(item.createdAt)}</small></div><div style={{ textAlign: 'right' }}><b>{money(item.invoiceAmount)}</b>{differs(item.invoiceAmount, item.paidAmount) && <small>Thực thu demo: {money(item.paidAmount)}</small>}<small>{item.bookingStatus}</small></div></div>)}</section>
      </div>
    </>}
  </div>
}

const Filter = ({ label, children }) => <label style={styles.label}>{label}{children}</label>
const Rank = ({ name, detail, value, subValue }) => <div style={styles.rank}><strong>{name}</strong><span>{detail}</span><div style={{ textAlign: 'right' }}><b>{value}</b>{subValue && <small style={styles.subValue}>{subValue}</small>}</div></div>
const styles = {
  header: { display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'center', marginBottom: 20, flexWrap: 'wrap' },
  title: { margin: 0, color: 'var(--color-text)', fontSize: 27, fontWeight: 900 }, subtitle: { margin: '6px 0 0', color: 'var(--color-text-muted)' },
  link: { padding: '11px 15px', borderRadius: 8, background: '#ec6a06', color: '#fff', fontWeight: 800, textDecoration: 'none' },
  filters: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 10, padding: 16, marginBottom: 18, display: 'flex', gap: 12, alignItems: 'end', flexWrap: 'wrap' },
  label: { display: 'flex', flexDirection: 'column', gap: 6, fontSize: 12, fontWeight: 800, color: 'var(--color-text-muted)' },
  input: { minHeight: 40, border: '1px solid #cbd5e1', borderRadius: 7, padding: '7px 9px', background: '#fff', color: '#0f172a', cursor: 'pointer' },
  notice: { padding: 13, borderRadius: 8, background: '#fff7ed', border: '1px solid #fed7aa', color: '#9a3412', marginBottom: 16, fontWeight: 700 },
  cardGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 14, marginBottom: 18 },
  card: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 10, padding: 18, display: 'flex', alignItems: 'center', gap: 13, boxShadow: 'var(--shadow-card)' },
  icon: { width: 44, height: 44, borderRadius: 10, display: 'grid', placeItems: 'center' }, value: { color: 'var(--color-text)', fontSize: 22, fontWeight: 900 }, cardLabel: { color: 'var(--color-text-muted)', fontSize: 12, fontWeight: 700, marginTop: 3 },
  columns: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: 16, marginBottom: 16 },
  panel: { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 10, padding: 18, color: 'var(--color-text-muted)', boxShadow: 'var(--shadow-card)', minWidth: 0 },
  sectionTitle: { margin: '0 0 16px', color: 'var(--color-text)', fontSize: 16, fontWeight: 900 },
  chart: { height: 215, display: 'flex', alignItems: 'end', gap: 5, overflowX: 'auto', paddingTop: 12 }, barColumn: { minWidth: 22, flex: '1 0 22px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'end', height: '100%' }, bar: { width: '70%', minWidth: 10, maxWidth: 28, background: 'linear-gradient(180deg,#fb923c,#ea580c)', borderRadius: '5px 5px 0 0' }, barLabel: { fontSize: 9, marginTop: 6, transform: 'rotate(-35deg)', whiteSpace: 'nowrap' },
  rank: { display: 'grid', gridTemplateColumns: 'minmax(130px,1fr) auto auto', alignItems: 'center', gap: 10, padding: '11px 0', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text)' },
  subValue: { display: 'block', marginTop: 4, color: '#d97706', fontWeight: 800 },
  booking: { display: 'flex', justifyContent: 'space-between', gap: 12, padding: '10px 0', borderBottom: '1px solid var(--color-border)', color: 'var(--color-text)' },
  error: { padding: 13, borderRadius: 8, background: '#fee2e2', border: '1px solid #fecaca', color: '#991b1b', marginBottom: 16 },
}
