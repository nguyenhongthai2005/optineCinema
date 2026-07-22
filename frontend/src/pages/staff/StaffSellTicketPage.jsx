import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import staffService from '../../services/staff.service';
import showtimeService from '../../services/showtime.service';
import { getSeatDisplayStatus, isSeatSelectableForCounter, normalizeShowtimeSeat } from '../../utils/showtimeSeat';
import { darkButton, fmtDateTime, greenButton, input, mutedButton, page, primaryButton, section, subtitle, title, vnd } from './staffUi';
import { useToast } from '../../context/ToastContext';

export default function StaffSellTicketPage() {
  const [params] = useSearchParams();
  const [showtimes, setShowtimes] = useState([]);
  const [showtimeId, setShowtimeId] = useState(params.get('showtimeId') || '');
  const [seats, setSeats] = useState([]);
  const [combos, setCombos] = useState([]);
  const [comboQty, setComboQty] = useState({});
  const [selected, setSelected] = useState(new Set());
  const [lockData, setLockData] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [customerInfo, setCustomerInfo] = useState({ fullName: '', phone: '', email: '' });
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [seatLoading, setSeatLoading] = useState(false);
  const toast = useToast();

  useEffect(() => {
    staffService.getTodayShowtimes().then((res) => setShowtimes(res.data || [])).catch(() => toast.error('Không tải được suất chiếu hôm nay.'));
    staffService.getActiveCombos().then((res) => setCombos(res.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    if (!showtimeId) return;
    setSelected(new Set());
    setLockData(null);
    setResult(null);
    loadSeatMap(showtimeId);
  }, [showtimeId]);

  const loadSeatMap = async (targetShowtimeId) => {
    setSeatLoading(true);
    setError('');
    try {
      const seatsRes = await showtimeService.getSeats(targetShowtimeId);
      const nextSeats = (seatsRes.data || []).map(normalizeShowtimeSeat);
      setSeats(nextSeats);
      const ownLocked = nextSeats.filter((seat) => seat.status === 'LOCKED' && seat.lockedByCurrentUser);
      if (ownLocked.length > 0) {
        setSelected(new Set(ownLocked.map((seat) => seat.seatId)));
        setLockData({
          lockedShowtimeSeatIds: ownLocked.map((seat) => seat.showtimeSeatId),
          expiredAt: ownLocked.map((seat) => seat.lockExpiresAt).filter(Boolean).sort()[0],
          totalPrice: ownLocked.reduce((sum, seat) => sum + Number(seat.finalPrice || seat.basePrice || 0), 0),
          showtimeId: targetShowtimeId,
        });
      }
    } catch (err) {
      const text = err.response?.data?.message || err.response?.data || 'Không thể tải sơ đồ ghế.';
      const nextError = String(text).includes('Suất chiếu') ? 'Suất chiếu không còn khả dụng để bán vé.' : text;
      setError(nextError);
      toast.error(nextError);
      setSeats([]);
    } finally {
      setSeatLoading(false);
    }
  };

  const selectedSeats = useMemo(() => seats.filter((seat) => selected.has(seat.seatId)), [seats, selected]);
  const total = selectedSeats.reduce((sum, seat) => sum + Number(seat.finalPrice || seat.basePrice || 0), 0);
  const selectedCombos = useMemo(() => combos
    .map((combo) => ({ ...combo, quantity: Number(comboQty[combo.id] || 0) }))
    .filter((combo) => combo.quantity > 0), [combos, comboQty]);
  const comboTotal = selectedCombos.reduce((sum, combo) => sum + Number(combo.price || 0) * combo.quantity, 0);
  const ticketTotal = Number(lockData?.totalPrice || total || 0);
  const grandTotal = ticketTotal + comboTotal;
  const rows = seats.reduce((acc, seat) => {
    acc[seat.rowLabel] = acc[seat.rowLabel] || [];
    acc[seat.rowLabel].push(seat);
    return acc;
  }, {});

  const toggleSeat = (seat) => {
    if (lockData) return;
    if (getSeatDisplayStatus(seat) === 'MAINTENANCE') {
      setError('Ghế đang bảo trì, vui lòng chọn ghế khác.');
      return;
    }
    if (!isSeatSelectableForCounter(seat, lockData)) return;
    setSelected((prev) => {
      const next = new Set(prev);
      next.has(seat.seatId) ? next.delete(seat.seatId) : next.add(seat.seatId);
      return next;
    });
  };

  const lockSeats = async () => {
    if (!showtimeId || selected.size === 0) {
      toast.warning('Vui lòng chọn suất chiếu và ít nhất một ghế.');
      return;
    }
    if (loading) return;
    setLoading(true);
    setError('');
    try {
      const res = await showtimeService.lockSeats(showtimeId, Array.from(selected));
      setLockData(res.data);
      toast.success('Đã giữ ghế! Vui lòng hoàn tất thanh toán trong thời gian giữ.');
      await loadSeatMap(showtimeId);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Không giữ được ghế.';
      setError(typeof msg === 'string' ? msg : 'Không giữ được ghế.');
      toast.error(typeof msg === 'string' ? msg : 'Không giữ được ghế.');
    } finally {
      setLoading(false);
    }
  };

  const reset = async () => {
    if (showtimeId) {
      try { await showtimeService.releaseSeats(showtimeId); } catch {}
      await loadSeatMap(showtimeId);
    }
    setSelected(new Set());
    setLockData(null);
    setResult(null);
    setError('');
  };

  const confirm = async () => {
    if (!lockData?.lockedShowtimeSeatIds?.length) {
      toast.warning('Vui lòng giữ ghế trước khi tạo đơn.');
      return;
    }
    if (loading) return;
    setLoading(true);
    setError('');
    try {
      const payload = {
        showtimeId: Number(showtimeId),
        lockedShowtimeSeatIds: lockData.lockedShowtimeSeatIds,
        customerInfo,
        paymentMethod,
        combos: selectedCombos.map((combo) => ({ comboId: combo.id, quantity: combo.quantity })),
      };
      const res = await staffService.createCounterBooking(payload);
      setResult(res.data);
      toast.success(res.data?.message || 'Đã tạo đơn bán vé thành công!');
      await loadSeatMap(showtimeId);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || 'Không tạo được đơn bán vé.';
      setError(typeof msg === 'string' ? msg : 'Không tạo được đơn bán vé.');
      toast.error(typeof msg === 'string' ? msg : 'Không tạo được đơn bán vé.');
    } finally {
      setLoading(false);
    }
  };

  const printTicket = () => window.print();
  const createNewOrder = async () => {
    setComboQty({});
    setCustomerInfo({ fullName: '', phone: '', email: '' });
    setPaymentMethod('CASH');
    await reset();
  };
  const changeComboQty = (id, qty) => setComboQty((prev) => ({ ...prev, [id]: Math.max(0, Number(qty) || 0) }));
  const printableTickets = result?.tickets || [];

  return (
    <div style={page}>
      <style>{printStyles}</style>
      <h1 style={title}>Bán vé tại quầy</h1>
      <p style={subtitle}>Chọn suất chiếu, giữ ghế realtime, nhập khách hàng và xác nhận thanh toán.</p>
      {error && <div style={danger}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(360px, 1fr) 360px', gap: 16, marginTop: 18, alignItems: 'start' }}>
        <section style={section}>
          <label style={label}>Suất chiếu</label>
          <select value={showtimeId} onChange={(e) => setShowtimeId(e.target.value)} style={input}>
            <option value="">Chọn suất chiếu</option>
            {showtimes.map((item) => <option key={item.showtimeId} value={item.showtimeId}>{item.movieTitle} · {item.room} · {fmtDateTime(item.startTime)}</option>)}
          </select>

          <div style={{ marginTop: 18, textAlign: 'center', color: 'var(--color-text-muted)', fontWeight: 800 }}>MÀN HÌNH</div>
          <div style={{ marginTop: 12, overflowX: 'auto' }}>
            {seatLoading && <div style={emptyState}>Đang tải sơ đồ ghế...</div>}
            {!seatLoading && showtimeId && seats.length === 0 && <div style={emptyState}>Không thể tải sơ đồ ghế.</div>}
            {!seatLoading && Object.keys(rows).sort().map((row) => (
              <div key={row} style={{ display: 'flex', gap: 6, alignItems: 'center', justifyContent: 'center', marginBottom: 7 }}>
                <span style={{ width: 22, color: 'var(--color-text-muted)', fontWeight: 800 }}>{row}</span>
                {rows[row].sort((a, b) => a.columnNumber - b.columnNumber).map((seat) => {
                  const active = selected.has(seat.seatId);
                  const lockedByMe = lockData?.lockedShowtimeSeatIds?.includes(seat.showtimeSeatId);
                  const status = getSeatDisplayStatus(seat);
                  const maintenance = status === 'MAINTENANCE';
                  const disabled = !isSeatSelectableForCounter(seat, lockData) && !lockedByMe;
                  const background = maintenance
                    ? '#7f1d1d'
                    : lockedByMe
                      ? '#f59e0b'
                      : active
                        ? '#2563eb'
                        : disabled
                          ? 'var(--surface-container-high)'
                          : 'var(--surface-container-low)';
                  return (
                    <button
                      key={seat.showtimeSeatId}
                      onClick={() => toggleSeat(seat)}
                      disabled={disabled || !!lockData}
                      title={`${seat.seatLabel} · ${seat.statusLabel || status}`}
                      style={{
                        width: 34,
                        height: 34,
                        borderRadius: 6,
                        border: maintenance ? '1px solid #ef4444' : '1px solid var(--color-border)',
                        background,
                        color: maintenance ? '#fecaca' : lockedByMe || active ? 'white' : disabled ? 'var(--color-text-muted)' : 'var(--color-text)',
                        fontWeight: 800,
                        cursor: disabled || lockData ? 'default' : 'pointer',
                      }}
                    >
                      {seat.columnNumber}
                    </button>
                  );
                })}
              </div>
            ))}
          </div>
        </section>

        <aside style={section}>
          <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Thông tin đơn</h2>
          <div style={{ color: 'var(--color-text-muted)', fontSize: 14 }}>Ghế: <strong style={{ color: 'var(--color-text)' }}>{selectedSeats.map((s) => `${s.rowLabel}${s.columnNumber}`).join(', ') || '-'}</strong></div>
          <div style={{ marginTop: 6, color: 'var(--color-text-muted)', fontSize: 14 }}>Tổng vé: <strong style={{ color: 'var(--color-text)' }}>{vnd(ticketTotal)}</strong></div>
          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button onClick={lockSeats} disabled={loading || !!lockData} style={primaryButton}>Giữ ghế</button>
            <button onClick={reset} disabled={loading} style={mutedButton}>Chọn lại</button>
          </div>

          <hr style={{ border: 0, borderTop: '1px solid var(--color-border)', margin: '18px 0' }} />
          <h2 style={{ margin: '0 0 8px', fontSize: 16 }}>Combo bắp nước</h2>
          <div style={{ display: 'grid', gap: 8, maxHeight: 220, overflowY: 'auto', paddingRight: 4 }}>
            {combos.map((combo) => (
              <div key={combo.id} style={{ display: 'grid', gridTemplateColumns: '1fr 92px', gap: 8, alignItems: 'center', padding: 8, border: '1px solid var(--color-border)', borderRadius: 8 }}>
                <div>
                  <strong>{combo.name}</strong>
                  <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{vnd(combo.price)}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <button style={{ ...mutedButton, padding: '5px 8px' }} onClick={() => changeComboQty(combo.id, Number(comboQty[combo.id] || 0) - 1)}>-</button>
                  <input style={{ ...input, padding: 6, textAlign: 'center' }} value={comboQty[combo.id] || 0} onChange={(e) => changeComboQty(combo.id, e.target.value)} />
                  <button style={{ ...mutedButton, padding: '5px 8px' }} onClick={() => changeComboQty(combo.id, Number(comboQty[combo.id] || 0) + 1)}>+</button>
                </div>
              </div>
            ))}
          </div>
          <div style={{ marginTop: 8, color: 'var(--color-text-muted)', fontSize: 14 }}>Tổng combo: <strong style={{ color: 'var(--color-text)' }}>{vnd(comboTotal)}</strong></div>
          <div style={{ marginTop: 4, color: '#22c55e', fontSize: 18, fontWeight: 850 }}>Tổng đơn: {vnd(grandTotal)}</div>

          <hr style={{ border: 0, borderTop: '1px solid var(--color-border)', margin: '18px 0' }} />
          <label style={label}>Tên khách</label>
          <input style={input} value={customerInfo.fullName} onChange={(e) => setCustomerInfo({ ...customerInfo, fullName: e.target.value })} placeholder="Khách vãng lai" />
          <label style={label}>Số điện thoại</label>
          <input style={input} value={customerInfo.phone} onChange={(e) => setCustomerInfo({ ...customerInfo, phone: e.target.value })} placeholder="0900000000" />
          <label style={label}>Email nhận vé</label>
          <input style={input} value={customerInfo.email} onChange={(e) => setCustomerInfo({ ...customerInfo, email: e.target.value })} placeholder="customer@example.com" />

          <label style={label}>Phương thức thanh toán</label>
          <select style={input} value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
            <option value="CASH">Tiền mặt</option>
            <option value="VIETQR">VietQR</option>
            <option value="OFFLINE">Offline</option>
          </select>

          <button onClick={confirm} disabled={loading || !lockData} style={{ ...greenButton, width: '100%', marginTop: 14 }}>Xác nhận thanh toán</button>

          {result && printableTickets.length === 0 && (
            <div style={{ marginTop: 16, background: 'var(--surface-container-low)', border: '1px solid var(--color-border)', color: 'var(--color-text)', borderRadius: 8, padding: 12 }}>
              <strong>Booking #{result.bookingId}</strong>
              <div>Tổng tiền: {vnd(result.totalAmount)}</div>
              {selectedCombos.length > 0 && <div>Combo: {selectedCombos.map((item) => `${item.name} x${item.quantity}`).join(', ')}</div>}
              <div>Trạng thái: {result.paymentStatus}</div>
              {result.ticketIds?.length > 0 && <div>Vé: {result.ticketIds.join(', ')}</div>}
              {result.vietQr?.qrImageUrl && <img src={result.vietQr.qrImageUrl} alt="VietQR" style={{ width: '100%', marginTop: 10, borderRadius: 8 }} />}
              {result.vietQr?.transferContent && <div style={{ fontFamily: 'monospace', fontWeight: 800 }}>Nội dung CK: {result.vietQr.transferContent}</div>}
            </div>
          )}
        </aside>
      </div>

      {result && printableTickets.length > 0 && (
        <div className="counter-ticket-modal-backdrop" style={modalBackdrop}>
          <div className="counter-ticket-modal" style={modal}>
            <div className="counter-ticket-print-actions" style={modalHeader}>
              <div>
                <h2 style={{ margin: 0, fontSize: 22 }}>Bán vé thành công</h2>
                <p style={{ margin: '6px 0 0', color: 'var(--color-text-muted)', fontSize: 14 }}>Vé đã sẵn sàng để in và soát bằng mã QR.</p>
              </div>
              <button onClick={() => setResult(null)} style={iconButton} aria-label="Đóng">
                <span className="material-symbols-outlined" style={{ fontSize: 20 }}>close</span>
              </button>
            </div>

            <PrintableCounterTickets result={result} tickets={printableTickets} />

            <div className="counter-ticket-print-actions" style={modalActions}>
              <button onClick={printTicket} style={primaryButton}>In vé</button>
              <button onClick={createNewOrder} style={greenButton}>Tạo đơn mới</button>
              <button onClick={() => setResult(null)} style={mutedButton}>Đóng</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function PrintableCounterTickets({ result, tickets }) {
  const seats = result.seats?.join(', ') || tickets.map((ticket) => ticket.seatLabel).join(', ');
  return (
    <div className="counter-ticket-print-area" style={printArea}>
      <section style={receiptSummary}>
        <div style={{ color: '#111827', fontSize: 12, fontWeight: 900, letterSpacing: 1.2, textTransform: 'uppercase' }}>Opticine Cinema</div>
        <h3 style={{ margin: '6px 0 12px', color: '#111827', fontSize: 20 }}>Bán vé thành công</h3>
        <SummaryLine label="Mã đơn" value={result.bookingCode || `BK-${result.bookingId}`} />
        <SummaryLine label="Phim" value={result.movieTitle} />
        <SummaryLine label="Suất chiếu" value={fmtDateTime(result.showtimeStart)} />
        <SummaryLine label="Phòng" value={result.roomName} />
        <SummaryLine label="Ghế" value={seats} />
        <SummaryLine label="Tổng tiền" value={vnd(result.finalAmount || result.totalAmount)} />
        <SummaryLine label="Phương thức thanh toán" value={paymentLabel(result.paymentMethod)} />
      </section>

      <div style={ticketGrid}>
        {tickets.map((ticket) => (
          <article key={ticket.ticketId || ticket.id} style={ticketCard}>
            <div style={{ textAlign: 'center', borderBottom: '1px dashed #cbd5e1', paddingBottom: 10, marginBottom: 12 }}>
              <div style={{ color: '#111827', fontSize: 12, fontWeight: 900, letterSpacing: 1.4 }}>OPTICINE CINEMA</div>
              <h3 style={{ margin: '6px 0 0', color: '#111827', fontSize: 18 }}>VÉ XEM PHIM</h3>
            </div>

            <div style={{ display: 'grid', gap: 7, color: '#111827', fontSize: 13 }}>
              <SummaryLine label="Phim" value={ticket.movieTitle || result.movieTitle} />
              <SummaryLine label="Suất chiếu" value={fmtDateTime(ticket.startTime || result.showtimeStart)} />
              <SummaryLine label="Phòng" value={ticket.roomName || result.roomName} />
              <SummaryLine label="Ghế" value={ticket.seatLabel} strong />
              <SummaryLine label="Mã vé" value={ticket.ticketCode || ticket.qrCode} />
              <SummaryLine label="Thanh toán" value={paymentLabel(ticket.paymentMethod || result.paymentMethod)} />
              <SummaryLine label="Tổng tiền" value={vnd(ticket.price)} />
            </div>

            <div style={qrBox}>
              <QRCodeSVG value={ticket.qrCode || ticket.qrPayload || 'invalid'} size={136} level="Q" includeMargin />
            </div>
            <div style={qrText}>{ticket.qrCode || ticket.qrPayload || 'N/A'}</div>
            <p style={{ margin: '12px 0 0', color: '#475569', fontSize: 12, textAlign: 'center' }}>Vui lòng đưa mã QR cho nhân viên soát vé khi vào rạp.</p>
          </article>
        ))}
      </div>
    </div>
  );
}

function SummaryLine({ label, value, strong }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
      <span style={{ color: '#64748b' }}>{label}</span>
      <strong style={{ color: '#111827', textAlign: 'right', fontWeight: strong ? 900 : 750 }}>{value || '-'}</strong>
    </div>
  );
}

const paymentLabel = (method) => {
  if (method === 'CASH') return 'Tiền mặt';
  if (method === 'VIETQR') return 'VietQR';
  if (method === 'OFFLINE') return 'Offline';
  return method || '-';
};

const label = { display: 'block', marginTop: 12, marginBottom: 6, color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 800 };
const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
const emptyState = { padding: 18, color: 'var(--color-text-muted)', fontWeight: 800, textAlign: 'center' };
const modalBackdrop = { position: 'fixed', inset: 0, zIndex: 900, background: 'rgba(2, 6, 23, 0.78)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 24 };
const modal = { width: 'min(980px, 100%)', maxHeight: '92vh', overflowY: 'auto', background: 'var(--surface-container)', color: 'var(--color-text)', border: '1px solid var(--color-border)', borderRadius: 8, boxShadow: '0 24px 80px rgba(0,0,0,0.45)' };
const modalHeader = { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 16, padding: 18, borderBottom: '1px solid var(--color-border)' };
const modalActions = { display: 'flex', justifyContent: 'flex-end', gap: 10, padding: 18, borderTop: '1px solid var(--color-border)', flexWrap: 'wrap' };
const iconButton = { width: 36, height: 36, borderRadius: 8, border: '1px solid var(--color-border)', background: 'var(--surface-container-high)', color: 'var(--color-text)', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' };
const printArea = { background: '#f8fafc', padding: 18, display: 'grid', gap: 16 };
const receiptSummary = { background: '#fff', border: '1px solid #dbe3ef', borderRadius: 8, padding: 16, display: 'grid', gap: 7 };
const ticketGrid = { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 14 };
const ticketCard = { background: '#fff', border: '1px solid #dbe3ef', borderRadius: 8, padding: 16, breakInside: 'avoid', pageBreakInside: 'avoid' };
const qrBox = { margin: '14px auto 8px', width: 164, height: 164, background: '#fff', border: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'center' };
const qrText = { color: '#475569', fontFamily: 'monospace', fontSize: 10, textAlign: 'center', wordBreak: 'break-all' };
const printStyles = `
@media print {
  @page { margin: 10mm; }
  body * { visibility: hidden !important; }
  .counter-ticket-print-area,
  .counter-ticket-print-area * { visibility: visible !important; }
  .counter-ticket-modal-backdrop {
    position: static !important;
    inset: auto !important;
    display: block !important;
    padding: 0 !important;
    background: #fff !important;
  }
  .counter-ticket-modal {
    width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    border: 0 !important;
    box-shadow: none !important;
    background: #fff !important;
  }
  .counter-ticket-print-actions { display: none !important; }
  .counter-ticket-print-area {
    position: absolute !important;
    left: 0 !important;
    top: 0 !important;
    width: 100% !important;
    padding: 0 !important;
    background: #fff !important;
  }
}
`;
