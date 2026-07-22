import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import axios from 'axios'
import ETicket from '../components/ETicket'
import { API_BASE_URL, authHeader } from '../services/api'

export default function PaymentResult() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const status = searchParams.get('status')
  const bookingId = searchParams.get('bookingId')
  const autoConfirmed = searchParams.get('autoConfirmed') === 'true'
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(true)
  const [emailMessage, setEmailMessage] = useState('')
  const [emailError, setEmailError] = useState('')

  useEffect(() => {
    if (status === 'success' && bookingId) {
      axios.get(`${API_BASE_URL}/bookings/${bookingId}/tickets`, {
        headers: authHeader()
      }).then(res => {
        setTickets(res.data)
      }).catch(() => {
        // tickets not critical
      }).finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [status, bookingId])

  const resendEmail = async () => {
    setEmailMessage('')
    setEmailError('')
    try {
      await axios.post(`${API_BASE_URL}/bookings/${bookingId}/send-ticket-email`, {}, { headers: authHeader() })
      setEmailMessage('Vé đã được gửi lại về email của bạn.')
    } catch (err) {
      setEmailError(err.response?.data || 'Không gửi lại được email vé.')
    }
  }

  if (status === 'success') {
    return (
      <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem' }}>
        <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: '1.5rem', padding: '3rem', maxWidth: '500px', width: '100%', textAlign: 'center', border: '1px solid rgba(255,255,255,0.1)' }}>
          <div style={{ fontSize: '5rem', marginBottom: '1rem' }}>🎉</div>
          <h1 style={{ color: '#22c55e', fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>Thanh toán thành công</h1>
          <p style={{ color: '#94a3b8', marginBottom: '2rem' }}>
            {autoConfirmed
              ? `Đơn hàng #${bookingId} đã được xác nhận tự động.`
              : `Đơn hàng #${bookingId} đã được xác nhận.`}
          </p>
          <p style={{ color: '#86efac', marginBottom: '1rem' }}>Vé đã được gửi qua email của bạn.</p>
          {emailMessage && <p style={{ color: '#86efac' }}>{emailMessage}</p>}
          {emailError && <p style={{ color: '#fca5a5' }}>{emailError}</p>}

          {loading ? (
            <p style={{ color: '#64748b' }}>Đang tải vé...</p>
          ) : tickets.length > 0 ? (
            <div>
              <h3 style={{ color: '#e2e8f0', marginBottom: '1rem' }}>Vé của bạn</h3>
              {tickets.map((ticket, idx) => (
                <ETicket key={idx} ticket={ticket} />
              ))}
            </div>
          ) : (
            <div style={{ background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '1rem', padding: '1.5rem', marginBottom: '1.5rem' }}>
              <p style={{ color: '#86efac', margin: 0 }}>Đơn hàng #{bookingId} đã được xác nhận thành công.</p>
            </div>
          )}

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.5rem' }}>
            <button
              onClick={() => navigate(`/booking/success?bookingId=${bookingId}&status=success`)}
              style={{ flex: 1, background: 'rgba(34,197,94,0.14)', color: '#86efac', border: '1px solid rgba(34,197,94,0.3)', borderRadius: '0.75rem', padding: '0.875rem 1rem', fontSize: '0.9rem', fontWeight: 600, cursor: 'pointer' }}
            >
              Xem e-ticket
            </button>
            <button
              onClick={() => navigate('/my-bookings')}
              style={{ flex: 1, background: 'rgba(255,255,255,0.1)', color: '#e2e8f0', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '0.75rem', padding: '0.875rem 1rem', fontSize: '0.9rem', fontWeight: 600, cursor: 'pointer' }}
            >
              Vé của tôi
            </button>
            <button
              onClick={resendEmail}
              style={{ flex: 1, background: 'rgba(255,255,255,0.1)', color: '#e2e8f0', border: '1px solid rgba(255,255,255,0.15)', borderRadius: '0.75rem', padding: '0.875rem 1rem', fontSize: '0.9rem', fontWeight: 600, cursor: 'pointer' }}
            >
              Gửi lại email
            </button>
            <button
              onClick={() => navigate('/')}
              style={{ flex: 1, background: 'linear-gradient(90deg, #6366f1, #8b5cf6)', color: 'white', border: 'none', borderRadius: '0.75rem', padding: '0.875rem 1rem', fontSize: '0.9rem', fontWeight: 600, cursor: 'pointer' }}
            >
              Về trang chủ
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem' }}>
      <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: '1.5rem', padding: '3rem', maxWidth: '500px', width: '100%', textAlign: 'center', border: '1px solid rgba(255,255,255,0.1)' }}>
        <div style={{ fontSize: '5rem', marginBottom: '1rem' }}>❌</div>
        <h1 style={{ color: '#ef4444', fontSize: '2rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>Thanh toán thất bại</h1>
        <p style={{ color: '#94a3b8', marginBottom: '2rem' }}>Giao dịch bị hủy hoặc có lỗi xảy ra. Ghế đã được mở khóa. Vui lòng thử lại hoặc chọn phương thức thanh toán khác.</p>
        <button
          onClick={() => navigate('/showtimes')}
          style={{ background: 'linear-gradient(90deg, #6366f1, #8b5cf6)', color: 'white', border: 'none', borderRadius: '0.75rem', padding: '0.875rem 2rem', fontSize: '1rem', fontWeight: 'bold', cursor: 'pointer', width: '100%' }}
        >
          Quay lại lịch chiếu
        </button>
      </div>
    </div>
  )
}
