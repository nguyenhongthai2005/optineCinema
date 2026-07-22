import { useEffect, useState } from 'react'
import adminService from '../../services/admin.service'

export default function PendingVietQrPayments() {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  useEffect(() => {
    fetchPending()
  }, [])

  const fetchPending = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await adminService.getPendingPayments()
      setPayments(res.data || [])
    } catch (err) {
      setError(err.response?.data || 'Khong tai duoc danh sach thanh toan')
    } finally {
      setLoading(false)
    }
  }

  const act = async (bookingId, action) => {
    setBusyId(bookingId)
    setMessage('')
    setError('')
    try {
      const res = action === 'confirm'
        ? await adminService.confirmVietQrPayment(bookingId)
        : await adminService.rejectVietQrPayment(bookingId)
      setMessage(res.data?.message || 'Da cap nhat thanh toan')
      await fetchPending()
    } catch (err) {
      setError(err.response?.data || 'Khong cap nhat duoc thanh toan')
    } finally {
      setBusyId(null)
    }
  }

  const formatTime = (value) => value ? new Date(value).toLocaleString('vi-VN') : 'N/A'
  const formatMoney = (value) => Number(value || 0).toLocaleString('vi-VN') + ' VND'

  return (
    <div style={{ padding: 28 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
        <div>
          <h1 style={{ margin: 0, color: '#bfc8dc', fontSize: 28, fontWeight: 800 }}>Xác nhận thanh toán VietQR</h1>
          <p style={{ margin: '6px 0 0', color: '#64748b' }}>Đối chiếu chuyển khoản trong app ngân hàng rồi xác nhận thủ công.</p>
        </div>
        <button onClick={fetchPending} style={secondaryButton}>Làm mới</button>
      </div>

      {message && <div style={successBox}>{message}</div>}
      {error && <div style={errorBox}>{error}</div>}

      <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: 24, color: '#64748b' }}>Đang tải...</div>
        ) : payments.length === 0 ? (
          <div style={{ padding: 24, color: '#64748b' }}>Không có thanh toán VietQR nào đang chờ duyệt.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 1100 }}>
              <thead>
                <tr style={{ background: '#f8fafc', color: '#475569', textAlign: 'left' }}>
                  {['Booking', 'Khách hàng', 'Email', 'Phim', 'Suất chiếu', 'Phòng', 'Ghế', 'Số tiền', 'Nội dung CK', 'Trạng thái', 'Tạo lúc', 'Thao tác'].map(head => (
                    <th key={head} style={th}>{head}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {payments.map(payment => (
                  <tr key={payment.bookingId} style={{ borderTop: '1px solid #e2e8f0' }}>
                    <td style={td}>#{payment.bookingId}</td>
                    <td style={td}>{payment.customerName}</td>
                    <td style={td}>{payment.customerEmail}</td>
                    <td style={td}>{payment.movieTitle}</td>
                    <td style={td}>{formatTime(payment.showtimeStartTime)}</td>
                    <td style={td}>{payment.roomName}</td>
                    <td style={td}>{(payment.seats || []).join(', ')}</td>
                    <td style={{ ...td, fontWeight: 800, color: '#15803d' }}>{formatMoney(payment.amount)}</td>
                    <td style={{ ...td, fontFamily: 'monospace', fontWeight: 800 }}>{payment.transferContent}</td>
                    <td style={td}>{payment.paymentStatus}</td>
                    <td style={td}>{formatTime(payment.createdAt)}</td>
                    <td style={td}>
                      <div style={{ display: 'flex', gap: 8 }}>
                        <button
                          onClick={() => act(payment.bookingId, 'confirm')}
                          disabled={busyId === payment.bookingId}
                          style={confirmButton}
                        >
                          Confirm
                        </button>
                        <button
                          onClick={() => act(payment.bookingId, 'reject')}
                          disabled={busyId === payment.bookingId}
                          style={rejectButton}
                        >
                          Reject
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

const th = {
  padding: '12px 14px',
  fontSize: 12,
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
  whiteSpace: 'nowrap',
}

const td = {
  padding: '13px 14px',
  color: '#334155',
  fontSize: 14,
  verticalAlign: 'top',
  whiteSpace: 'nowrap',
}

const secondaryButton = {
  background: '#f8fafc',
  color: '#0f172a',
  border: '1px solid #cbd5e1',
  borderRadius: 8,
  padding: '0.65rem 0.9rem',
  fontWeight: 700,
  cursor: 'pointer',
}

const confirmButton = {
  background: '#16a34a',
  color: '#fff',
  border: 'none',
  borderRadius: 8,
  padding: '0.5rem 0.75rem',
  fontWeight: 800,
  cursor: 'pointer',
}

const rejectButton = {
  background: '#dc2626',
  color: '#fff',
  border: 'none',
  borderRadius: 8,
  padding: '0.5rem 0.75rem',
  fontWeight: 800,
  cursor: 'pointer',
}

const successBox = {
  background: '#dcfce7',
  border: '1px solid #86efac',
  color: '#166534',
  borderRadius: 8,
  padding: '0.85rem 1rem',
  marginBottom: 14,
}

const errorBox = {
  background: '#fee2e2',
  border: '1px solid #fecaca',
  color: '#991b1b',
  borderRadius: 8,
  padding: '0.85rem 1rem',
  marginBottom: 14,
}
