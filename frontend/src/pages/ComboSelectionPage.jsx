import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Col, Container, Row, Spinner } from 'react-bootstrap';
import AppNavbar from '../components/AppNavbar';
import comboService from '../services/combo.service';

export default function ComboSelectionPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const bookingState = location.state || {};
  const [combos, setCombos] = useState([]);
  const [quantities, setQuantities] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    comboService.getActiveCombos()
      .then((res) => setCombos(res.data || []))
      .catch(() => setError('Không tải được danh sách bắp nước. Bạn vẫn có thể bỏ qua bước này.'))
      .finally(() => setLoading(false));
  }, []);

  const selectedCombos = useMemo(() => combos
    .map((combo) => ({ ...combo, quantity: Number(quantities[combo.id] || 0) }))
    .filter((combo) => combo.quantity > 0), [combos, quantities]);

  const comboTotal = selectedCombos.reduce((sum, combo) => sum + Number(combo.price || 0) * combo.quantity, 0);
  const ticketTotal = Number(bookingState.totalPrice || 0);
  const grandTotal = ticketTotal + comboTotal;

  if (!bookingState.lockedShowtimeSeatIds?.length) {
    return (
      <>
        <AppNavbar />
        <Container className="py-5 mt-5 text-center text-white">
          <h3>Không tìm thấy thông tin ghế đã giữ</h3>
          <Button onClick={() => navigate('/showtimes')} className="mt-3">Quay lại lịch chiếu</Button>
        </Container>
      </>
    );
  }

  const changeQty = (id, nextQty) => {
    setQuantities((prev) => ({ ...prev, [id]: Math.max(0, Number(nextQty) || 0) }));
  };

  const continueToConfirm = () => {
    navigate('/booking/confirm', {
      state: {
        ...bookingState,
        combos: selectedCombos.map((combo) => ({ comboId: combo.id, quantity: combo.quantity })),
        selectedCombos,
        comboTotal,
        grandTotal,
      },
    });
  };

  const vnd = (value) => Number(value || 0).toLocaleString('vi-VN') + ' VND';

  return (
    <>
      <AppNavbar />
      <div style={{ minHeight: '100vh', background: 'var(--surface)', paddingTop: 80 }}>
        <Container className="py-4">
          <div className="d-flex justify-content-between align-items-end gap-3 mb-4 flex-wrap">
            <div>
              <h2 style={{ color: 'var(--on-surface)', fontWeight: 800, margin: 0 }}>Chọn bắp nước</h2>
              <div style={{ color: '#94a3b8', marginTop: 6 }}>Thêm combo cho buổi xem phim hoặc bỏ qua để thanh toán vé.</div>
            </div>
            <Button variant="outline-light" onClick={continueToConfirm}>Bỏ qua</Button>
          </div>

          {error && <Alert variant="warning">{error}</Alert>}
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" variant="light" /></div>
          ) : (
            <Row className="g-3">
              {combos.map((combo) => (
                <Col md={6} lg={4} key={combo.id}>
                  <Card style={{ background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: 8, height: '100%' }}>
                    {combo.imageUrl && <Card.Img variant="top" src={combo.imageUrl} style={{ height: 150, objectFit: 'cover' }} />}
                    <Card.Body className="d-flex flex-column">
                      <div className="d-flex justify-content-between gap-2">
                        <h5 style={{ color: 'var(--on-surface)', fontWeight: 800 }}>{combo.name}</h5>
                        <strong style={{ color: '#22c55e', whiteSpace: 'nowrap' }}>{vnd(combo.price)}</strong>
                      </div>
                      <p style={{ color: '#94a3b8', minHeight: 44 }}>{combo.description}</p>
                      <div className="d-flex align-items-center gap-2 mt-auto">
                        <Button variant="outline-light" size="sm" onClick={() => changeQty(combo.id, Number(quantities[combo.id] || 0) - 1)}>-</Button>
                        <input
                          value={quantities[combo.id] || 0}
                          onChange={(e) => changeQty(combo.id, e.target.value)}
                          style={{ width: 56, textAlign: 'center', background: '#111827', color: '#fff', border: '1px solid rgba(255,255,255,0.16)', borderRadius: 6, padding: '5px 6px' }}
                        />
                        <Button variant="outline-light" size="sm" onClick={() => changeQty(combo.id, Number(quantities[combo.id] || 0) + 1)}>+</Button>
                      </div>
                    </Card.Body>
                  </Card>
                </Col>
              ))}
            </Row>
          )}

          <div style={{ position: 'sticky', bottom: 0, marginTop: 24, background: '#0f172a', border: '1px solid rgba(255,255,255,0.12)', borderRadius: 8, padding: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <div style={{ color: '#cbd5e1' }}>
              Vé: <strong>{vnd(ticketTotal)}</strong> · Combo: <strong>{vnd(comboTotal)}</strong>
              <div style={{ color: '#22c55e', fontWeight: 800, fontSize: 20 }}>Tổng: {vnd(grandTotal)}</div>
            </div>
            <Button onClick={continueToConfirm} style={{ background: '#16a34a', border: 'none', fontWeight: 800 }}>Tiếp tục thanh toán</Button>
          </div>
        </Container>
      </div>
    </>
  );
}
