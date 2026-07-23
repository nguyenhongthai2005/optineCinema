import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import ETicket from '../components/ETicket'
import { API_BASE_URL, authHeader } from '../services/api'

export default function MyBookings() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedBooking, setSelectedBooking] = useState(null)
  const [tickets, setTickets] = useState([])
  const [ticketLoading, setTicketLoading] = useState(false)
  const [emailMessage, setEmailMessage] = useState('')
  const [retryLoading, setRetryLoading] = useState(null)
  const [, setTick] = useState(0)
  const navigate = useNavigate()

  // Countdown timer - refresh mỗi giây
  useEffect(() => {
    const hasExpiring = bookings.some(b => (b.paymentStatus === 'FAILED' || b.paymentStatus === 'PENDING') && b.expiredAt)
    if (!hasExpiring) return
    const interval = setInterval(() => setTick(t => t + 1), 1000)
    return () => clearInterval(interval)
  }, [bookings])

  useEffect(() => {
    fetchBookings()
  }, [])

  const fetchBookings = async () => {
    try {
      if (!authHeader().Authorization) {
        navigate('/login', { state: { from: { pathname: '/my-bookings' } } })
        return
      }
      const res = await axios.get(`${API_BASE_URL}/bookings/my-bookings`, {
        headers: authHeader()
      })
      setBookings(res.data)
    } catch (err) {
      setError('Không thể tải lịch sử đặt vé')
    } finally {
      setLoading(false)
    }
  }

  const viewTickets = async (bookingId) => {
    if (selectedBooking === bookingId) {
      setSelectedBooking(null)
      setTickets([])
      return
    }
    setSelectedBooking(bookingId)
    setTicketLoading(true)
    try {
      const res = await axios.get(`${API_BASE_URL}/bookings/${bookingId}/tickets`, {
        headers: authHeader()
      })
      setTickets(res.data)
    } catch (err) {
      setTickets([])
      if (err.response?.status === 403) {
        setError('Bạn không có quyền xem vé này.')
      }
    } finally {
      setTicketLoading(false)
    }
  }

  const resendEmail = async (bookingId) => {
    setEmailMessage('')
    try {
      await axios.post(`${API_BASE_URL}/bookings/${bookingId}/send-ticket-email`, {}, {
        headers: authHeader()
      })
      setEmailMessage(`Đã gửi lại email vé cho đơn hàng #${bookingId}`)
    } catch (err) {
      setEmailMessage(err.response?.data || 'Không gửi lại được email vé')
    }
  }

  const cancelBooking = async (bookingId) => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) return;
    try {
      await axios.put(`${API_BASE_URL}/bookings/${bookingId}/cancel`, {}, {
        headers: authHeader()
      })
      alert('Đã hủy đơn hàng thành công!')
      fetchBookings()
    } catch (err) {
      alert(err.response?.data?.message || err.response?.data || 'Không thể hủy đơn hàng.')
    }
  }

  const retryPayment = async (bookingId) => {
    setRetryLoading(bookingId)
    try {
      const res = await axios.put(`${API_BASE_URL}/bookings/${bookingId}/retry-payment`, {}, {
        headers: authHeader()
      })
      const { paymentMethod } = res.data
      if (paymentMethod === 'VIETQR') {
        navigate(`/payment/vietqr?bookingId=${bookingId}`)
      } else {
        // VNPAY - tạo URL thanh toán mới
        const vnpayRes = await axios.get(`${API_BASE_URL}/payment/vnpay/create-url?bookingId=${bookingId}`, {
          headers: authHeader()
        })
        window.location.href = vnpayRes.data.paymentUrl
      }
    } catch (err) {
      alert(err.response?.data?.message || err.response?.data || 'Không thể thanh toán lại.')
      fetchBookings()
    } finally {
      setRetryLoading(null)
    }
  }

  const getCountdown = (expiredAt) => {
    if (!expiredAt) return null
    const diff = new Date(expiredAt).getTime() - Date.now()
    if (diff <= 0) return null
    const mins = Math.floor(diff / 60000)
    const secs = Math.floor((diff % 60000) / 1000)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const formatTime = (dateString) => {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return date.toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit',
      day: '2-digit', month: '2-digit', year: 'numeric'
    })
  }

  const formatPrice = (price) => {
    if (!price) return '0'
    return Number(price).toLocaleString('vi-VN')
  }

  const getStatusBadge = (status) => {
    const styles = {
      CONFIRMED: { bg: 'rgba(34,197,94,0.15)', color: '#22c55e', text: 'Đã thanh toán' },
      PENDING: { bg: 'rgba(234,179,8,0.15)', color: '#eab308', text: 'Chờ thanh toán' },
      PENDING_PAYMENT: { bg: 'rgba(234,179,8,0.15)', color: '#eab308', text: 'Chờ thanh toán' },
      WAITING_CONFIRMATION: { bg: 'rgba(59,130,246,0.15)', color: '#60a5fa', text: 'Chờ xác nhận' },
      CANCELLED: { bg: 'rgba(239,68,68,0.15)', color: '#ef4444', text: 'Đã hủy' },
      FAILED: { bg: 'rgba(239,68,68,0.15)', color: '#f97316', text: 'Thanh toán thất bại' },
    }
    const s = styles[status] || { bg: 'rgba(148,163,184,0.15)', color: '#94a3b8', text: status }
    return (
      <span style={{
        background: s.bg, color: s.color,
        padding: '0.25rem 0.75rem', borderRadius: '999px',
        fontSize: '0.75rem', fontWeight: 600,
      }}>
        {s.text}
      </span>
    )
  }

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
      padding: '2rem',
    }}>
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '2rem' }}>
          <div>
            <h1 style={{ color: '#f8fafc', fontSize: '1.75rem', fontWeight: 700, margin: 0 }}>
              Lịch sử đặt vé
            </h1>
            <p style={{ color: '#64748b', margin: '0.25rem 0 0', fontSize: '0.9rem' }}>
              Xem lại các đơn đặt vé của bạn
            </p>
          </div>
          <button
            onClick={() => navigate('/')}
            style={{
              background: 'rgba(255,255,255,0.08)',
              color: '#e2e8f0',
              border: '1px solid rgba(255,255,255,0.12)',
              borderRadius: '0.75rem',
              padding: '0.625rem 1.25rem',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Trang chủ
          </button>
        </div>

        {/* Content */}
        {loading ? (
          <div style={{ textAlign: 'center', padding: '4rem 0' }}>
            <div style={{
              width: '40px', height: '40px',
              border: '3px solid rgba(255,255,255,0.1)',
              borderTop: '3px solid #6366f1',
              borderRadius: '50%',
              animation: 'spin 1s linear infinite',
              margin: '0 auto 1rem',
            }} />
            <p style={{ color: '#64748b' }}>Đang tải...</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        ) : error ? (
          <div style={{
            background: 'rgba(239,68,68,0.1)',
            border: '1px solid rgba(239,68,68,0.3)',
            borderRadius: '1rem',
            padding: '2rem',
            textAlign: 'center',
          }}>
            <p style={{ color: '#ef4444', margin: 0 }}>{error}</p>
          </div>
        ) : bookings.length === 0 ? (
          <div style={{
            background: 'rgba(255,255,255,0.05)',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: '1rem',
            padding: '4rem 2rem',
            textAlign: 'center',
          }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🎬</div>
            <p style={{ color: '#94a3b8', fontSize: '1.1rem', margin: 0 }}>Bạn chưa có đơn đặt vé nào</p>
            <button
              onClick={() => navigate('/showtimes')}
              style={{
                marginTop: '1.5rem',
                background: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
                color: '#fff', border: 'none', borderRadius: '0.75rem',
                padding: '0.75rem 2rem', fontSize: '0.9rem', fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Đặt vé ngay
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {emailMessage && (
              <div style={{ background: 'rgba(99,102,241,0.12)', color: '#c7d2fe', border: '1px solid rgba(99,102,241,0.3)', borderRadius: '0.75rem', padding: '0.85rem 1rem' }}>
                {emailMessage}
              </div>
            )}
            {bookings.map((booking) => (
              <div key={booking.id}>
                {/* Booking card */}
                <div
                  onClick={() => booking.status === 'CONFIRMED' && viewTickets(booking.id)}
                  style={{
                    background: 'rgba(255,255,255,0.05)',
                    border: selectedBooking === booking.id
                      ? '1px solid rgba(99,102,241,0.5)'
                      : '1px solid rgba(255,255,255,0.08)',
                    borderRadius: '1rem',
                    padding: '1.25rem 1.5rem',
                    cursor: booking.status === 'CONFIRMED' ? 'pointer' : 'default',
                    transition: 'all 0.2s ease',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
                        <span style={{ color: '#e2e8f0', fontWeight: 700, fontSize: '1rem' }}>
                          #{booking.id}
                        </span>
                        {getStatusBadge(booking.paymentStatus === 'FAILED' ? 'FAILED' : booking.status)}
                        {booking.paymentMethod && (
                          <span style={{ color: '#94a3b8', fontSize: '0.75rem', fontWeight: 700 }}>
                            {booking.paymentMethod}
                          </span>
                        )}
                      </div>

                      {booking.status === 'WAITING_CONFIRMATION' && (
                        <div style={{ color: '#bfdbfe', fontSize: '0.85rem', marginBottom: '0.75rem' }}>
                          Đang chờ xác nhận chuyển khoản.
                          {booking.paymentReference ? ` Nội dung: ${booking.paymentReference}` : ''}
                        </div>
                      )}

                      {booking.comboItems?.length > 0 && (
                        <div style={{ marginTop: '0.75rem', color: '#cbd5e1', fontSize: '0.82rem' }}>
                          <span style={{ color: '#64748b', fontWeight: 700 }}>Combo: </span>
                          {booking.comboItems.map((item) => `${item.comboName} x${item.quantity}`).join(', ')}
                        </div>
                      )}

                      {booking.paymentMethod === 'VIETQR' && Number(booking.vietQrPayableAmount || 0) > 0 && (
                        <div style={{ marginTop: '0.75rem', color: '#bfdbfe', fontSize: '0.82rem' }}>
                          Số tiền đã chuyển khoản demo: <strong>{formatPrice(booking.vietQrPaidAmount || booking.vietQrPayableAmount)} VND</strong>
                        </div>
                      )}

                      <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginTop: '0.75rem' }}>
                        <div>
                          <p style={{ margin: 0, color: '#64748b', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Suất chiếu</p>
                          <p style={{ margin: '0.15rem 0 0', color: '#cbd5e1', fontSize: '0.85rem' }}>{formatTime(booking.startTime)}</p>
                        </div>
                        <div>
                          <p style={{ margin: 0, color: '#64748b', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Số ghế</p>
                          <p style={{ margin: '0.15rem 0 0', color: '#cbd5e1', fontSize: '0.85rem' }}>{booking.seatCount} ghế</p>
                        </div>
                        <div>
                          <p style={{ margin: 0, color: '#64748b', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Tổng tiền</p>
                          <p style={{ margin: '0.15rem 0 0', color: '#22c55e', fontSize: '0.85rem', fontWeight: 600 }}>{formatPrice(booking.totalAmount)} VND</p>
                          {booking.promotionCode && (
                            <p style={{ margin: '0.1rem 0 0', color: '#cbd5e1', fontSize: '0.72rem' }}>Voucher: {booking.promotionCode}</p>
                          )}
                          {Number(booking.discountAmount || 0) > 0 && (
                            <p style={{ margin: '0.1rem 0 0', color: '#22c55e', fontSize: '0.72rem' }}>Tổng giảm giá: -{formatPrice(booking.discountAmount)} VND</p>
                          )}
                          {Number(booking.voucherDiscountAmount || 0) > 0 && (
                            <p style={{ margin: '0.1rem 0 0', color: '#22c55e', fontSize: '0.72rem' }}>Voucher: -{formatPrice(booking.voucherDiscountAmount)} VND</p>
                          )}
                          {Number(booking.membershipDiscountAmount || 0) > 0 && (
                            <p style={{ margin: '0.1rem 0 0', color: '#22c55e', fontSize: '0.72rem' }}>
                              Thành viên {booking.membershipTierName}: -{formatPrice(booking.membershipDiscountAmount)} VND
                            </p>
                          )}
                          {Number(booking.comboTotal || 0) > 0 && (
                            <p style={{ margin: '0.1rem 0 0', color: '#94a3b8', fontSize: '0.72rem' }}>Combo: {formatPrice(booking.comboTotal)} VND</p>
                          )}
                          {booking.paymentMethod === 'VIETQR' && Number(booking.vietQrPayableAmount || 0) > 0 && (
                            <p style={{ margin: '0.1rem 0 0', color: '#94a3b8', fontSize: '0.72rem' }}>Demo đã chuyển: {formatPrice(booking.vietQrPaidAmount || booking.vietQrPayableAmount)} VND</p>
                          )}
                        </div>
                      </div>
                    </div>

                    {booking.status === 'CONFIRMED' && (
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <button
                          onClick={(event) => {
                            event.stopPropagation()
                            resendEmail(booking.id)
                          }}
                          style={{ background: 'rgba(255,255,255,0.08)', color: '#cbd5e1', border: '1px solid rgba(255,255,255,0.12)', borderRadius: '0.5rem', padding: '0.45rem 0.65rem', fontSize: '0.72rem', cursor: 'pointer' }}
                        >
                          Gửi email
                        </button>
                        <div style={{
                          color: '#6366f1',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.25rem',
                          whiteSpace: 'nowrap',
                        }}>
                          {selectedBooking === booking.id ? 'Ẩn vé' : 'Xem vé'}
                          <span style={{
                            display: 'inline-block',
                            transform: selectedBooking === booking.id ? 'rotate(180deg)' : 'rotate(0deg)',
                            transition: 'transform 0.2s',
                          }}>▼</span>
                        </div>
                      </div>
                    )}

                    {(booking.status === 'PENDING_PAYMENT' || booking.status === 'PENDING' || booking.paymentStatus === 'FAILED') && (
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                        {(booking.paymentStatus === 'FAILED' || booking.paymentStatus === 'PENDING') && getCountdown(booking.expiredAt) && (
                          <button
                            disabled={retryLoading === booking.id}
                            onClick={(event) => {
                              event.stopPropagation()
                              retryPayment(booking.id)
                            }}
                            style={{
                              background: 'rgba(59,130,246,0.15)', color: '#60a5fa',
                              border: '1px solid rgba(59,130,246,0.3)', borderRadius: '0.5rem',
                              padding: '0.45rem 0.65rem', fontSize: '0.72rem',
                              cursor: retryLoading === booking.id ? 'wait' : 'pointer',
                              transition: 'all 0.2s', fontWeight: 600,
                              opacity: retryLoading === booking.id ? 0.6 : 1,
                            }}
                            onMouseOver={(e) => { if (retryLoading !== booking.id) e.currentTarget.style.background = 'rgba(59,130,246,0.25)' }}
                            onMouseOut={(e) => e.currentTarget.style.background = 'rgba(59,130,246,0.15)'}
                          >
                            {retryLoading === booking.id ? 'Đang xử lý...' : `${booking.paymentStatus === 'FAILED' ? 'Thanh toán lại' : 'Tiếp tục thanh toán'} (${getCountdown(booking.expiredAt)})`}
                          </button>
                        )}
                        {(booking.paymentStatus === 'FAILED' || booking.paymentStatus === 'PENDING') && !getCountdown(booking.expiredAt) && (
                          <span style={{ color: '#94a3b8', fontSize: '0.72rem', fontStyle: 'italic' }}>
                            Hết thời gian thanh toán
                          </span>
                        )}
                        <button
                          onClick={(event) => {
                            event.stopPropagation()
                            cancelBooking(booking.id)
                          }}
                          style={{ background: 'rgba(239,68,68,0.1)', color: '#ef4444', border: '1px solid rgba(239,68,68,0.2)', borderRadius: '0.5rem', padding: '0.45rem 0.65rem', fontSize: '0.72rem', cursor: 'pointer', transition: 'all 0.2s' }}
                          onMouseOver={(e) => e.currentTarget.style.background = 'rgba(239,68,68,0.2)'}
                          onMouseOut={(e) => e.currentTarget.style.background = 'rgba(239,68,68,0.1)'}
                        >
                          Hủy đơn hàng
                        </button>
                      </div>
                    )}

                  </div>
                </div>

                {/* Tickets dropdown */}
                {selectedBooking === booking.id && (
                  <div style={{
                    marginTop: '0.5rem',
                    padding: '1.5rem',
                    background: 'rgba(255,255,255,0.03)',
                    borderRadius: '1rem',
                    border: '1px solid rgba(255,255,255,0.06)',
                  }}>
                    {ticketLoading ? (
                      <p style={{ color: '#64748b', textAlign: 'center', margin: 0 }}>Đang tải vé...</p>
                    ) : tickets.length > 0 ? (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'center' }}>
                        {tickets.map((ticket, idx) => (
                          <ETicket key={idx} ticket={ticket} />
                        ))}
                      </div>
                    ) : (
                      <p style={{ color: '#64748b', textAlign: 'center', margin: 0 }}>Không tìm thấy vé</p>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
