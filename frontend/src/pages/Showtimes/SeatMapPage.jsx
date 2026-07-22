import { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Button, Badge,
  Spinner, Alert, Stack,
} from 'react-bootstrap';
import { AuthContext } from '../../context/AuthContext';
import AppNavbar from '../../components/AppNavbar';
import showtimeService from '../../services/showtime.service';
import { connectSeatSocket } from '../../services/seatHoldSocket';

const SEAT_COLORS = {
  NORMAL:  { bg: '#1e2d40', border: '#4a90d9', selected: '#4a90d9', label: 'Thường' },
  VIP:     { bg: '#2d1e40', border: '#a855f7', selected: '#a855f7', label: 'VIP'    },
  COUPLE:  { bg: '#401e2d', border: '#f43f5e', selected: '#f43f5e', label: 'Đôi'    },
  PREMIUM: { bg: '#1e3330', border: '#10b981', selected: '#10b981', label: 'Premium'},
  MAINTENANCE: { bg: '#450a0a', border: '#ef4444', selected: '#450a0a', label: 'Bảo trì' },
};

const vnd = (n) => (Math.round(n || 0)).toLocaleString('vi-VN') + '₫';
const fmtTime = (s) =>
  `${String(Math.floor(s/60)).padStart(2,'0')}:${String(s%60).padStart(2,'0')}`;
const seatLabel = (seat) => `${seat.rowLabel}${seat.columnNumber}`;
const seatPrice = (seat) => Number(seat.finalPrice || seat.basePrice || 0);
const multiplier = (value) => Number(value || 1).toLocaleString('vi-VN', { maximumFractionDigits: 2 });

