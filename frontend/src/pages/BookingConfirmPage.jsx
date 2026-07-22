import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Container, Card, Row, Col, Button, Spinner, Alert } from 'react-bootstrap';
import axios from 'axios';
import AppNavbar from '../components/AppNavbar';
import { API_BASE_URL, authHeader } from '../services/api';
import promotionService from '../services/promotion.service';
import { useToast } from '../context/ToastContext';

export default function BookingConfirmPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('VNPAY');
  // Promotion state
  const [couponCode, setCouponCode] = useState('');
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponResult, setCouponResult] = useState(null); // { valid, discountAmount, message }
  const [appliedCode, setAppliedCode] = useState(null);
  const [availableVouchers, setAvailableVouchers] = useState([]);
  const [pricePreview, setPricePreview] = useState(null);
  const [pricePreviewLoading, setPricePreviewLoading] = useState(false);

  const {
    showtimeId,
    lockedShowtimeSeatIds,
    totalPrice,
    combos,
    selectedCombos,
    comboTotal,
    grandTotal,
    selectedSeats,
    expiredAt,
    showtime
  } = location.state || {};

  const seatsToRender = selectedSeats || [];
  const combosToRender = selectedCombos || [];
  const ticketTotal = Number(totalPrice || 0);
  const selectedComboTotal = Number(comboTotal || combosToRender.reduce((sum, item) => sum + Number(item.price || item.unitPrice || 0) * Number(item.quantity || 0), 0));
  const rawTotal = Number(grandTotal || ticketTotal + selectedComboTotal);
  const discountAmount = couponResult?.valid ? Number(couponResult.discountAmount || 0) : 0;
  const summaryTicketTotal = Number(pricePreview?.ticketTotal ?? ticketTotal);
  const summaryComboTotal = Number(pricePreview?.comboTotal ?? selectedComboTotal);
  const summaryGrossTotal = Number(pricePreview?.grossTotal ?? rawTotal);
  const voucherDiscountAmount = Number(pricePreview?.voucherDiscountAmount ?? discountAmount);
  const membershipDiscountAmount = Number(pricePreview?.membershipDiscountAmount ?? 0);
  const membershipDiscountPercent = Number(pricePreview?.membershipDiscountPercent ?? 0);
  const membershipTierName = pricePreview?.membershipTierName;
  const totalDiscountAmount = Number(pricePreview?.discountAmount ?? voucherDiscountAmount + membershipDiscountAmount);
  const totalToPay = Number(pricePreview?.finalAmount ?? pricePreview?.totalAmount ?? Math.max(0, rawTotal - discountAmount));

  useEffect(() => {
    if (!expiredAt) return;
    const tick = () => {
      const next = Math.max(0, Math.floor((new Date(expiredAt).getTime() - Date.now()) / 1000));
      setSecondsLeft(next);
    };
    tick();
    const timer = setInterval(tick, 1000);
    return () => clearInterval(timer);
  }, [expiredAt]);

  useEffect(() => {
    promotionService.getAvailable(ticketTotal, selectedComboTotal)
      .then((res) => setAvailableVouchers(res.data || []))
      .catch(() => setAvailableVouchers([]));
  }, [ticketTotal, selectedComboTotal]);

  useEffect(() => {
    if (!lockedShowtimeSeatIds || lockedShowtimeSeatIds.length === 0 || !authHeader().Authorization) return;
    let cancelled = false;
    setPricePreviewLoading(true);
    axios.post(`${API_BASE_URL}/bookings/price-preview`, {
      showtimeSeatIds: lockedShowtimeSeatIds,
      combos: combos || combosToRender.map((combo) => ({ comboId: combo.id || combo.comboId, quantity: combo.quantity })),
      promotionCode: appliedCode || undefined,
    }, {
      headers: authHeader()
    }).then((res) => {
      if (!cancelled) setPricePreview(res.data);
    }).catch(() => {
      if (!cancelled) setPricePreview(null);
    }).finally(() => {
      if (!cancelled) setPricePreviewLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [lockedShowtimeSeatIds, appliedCode]);

  if (!lockedShowtimeSeatIds || lockedShowtimeSeatIds.length === 0) {
    return (
      <>
        <AppNavbar />
        <Container className="py-5 mt-5 text-center text-white">
          <h3>Không tìm thấy thông tin ghế đã chọn</h3>
          <Button onClick={() => navigate('/showtimes')} className="mt-3">Quay lại lịch chiếu</Button>
        </Container>
      </>
    );
  }

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return;
    setCouponLoading(true);
    setCouponResult(null);
    try {
      const res = await promotionService.validateCoupon(
        couponCode.trim(),
        ticketTotal,
        selectedComboTotal
      );
      setCouponResult(res.data);
      if (res.data.valid) {
        setAppliedCode(couponCode.trim().toUpperCase());
      } else {
        setAppliedCode(null);
      }
    } catch (err) {
      setCouponResult({ valid: false, message: 'Không thể kiểm tra mã. Vui lòng thử lại.' });
      setAppliedCode(null);
    } finally {
      setCouponLoading(false);
    }
  };

  const handleSelectVoucher = (voucher) => {
    if (!voucher?.isApplicable) {
      setCouponResult({ valid: false, message: voucher?.unavailableReason || 'Mã khuyến mãi không hợp lệ hoặc đã hết hạn.' });
      setAppliedCode(null);
      return;
    }
    setCouponCode(voucher.code);
    setAppliedCode(voucher.code);
    setCouponResult({
      valid: true,
      code: voucher.code,
      discountAmount: voucher.estimatedDiscountAmount || 0,
      message: 'Áp dụng mã thành công!',
    });
  };

  const handleRemoveCoupon = () => {
    setCouponCode('');
    setCouponResult(null);
    setAppliedCode(null);
  };

  const handlePayment = async () => {
    setLoading(true);
    setError(null);
    try {
      if (!authHeader().Authorization) {
        throw new Error('Vui lòng đăng nhập trước khi thanh toán!');
      }

      // Bước 1: Tạo Booking
      const bookingRes = await axios.post(`${API_BASE_URL}/bookings`, {
        showtimeSeatIds: lockedShowtimeSeatIds,
        combos: combos || combosToRender.map((combo) => ({ comboId: combo.id || combo.comboId, quantity: combo.quantity })),
        promotionCode: appliedCode || undefined,
      }, {
        headers: authHeader()
      });

      const bookingId = bookingRes.data.id;

      if (paymentMethod === 'VIETQR') {
        navigate(`/payment/vietqr?bookingId=${bookingId}`);
        return;
      }

      // Bước 2: Lấy URL thanh toán VNPay
      const vnpayRes = await axios.get(`${API_BASE_URL}/payment/vnpay/create-url?bookingId=${bookingId}`, {
        headers: authHeader()
      });

      // Bước 3: Chuyển hướng sang VNPay
      window.location.href = vnpayRes.data.paymentUrl;

    } catch (err) {
      console.error(err);
      const message = err.response?.data?.message || err.response?.data || err.message || 'Có lỗi xảy ra khi xử lý thanh toán.';
      if (String(message).includes('Suất chiếu')) {
        const errMsg = 'Suất chiếu đã hết thời gian đặt vé. Đang quay lại lịch chiếu...';
        setError(errMsg);
        toast.error(errMsg, 4000);
        setTimeout(() => navigate('/showtimes'), 1800);
      } else {
        const errMsg = `${message}${String(message).toLowerCase().includes('expired') ? ' Vui lòng chọn ghế lại.' : ''}`;
        setError(errMsg);
        toast.error(errMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  const vnd = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  const seatLabel = (seat) => `${seat.rowLabel || seat.seat?.rowLabel || ''}${seat.columnNumber || seat.seat?.columnNumber || ''}`;
  const fmtTime = (value) => value ? new Date(value).toLocaleString('vi-VN') : 'Đang cập nhật';
  const lockTime = secondsLeft !== null
    ? `${String(Math.floor(secondsLeft / 60)).padStart(2, '0')}:${String(secondsLeft % 60).padStart(2, '0')}`
    : null;

  return (
    <>
      <AppNavbar />
      <div style={{ minHeight: '100vh', background: 'var(--surface)', paddingTop: 80 }}>
        <Container style={{ maxWidth: 600 }} className="py-4">
          <Card className="border-0 text-white" style={{ background: 'rgba(255,255,255,0.05)', borderRadius: '1rem' }}>
            <Card.Body className="p-4">
              <h3 className="fw-bold mb-4 text-center" style={{ color: 'var(--on-surface)' }}>Xác nhận đặt vé</h3>
              
              {error && <Alert variant="danger">{error}</Alert>}

              <div className="mb-4">
                <h5 style={{ color: '#aaa' }}>Thông tin suất chiếu</h5>
                <div style={{ color: 'var(--on-surface)' }}>
                  <div className="fw-bold">{showtime?.movieTitle || 'Phim đang cập nhật'}</div>
                  <div>{showtime?.roomName || 'Phòng TBA'} • {showtime?.screenType || '2D'}</div>
                  <div>{fmtTime(showtime?.startTime)}</div>
                  {lockTime && <div style={{ color: secondsLeft < 60 ? '#ef4444' : '#facc15' }}>Giữ ghế còn {lockTime}</div>}
                </div>
              </div>

              <div className="mb-4">
                <h5 style={{ color: '#aaa' }}>Thông tin ghế đã giữ</h5>
                <div className="d-flex flex-wrap gap-2 mt-2">
                  {seatsToRender.map(seat => (
                    <span key={seat.showtimeSeatId} style={{
                      background: '#7c5e10', border: '1px solid #ffd700',
                      padding: '4px 12px', borderRadius: '4px', fontWeight: 'bold'
                    }}>
                      Ghế {seatLabel(seat)}
                    </span>
                  ))}
                </div>
              </div>

              {seatsToRender.length > 0 && (
                <div className="mb-4">
                  <h5 style={{ color: '#aaa' }}>Chi tiết giá vé</h5>
                  {seatsToRender.map((seat) => (
                    <Row key={seat.showtimeSeatId} className="mb-2">
                      <Col>Ghế {seatLabel(seat)} ({seat.seatType || seat.seat?.seatType || 'NORMAL'})</Col>
                      <Col className="text-end">{vnd(seat.finalPrice || seat.basePrice || 0)}</Col>
                    </Row>
                  ))}
                </div>
              )}

              {combosToRender.length > 0 && (
                <div className="mb-4">
                  <h5 style={{ color: '#aaa' }}>Bắp nước / combo</h5>
                  {combosToRender.map((combo) => (
                    <Row key={combo.id || combo.comboId} className="mb-2">
                      <Col>{combo.name || combo.comboName} x {combo.quantity}</Col>
                      <Col className="text-end">{vnd(Number(combo.price || combo.unitPrice || 0) * Number(combo.quantity || 0))}</Col>
                    </Row>
                  ))}
                </div>
              )}

              <div className="mb-4">
                <Row>
                  <Col><h5 style={{ color: '#aaa' }}>Tổng số ghế</h5></Col>
                  <Col className="text-end fw-bold">{lockedShowtimeSeatIds.length} ghế</Col>
                </Row>
                <hr style={{ borderColor: 'rgba(255,255,255,0.1)' }}/>
                <Row>
                  <Col><h5 style={{ color: '#aaa' }}>Tiền vé</h5></Col>
                  <Col className="text-end fw-bold">{vnd(summaryTicketTotal)}</Col>
                </Row>
                <Row>
                  <Col><h5 style={{ color: '#aaa' }}>Tiền bắp nước/combo</h5></Col>
                  <Col className="text-end fw-bold">{vnd(summaryComboTotal)}</Col>
                </Row>
                <Row>
                  <Col><h5 style={{ color: '#aaa' }}>Tạm tính</h5></Col>
                  <Col className="text-end fw-bold">{vnd(summaryGrossTotal)}</Col>
                </Row>

                {/* ── Coupon input ── */}
                <hr style={{ borderColor: 'rgba(255,255,255,0.1)' }}/>
                <div className="mb-3">
                  <h5 style={{ color: '#aaa' }}>Ưu đãi / Voucher</h5>
                  {availableVouchers.length > 0 && (
                    <div style={{ display: 'grid', gap: 8, marginTop: 8, marginBottom: 12 }}>
                      {availableVouchers.map((voucher) => (
                        <button
                          key={voucher.id || voucher.code}
                          type="button"
                          onClick={() => handleSelectVoucher(voucher)}
                          disabled={loading || couponLoading}
                          style={{
                            textAlign: 'left',
                            background: appliedCode === voucher.code ? 'rgba(34,197,94,0.14)' : 'rgba(255,255,255,0.05)',
                            border: appliedCode === voucher.code ? '1px solid rgba(34,197,94,0.6)' : '1px solid rgba(255,255,255,0.1)',
                            color: voucher.isApplicable ? 'var(--on-surface)' : '#94a3b8',
                            borderRadius: '0.75rem',
                            padding: '0.75rem 1rem',
                            cursor: 'pointer',
                            opacity: voucher.isApplicable ? 1 : 0.65,
                          }}
                        >
                          <div className="fw-bold">{voucher.code} - {voucher.description}</div>
                          <div style={{ color: voucher.isApplicable ? '#86efac' : '#fca5a5', fontSize: '0.82rem' }}>
                            {voucher.isApplicable
                              ? `Dự kiến giảm ${vnd(voucher.estimatedDiscountAmount || 0)}`
                              : voucher.unavailableReason || 'Chưa đủ điều kiện áp dụng'}
                            {voucher.endDate ? ` · Hết hạn ${new Date(voucher.endDate).toLocaleDateString('vi-VN')}` : ''}
                          </div>
                        </button>
                      ))}
                    </div>
                  )}
                  {appliedCode ? (
                    <div style={{
                      display: 'flex', alignItems: 'center', gap: 10,
                      background: 'rgba(34,197,94,0.1)', border: '1px solid rgba(34,197,94,0.4)',
                      borderRadius: '0.75rem', padding: '0.75rem 1rem', marginTop: 8,
                    }}>
                      <span style={{ fontSize: 18 }}>🎟️</span>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, color: '#22c55e' }}>Đã áp dụng: {appliedCode}</div>
                        <div style={{ fontSize: '0.82rem', color: '#86efac' }}>Giảm: -{vnd(voucherDiscountAmount)}</div>
                      </div>
                      <button
                        type="button"
                        onClick={handleRemoveCoupon}
                        style={{
                          background: 'none', border: '1px solid rgba(239,68,68,0.5)',
                          color: '#ef4444', borderRadius: '0.5rem', padding: '4px 10px',
                          cursor: 'pointer', fontSize: '0.8rem'
                        }}
                      >
                        Xóa
                      </button>
                    </div>
                  ) : (
                    <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                      <input
                        id="coupon-code-input"
                        type="text"
                        value={couponCode}
                        onChange={(e) => {
                          setCouponCode(e.target.value.toUpperCase());
                          setCouponResult(null);
                        }}
                        onKeyDown={(e) => e.key === 'Enter' && handleApplyCoupon()}
                        placeholder="Nhập mã voucher..."
                        disabled={loading || couponLoading}
                        style={{
                          flex: 1, background: 'rgba(255,255,255,0.07)',
                          border: couponResult && !couponResult.valid
                            ? '1px solid rgba(239,68,68,0.6)'
                            : '1px solid rgba(255,255,255,0.15)',
                          color: 'var(--on-surface)', borderRadius: '0.5rem',
                          padding: '8px 12px', outline: 'none', fontSize: '0.9rem',
                          letterSpacing: '1px', textTransform: 'uppercase',
                        }}
                      />
                      <button
                        id="apply-coupon-btn"
                        type="button"
                        onClick={handleApplyCoupon}
                        disabled={!couponCode.trim() || loading || couponLoading}
                        style={{
                          background: 'linear-gradient(90deg,#6366f1,#8b5cf6)',
                          border: 'none', borderRadius: '0.5rem',
                          color: '#fff', padding: '8px 16px',
                          cursor: couponCode.trim() ? 'pointer' : 'not-allowed',
                          opacity: couponCode.trim() ? 1 : 0.5,
                          fontWeight: 600, fontSize: '0.9rem',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {couponLoading ? <Spinner size="sm" /> : 'Áp dụng'}
                      </button>
                    </div>
                  )}
                  {couponResult && !couponResult.valid && (
                    <div style={{ color: '#ef4444', fontSize: '0.82rem', marginTop: 4 }}>
                      ⚠️ {couponResult.message}
                    </div>
                  )}
                </div>

                <hr style={{ borderColor: 'rgba(255,255,255,0.1)' }}/>
                {voucherDiscountAmount > 0 && (
                  <Row className="mb-1">
                    <Col><h5 style={{ color: '#22c55e' }}>Khuyến mãi ({appliedCode})</h5></Col>
                    <Col className="text-end fw-bold" style={{ color: '#22c55e' }}>-{vnd(voucherDiscountAmount)}</Col>
                  </Row>
                )}
                <div style={{ margin: '0.75rem 0' }}>
                  <h5 style={{ color: '#aaa' }}>Ưu đãi thành viên</h5>
                  {membershipDiscountAmount > 0 ? (
                    <>
                      <div style={{ color: '#cbd5e1', fontSize: '0.9rem' }}>
                        {membershipTierName} - Giảm {membershipDiscountPercent.toLocaleString('vi-VN')}% mỗi giao dịch
                      </div>
                      <Row className="mt-1">
                        <Col><span style={{ color: '#22c55e' }}>Giảm giá thành viên</span></Col>
                        <Col className="text-end fw-bold" style={{ color: '#22c55e' }}>-{vnd(membershipDiscountAmount)}</Col>
                      </Row>
                    </>
                  ) : (
                    <div style={{ color: '#94a3b8', fontSize: '0.88rem' }}>Chưa có ưu đãi thành viên áp dụng.</div>
                  )}
                </div>
                {totalDiscountAmount > 0 && (
                  <Row className="mb-1">
                    <Col><h5 style={{ color: '#aaa' }}>Tổng giảm giá</h5></Col>
                    <Col className="text-end fw-bold" style={{ color: '#22c55e' }}>-{vnd(totalDiscountAmount)}</Col>
                  </Row>
                )}
                <Row>
                  <Col><h5 style={{ color: '#aaa' }}>Tổng thanh toán</h5></Col>
                  <Col className="text-end fw-bold" style={{ fontSize: '1.5rem', color: '#22c55e' }}>{pricePreviewLoading ? 'Đang tính...' : vnd(totalToPay)}</Col>
                </Row>
              </div>

              <div className="mb-4">
                <h5 style={{ color: '#aaa' }}>Phương thức thanh toán</h5>
                <div className="d-grid gap-2 mt-3">
                  {[
                    { value: 'VNPAY', title: 'VNPay Sandbox', note: 'Thanh toán thử qua cổng VNPay sandbox.' },
                    { value: 'VIETQR', title: 'Chuyển khoản VietQR', note: 'Quét QR chuyển khoản vào tài khoản ngân hàng đã cấu hình.' },
                  ].map(option => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setPaymentMethod(option.value)}
                      disabled={loading}
                      style={{
                        textAlign: 'left',
                        background: paymentMethod === option.value ? 'rgba(34,197,94,0.14)' : 'rgba(255,255,255,0.05)',
                        border: paymentMethod === option.value ? '1px solid rgba(34,197,94,0.6)' : '1px solid rgba(255,255,255,0.1)',
                        color: 'var(--on-surface)',
                        borderRadius: '0.75rem',
                        padding: '0.85rem 1rem',
                        cursor: loading ? 'not-allowed' : 'pointer',
                      }}
                    >
                      <div className="fw-bold">{option.title}</div>
                      <div style={{ color: '#94a3b8', fontSize: '0.85rem' }}>{option.note}</div>
                    </button>
                  ))}
                </div>
              </div>

              <div className="d-grid gap-3 mt-5">
                <Button 
                  onClick={handlePayment} 
                  disabled={loading}
                  style={{ background: 'linear-gradient(90deg, #6366f1, #8b5cf6)', border: 'none', padding: '12px' }}
                  className="fw-bold fs-5"
                >
                  {loading ? <Spinner size="sm" className="me-2" /> : null}
                  {loading ? 'Đang xử lý...' : paymentMethod === 'VIETQR' ? 'Tạo mã VietQR' : 'Thanh toán bằng VNPay'}
                </Button>
                <Button 
                  variant="outline-light" 
                  onClick={() => navigate(`/showtimes/${showtimeId}/seats`)}
                  disabled={loading}
                >
                  Quay lại chọn ghế
                </Button>
              </div>

            </Card.Body>
          </Card>
        </Container>
      </div>
    </>
  );
}
