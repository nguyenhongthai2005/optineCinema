import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import AppNavbar from '../components/AppNavbar'
import { API_BASE_URL, authHeader } from '../services/api'

export default function VietQrPaymentPage() {
  const [params] = useSearchParams()
  const bookingId = params.get('bookingId')
  const navigate = useNavigate()
  const [paymentInfo, setPaymentInfo] = useState(null)
  const [loading, setLoading] = useState(true)
  const [marking, setMarking] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [expired, setExpired] = useState(false)
  const submittingRef = useRef(false)

  useEffect(() => {
    createPayment()
  }, [bookingId])

  const createPayment = async () => {
    if (!bookingId) {
      setError('Thiếu mã đơn hàng')
      setLoading(false)
      return
    }
    try {
      const res = await axios.post(`${API_BASE_URL}/payment/vietqr/create`, { bookingId: Number(bookingId) }, {
        headers: authHeader()
      })
      setPaymentInfo(res.data)
    } catch (err) {
      setError(errorMessage(err, 'Không tạo được mã VietQR'))
    } finally {
      setLoading(false)
    }
  }

  const copyText = async (value) => {
    try {
      await navigator.clipboard.writeText(value || '')
      setMessage('Đã sao chép')
    } catch {
      setMessage('Không sao chép được, vui lòng copy thủ công')
    }
  }

  const markTransferred = async () => {
    if (submittingRef.current || expired) return
    submittingRef.current = true
    setMarking(true)
    setError('')
    try {
      const res = await axios.post(`${API_BASE_URL}/payment/vietqr/mark-transferred`, { bookingId: Number(bookingId) }, {
        headers: authHeader()
      })
      const data = res.data || {}
      setMessage(data.message || 'Thanh toán của bạn đang chờ admin xác nhận. Vé sẽ được gửi qua email sau khi xác nhận.')
      if (data.paymentStatus === 'PAID' || data.bookingStatus === 'CONFIRMED') {
        navigate(`/payment-result?status=success&bookingId=${bookingId}&method=VIETQR&autoConfirmed=true`)
      }
    } catch (err) {
      const text = errorMessage(err, 'Không cập nhật được trạng thái chuyển khoản')
      setError(text)
      setExpired(String(text).includes('hết hạn') || String(text).includes('Suất chiếu'))
    } finally {
      submittingRef.current = false
      setMarking(false)
    }
  }

  const vnd = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(amount || 0))

  return (
    <>
      <AppNavbar />
      <div style={{ minHeight: '100vh', background: '#0f172a', padding: '96px 1rem 2rem' }}>
        <div style={{ maxWidth: 760, margin: '0 auto', color: '#e2e8f0' }}>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '1rem' }}>Chuyển khoản VietQR</h1>

          {loading ? (
            <div style={{ color: '#94a3b8' }}>Đang tạo mã QR...</div>
          ) : error ? (
            <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.35)', color: '#fecaca', borderRadius: 8, padding: '1rem' }}>
              {error}
              {expired && (
                <div style={{ marginTop: 12 }}>
                  <button onClick={() => navigate('/showtimes')} style={secondaryButton}>Quay lại lịch chiếu</button>
                </div>
              )}
            </div>
          ) : paymentInfo && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.25rem', alignItems: 'start' }}>
              <div style={{ background: '#fff', borderRadius: 8, padding: '1rem' }}>
                <img src={paymentInfo.qrImageUrl} alt="VietQR" style={{ display: 'block', width: '100%', height: 'auto' }} />
              </div>

              <div style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, padding: '1.25rem' }}>
                {[
                  ['Tổng thanh toán', vnd(paymentInfo.finalAmount || paymentInfo.originalAmount || paymentInfo.amount)],
                  ...(Number(paymentInfo.voucherDiscountAmount || 0) > 0 ? [[`Voucher`, `-${vnd(paymentInfo.voucherDiscountAmount)}`]] : []),
                  ...(Number(paymentInfo.membershipDiscountAmount || 0) > 0 ? [[`Ưu đãi thành viên ${paymentInfo.membershipTierName || ''} (${Number(paymentInfo.membershipDiscountPercent || 0).toLocaleString('vi-VN')}%)`, `-${vnd(paymentInfo.membershipDiscountAmount)}`]] : []),
                  ...(Number(paymentInfo.discountAmount || 0) > 0 ? [['Tổng giảm giá', `-${vnd(paymentInfo.discountAmount)}`]] : []),
                  ['Số tiền chuyển khoản demo', vnd(paymentInfo.payableAmount || paymentInfo.amount)],
                  ['Ngân hàng', paymentInfo.bankId],
                  ['Số tài khoản', paymentInfo.accountNo],
                  ['Tên tài khoản', paymentInfo.accountName],
                  ['Nội dung chuyển khoản', paymentInfo.transferContent],
                ].map(([label, value]) => (
                  <div key={label} style={{ marginBottom: '0.9rem' }}>
                    <div style={{ color: '#94a3b8', fontSize: '0.78rem', textTransform: 'uppercase', fontWeight: 700 }}>{label}</div>
                    <div style={{ color: '#f8fafc', fontWeight: 700, wordBreak: 'break-word' }}>{value}</div>
                  </div>
                ))}

                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', margin: '1rem 0' }}>
                  <button onClick={() => copyText(paymentInfo.accountNo)} style={secondaryButton}>Copy số tài khoản</button>
                  <button onClick={() => copyText(paymentInfo.transferContent)} style={secondaryButton}>Copy nội dung</button>
                </div>

                {paymentInfo.demoMode && (
                  <div style={{ background: 'rgba(59,130,246,0.12)', border: '1px solid rgba(96,165,250,0.3)', color: '#bfdbfe', borderRadius: 8, padding: '0.85rem', margin: '1rem 0' }}>
                    <strong>Chế độ demo:</strong> hệ thống chỉ yêu cầu chuyển khoản số tiền test. Sau khi bấm "Tôi đã chuyển khoản", đơn hàng sẽ được xác nhận tự động.
                    {Number(paymentInfo.demoDiscountAmount || 0) > 0 && (
                      <div style={{ marginTop: 6 }}>Tiết kiệm demo: {vnd(paymentInfo.demoDiscountAmount)}</div>
                    )}
                  </div>
                )}

                <p style={{ color: '#facc15', margin: '1rem 0' }}>
                  Vui lòng chuyển đúng số tiền cần chuyển khoản và đúng nội dung chuyển khoản.
                  {!paymentInfo.autoConfirmEnabled && ' Đơn hàng sẽ được xác nhận sau khi nhân viên/admin kiểm tra chuyển khoản.'}
                </p>

                {message && (
                  <div style={{ background: 'rgba(34,197,94,0.12)', border: '1px solid rgba(34,197,94,0.35)', color: '#bbf7d0', borderRadius: 8, padding: '0.8rem', marginBottom: '1rem' }}>
                    {message === 'Payment is waiting for admin confirmation.'
                      ? 'Thanh toán của bạn đang chờ admin xác nhận. Vé sẽ được gửi qua email sau khi xác nhận.'
                      : message}
                  </div>
                )}
                {expired && (
                  <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.35)', color: '#fecaca', borderRadius: 8, padding: '0.8rem', marginBottom: '1rem' }}>
                    Đơn đặt vé đã hết hạn thanh toán.
                  </div>
                )}

                <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                  <button onClick={markTransferred} disabled={marking || expired} style={primaryButton}>
                    {marking ? 'Đang xác nhận...' : 'Tôi đã chuyển khoản'}
                  </button>
                  {expired && <button onClick={() => navigate('/showtimes')} style={secondaryButton}>Quay lại lịch chiếu</button>}
                  <button onClick={() => navigate('/my-bookings')} style={secondaryButton}>Vé của tôi</button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

const primaryButton = {
  background: '#22c55e',
  color: '#052e16',
  border: 'none',
  borderRadius: 8,
  padding: '0.75rem 1rem',
  fontWeight: 800,
  cursor: 'pointer',
}

const secondaryButton = {
  background: 'rgba(255,255,255,0.08)',
  color: '#e2e8f0',
  border: '1px solid rgba(255,255,255,0.15)',
  borderRadius: 8,
  padding: '0.7rem 0.9rem',
  fontWeight: 700,
  cursor: 'pointer',
}

function errorMessage(err, fallback) {
  const status = err.response?.status
  if (status === 401) return 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.'
  if (status === 403) return 'Bạn không có quyền xác nhận thanh toán cho đơn này.'
  if (status === 409 || status === 503) return 'Hệ thống đang xử lý giao dịch khác, vui lòng thử lại sau.'
  const data = err.response?.data
  if (typeof data === 'string') return data
  if (data?.message) return data.message
  return fallback
}