export default function SeatMapPage() {
  const { id: showtimeId } = useParams();
  const navigate           = useNavigate();
  const { user }           = useContext(AuthContext);

  const [seats, setSeats]           = useState([]);
  const [showtime, setShowtime]     = useState(null);
  const [selected, setSelected]     = useState(new Set());
  const [loading, setLoading]       = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError]           = useState('');
  const [expiredShowtime, setExpiredShowtime] = useState(false);
  const [info, setInfo]             = useState('');
  const [countdown, setCountdown]   = useState(null);
  const [lockData, setLockData]     = useState(null);

  // Load sơ đồ ghế
  useEffect(() => {
    Promise.all([
      showtimeService.getShowtime(showtimeId),
      showtimeService.getSeats(showtimeId),
    ])
      .then(([showtimeRes, seatsRes]) => {
        setShowtime(showtimeRes.data);
        const nextSeats = seatsRes.data || [];
        setSeats(nextSeats);
        const ownLocked = nextSeats.filter((seat) => seat.status === 'LOCKED' && seat.lockedByCurrentUser);
        if (ownLocked.length > 0) {
          setSelected(new Set(ownLocked.map((seat) => seat.seatId)));
          const expiresAt = ownLocked
            .map((seat) => seat.lockExpiresAt)
            .filter(Boolean)
            .sort()[0];
          const totalPrice = ownLocked.reduce((sum, seat) => sum + seatPrice(seat), 0);
          setLockData({
            lockedShowtimeSeatIds: ownLocked.map((seat) => seat.showtimeSeatId),
            expiredAt: expiresAt,
            totalPrice,
            showtimeId,
          });
          if (expiresAt) {
            const seconds = Math.max(0, Math.floor((new Date(expiresAt) - Date.now()) / 1000));
            setCountdown(seconds > 0 ? seconds : 0);
          }
          setInfo('Bạn đang giữ ghế từ bước trước. Có thể tiếp tục thanh toán.');
        }
      })
      .catch((err) => {
        console.error('Cannot load seat map', err);
        const raw = err.response?.data?.message || err.response?.data || '';
        if (String(raw).includes('Suất chiếu')) {
          setExpiredShowtime(true);
          setError('Suất chiếu đã hết thời gian đặt vé.');
        } else {
          setError('Không thể tải sơ đồ ghế.');
        }
      })
      .finally(() => setLoading(false));
  }, [showtimeId]);

  // ─── WebSocket subscription (AstraCine pattern) ─────
  useEffect(() => {
    const cleanup = connectSeatSocket(showtimeId, (event) => {
      // event: { type, showtimeId, seatIds, byUserId, expiresAt }
      const idsSet = new Set(event.seatIds);
      const isMine = user && event.byUserId === user.id;

      // Map event type → seat status mới trong state
      const newStatus = event.type === 'SEAT_HELD'  ? 'LOCKED'
                      : event.type === 'SEAT_SOLD'  ? 'BOOKED'
                      : 'AVAILABLE';

      setSeats(prevSeats => {
        const labels = [];
        const updated = prevSeats.map(s => {
          if (idsSet.has(s.seatId)) {
            labels.push(`${s.rowLabel}${s.columnNumber}`);
            return { ...s, status: newStatus };
          }
          return s;
        });

        // Nếu là người khác giữ/mua ghế → bỏ khỏi selected của mình
        if ((event.type === 'SEAT_HELD' || event.type === 'SEAT_SOLD') && !isMine) {
          setSelected(prevSel => {
            const next = new Set(prevSel);
            let removedAny = false;
            idsSet.forEach(id => {
              if (next.has(id)) { next.delete(id); removedAny = true; }
            });
            if (removedAny) {
              setInfo(`Ghế ${labels.join(', ')} vừa được người khác đặt — đã bỏ khỏi lựa chọn của bạn.`);
              setTimeout(() => setInfo(''), 4000);
            }
            return next;
          });
        }

        return updated;
      });
    });

    return cleanup;
  }, [showtimeId, user]);

  // Countdown timer
  useEffect(() => {
    if (countdown === null) return;
    if (countdown <= 0) {
      setSelected(new Set());
      setLockData(null);
      setCountdown(null);
      setError('Đã hết thời gian giữ ghế. Vui lòng chọn lại.');
      showtimeService.getSeats(showtimeId).then(r => setSeats(r.data));
      return;
    }
    const t = setTimeout(() => setCountdown(c => c - 1), 1000);
    return () => clearTimeout(t);
  }, [countdown, showtimeId]);

  // Group ghế theo hàng
  const seatsByRow = seats.reduce((acc, s) => {
    if (!acc[s.rowLabel]) acc[s.rowLabel] = [];
    acc[s.rowLabel].push(s);
    return acc;
  }, {});
  const sortedRows = Object.keys(seatsByRow).sort();

  const toggleSeat = (seat) => {
    if (seat.status === 'MAINTENANCE' || seat.maintenance) {
      setInfo('Ghế đang bảo trì, vui lòng chọn ghế khác.');
      setTimeout(() => setInfo(''), 3000);
      return;
    }
    if (seat.status === 'BOOKED' || (seat.status === 'LOCKED' && !seat.lockedByCurrentUser)) return;
    if (countdown !== null) return;
    setSelected(prev => {
      const next = new Set(prev);
      next.has(seat.seatId) ? next.delete(seat.seatId) : next.add(seat.seatId);
      return next;
    });
  };

  const estimatedTotal = seats
    .filter(s => selected.has(s.seatId))
    .reduce((sum, s) => sum + Number(s.finalPrice || s.basePrice || 0), 0);

  const handleConfirm = async () => {
    if (!user) {
      navigate('/login', { state: { from: { pathname: `/showtimes/${showtimeId}/seats` } } });
      return;
    }
    if (selected.size === 0) {
      setError('Vui lòng chọn ít nhất 1 ghế.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      const res  = await showtimeService.lockSeats(showtimeId, Array.from(selected));
      const data = res.data;
      setLockData(data);
      setCountdown(Math.floor((new Date(data.expiredAt) - Date.now()) / 1000));
    } catch (err) {
      const raw = err.response?.data?.message || err.response?.data || '';
      if (String(raw).includes('Suất chiếu')) {
        setExpiredShowtime(true);
        setError('Suất chiếu đã hết thời gian đặt vé.');
      } else {
        setError('Có lỗi khi giữ ghế. Vui lòng thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const goToPayment = () => {
    navigate('/booking/combos', {
      state: {
        showtimeId,
        lockedShowtimeSeatIds: lockData.lockedShowtimeSeatIds,
        totalPrice: lockData.totalPrice,
        expiredAt: lockData.expiredAt,
        selectedSeats: seats.filter(s =>
          lockData.lockedShowtimeSeatIds.includes(s.showtimeSeatId)),
        showtime,
      }
    });
  };

  const handleReset = async () => {
    try { await showtimeService.releaseSeats(showtimeId); } catch {}
    setSelected(new Set());
    setLockData(null);
    setCountdown(null);
    setError('');
    const res = await showtimeService.getSeats(showtimeId);
    setSeats(res.data);
  };

  if (loading) {
    return (
      <>
        <AppNavbar />
        <div className="d-flex justify-content-center align-items-center" style={{ height: '80vh' }}>
          <Spinner animation="border" variant="light" />
        </div>
      </>
    );
  }

  return (
    <>
      <AppNavbar />
      <div style={{ minHeight: '100vh', background: 'var(--surface)', paddingTop: 80 }}>
        <Container style={{ maxWidth: 900 }} className="py-4">

          <Row className="align-items-center mb-4 g-2">
            <Col>
              <h3 className="fw-bold mb-1" style={{ color: 'var(--on-surface)' }}>
                Chọn ghế
              </h3>
              <p className="mb-0" style={{ color: 'var(--on-surface-variant)', fontSize: 13 }}>
                {showtime?.movieTitle || `Suất chiếu #${showtimeId}`}
                {showtime?.displayStatusLabel && (
                  <Badge bg="primary" className="ms-2" style={{ fontSize: 10 }}>{showtime.displayStatusLabel}</Badge>
                )}
              </p>
              {showtime && (
                <p className="mb-0" style={{ color: 'var(--on-surface-variant)', fontSize: 12 }}>
                  {showtime.roomName} • {new Date(showtime.startTime).toLocaleString('vi-VN')}
                </p>
              )}
            </Col>
            {countdown !== null && (
              <Col xs="auto">
                <Badge
                  bg={countdown < 60 ? 'danger' : 'warning'}
                  text={countdown < 60 ? 'light' : 'dark'}
                  className="px-3 py-2 fs-6"
                >
                  ⏱ Còn {fmtTime(countdown)}
                </Badge>
              </Col>
            )}
          </Row>

          {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
          {expiredShowtime && (
            <Button variant="outline-light" className="mb-3" onClick={() => navigate('/showtimes')}>
              Quay lại lịch chiếu
            </Button>
          )}
          {info  && <Alert variant="info"   onClose={() => setInfo('')}  dismissible>{info}</Alert>}

          <div className="text-center mb-4">
            <div style={{
              width: '70%', margin: '0 auto', height: 6,
              background: 'linear-gradient(to right, transparent, #888, transparent)',
              borderRadius: 3,
            }} />
            <small className="d-block mt-2" style={{ color: '#666' }}>MÀN HÌNH</small>
          </div>

          <Card className="mb-4 border-0" style={{ background: 'rgba(255,255,255,0.03)' }}>
            <Card.Body style={{ overflowX: 'auto' }}>
              {sortedRows.map(row => (
                <Stack
                  key={row}
                  direction="horizontal"
                  gap={2}
                  className="mb-2 justify-content-center align-items-center"
                >
                  <span style={{
                    width: 24, textAlign: 'center', color: '#888',
                    fontSize: 12, fontWeight: 600, flexShrink: 0,
                  }}>
                    {row}
                  </span>
                  <div className="d-flex flex-wrap gap-1 justify-content-center">
                    {seatsByRow[row]
                      .sort((a, b) => a.columnNumber - b.columnNumber)
                      .map(seat => {
                        const isSelected = selected.has(seat.seatId);
                        const isBooked   = seat.status === 'BOOKED';
                        const isMaintenance = seat.status === 'MAINTENANCE' || seat.maintenance;
                        // Kiểm tra ghế MÌNH lock TRƯỚC, rồi ghế người khác lock là phần còn lại
                        const isMyLocked = seat.status === 'LOCKED' && (seat.lockedByCurrentUser || (lockData &&
                                           lockData.lockedShowtimeSeatIds.includes(seat.showtimeSeatId)));
                        const isLocked   = seat.status === 'LOCKED' && !isMyLocked;
                        const colors     = isMaintenance ? SEAT_COLORS.MAINTENANCE : SEAT_COLORS[seat.seatType] || SEAT_COLORS.NORMAL;

                        let bg, border, cursor, label;
                        if (isMaintenance)   { bg = colors.bg; border = colors.border; cursor = 'not-allowed'; label = 'Bảo trì'; }
                        else if (isBooked)   { bg = '#1a1a1a'; border = '#333'; cursor = 'not-allowed'; label = 'Đã bán'; }
                        else if (isMyLocked) { bg = '#7c5e10'; border = '#ffd700'; cursor = 'default';    label = 'Bạn đang giữ'; }
                        else if (isLocked)   { bg = '#2a2a2a'; border = '#555'; cursor = 'not-allowed';   label = 'Đang được giữ'; }
                        else if (isSelected) { bg = colors.selected; border = colors.selected; cursor = 'pointer'; label = colors.label; }
                        else                 { bg = colors.bg; border = colors.border; cursor = 'pointer'; label = colors.label; }

                        return (
                          <div
                            key={seat.showtimeSeatId}
                            onClick={() => toggleSeat(seat)}
                            title={`${seatLabel(seat)} - ${seat.seatTypeLabel || label}: ${vnd(seatPrice(seat))}
Giá ghế: ${vnd(seat.basePrice)}
Phòng ${seat.screenType || seat.roomName || 'thường'}: x${multiplier(seat.roomMultiplier)}
${seat.timeSlotName || 'Khung giờ'}: x${multiplier(seat.timeSlotMultiplier)}
Tạm tính: ${vnd(seatPrice(seat))}`}
                            style={{
                              width: 32, height: 32, borderRadius: 5,
                              background: bg,
                              border: `1.5px solid ${border}`,
                              cursor,
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontSize: 10,
                              color: isMaintenance ? '#fecaca'
                                : isBooked || isLocked ? '#444'
                                : isMyLocked ? '#fff'
                                : isSelected ? '#fff' : '#aaa',
                              fontWeight: 600,
                              transition: 'transform 0.1s, background 0.25s, border-color 0.25s',
                              userSelect: 'none',
                            }}
                            onMouseEnter={e => {
                              if (cursor === 'pointer') e.currentTarget.style.transform = 'scale(1.1)';
                            }}
                            onMouseLeave={e => { e.currentTarget.style.transform = 'scale(1)'; }}
                          >
                            {seat.columnNumber}
                          </div>
                        );
                      })}
                  </div>
                </Stack>
              ))}
            </Card.Body>
          </Card>

          <Stack direction="horizontal" gap={4} className="justify-content-center mb-4 flex-wrap">
            {[
              { color: '#1e2d40', border: '#4a90d9', label: 'Thường' },
              { color: '#2d1e40', border: '#a855f7', label: 'VIP'    },
              { color: '#401e2d', border: '#f43f5e', label: 'Đôi'    },
              { color: '#2a2a2a', border: '#555',    label: 'Đang được giữ' },
              { color: '#7c5e10', border: '#ffd700', label: 'Đang giữ bởi bạn' },
              { color: '#1a1a1a', border: '#333',    label: 'Đã bán' },
              { color: '#450a0a', border: '#ef4444', label: 'Bảo trì' },
            ].map(({ color, border, label }) => (
              <Stack key={label} direction="horizontal" gap={2}>
                <div style={{
                  width: 16, height: 16, borderRadius: 3,
                  background: color, border: `1.5px solid ${border}`,
                }} />
                <small style={{ color: '#aaa' }}>{label}</small>
              </Stack>
            ))}
          </Stack>

          <Card
            className="border-0"
            style={{
              position: 'sticky', bottom: 16,
              background: '#1a1a1a',
              boxShadow: '0 -4px 20px rgba(0,0,0,0.5)',
            }}
          >
            <Card.Body>
              <Row className="align-items-center g-2">
                <Col>
                  {lockData ? (
                    <>
                      <small style={{ color: '#aaa' }}>
                        {lockData.lockedShowtimeSeatIds.length} ghế đang được giữ
                      </small>
                      <div className="fw-bold" style={{ fontSize: 20, color: '#ffd700' }}>
                        {vnd(lockData.totalPrice)}
                      </div>
                    </>
                  ) : (
                    <>
                      <small style={{ color: '#aaa' }}>
                        {selected.size > 0
                          ? `Đã chọn ${selected.size} ghế`
                          : 'Chưa chọn ghế nào'}
                      </small>
                      {selected.size > 0 && (
                        <div style={{ color: 'var(--on-surface)' }}>
                          <div className="fw-bold" style={{ fontSize: 18 }}>
                            Tạm tính tiền vé: {vnd(estimatedTotal)}
                          </div>
                          <div style={{ color: '#94a3b8', fontSize: 12 }}>
                            {seats
                              .filter((seat) => selected.has(seat.seatId))
                              .map((seat) => `${seatLabel(seat)} - ${seat.seatTypeLabel || seat.seatType}: ${vnd(seatPrice(seat))}`)
                              .join(', ')}
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </Col>
                <Col xs="auto">
                  {lockData ? (
                    <Stack direction="horizontal" gap={2}>
                      <Button variant="outline-light" onClick={handleReset}>
                        Chọn lại
                      </Button>
                      <Button variant="warning" onClick={goToPayment} className="fw-bold">
                        Thanh toán →
                      </Button>
                    </Stack>
                  ) : (
                    <Button
                      variant="primary"
                      disabled={selected.size === 0 || submitting}
                      onClick={handleConfirm}
                      className="fw-bold px-4"
                    >
                      {submitting ? (
                        <>
                          <Spinner size="sm" animation="border" className="me-2" />
                          Đang giữ ghế...
                        </>
                      ) : 'Xác nhận ghế'}
                    </Button>
                  )}
                </Col>
              </Row>
            </Card.Body>
          </Card>
        </Container>
      </div>
    </>
  );
}
