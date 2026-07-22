import React from 'react';
import { QRCodeSVG } from 'qrcode.react';

/**
 * ETicket component - hiển thị vé điện tử.
 */
export default function ETicket({ ticket }) {
  const formatTime = (dateString) => {
    if (!dateString) return 'Chưa xác định';
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  const formatPrice = (price) => {
    if (!price) return '0';
    return Number(price).toLocaleString('vi-VN');
  };
  const hasBreakdown = ticket.finalAmount || ticket.ticketTotal || ticket.comboTotal || ticket.discountAmount;

  return (
    <div style={{
      background: 'linear-gradient(135deg, #1e293b 0%, #0f172a 100%)',
      borderRadius: '1rem',
      overflow: 'hidden',
      marginBottom: '1rem',
      border: '1px solid rgba(255,255,255,0.1)',
      maxWidth: '380px',
      width: '100%',
      margin: '0 auto 1rem',
    }}>
      {/* Header */}
      <div style={{
        background: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
        padding: '1rem 1.5rem',
        textAlign: 'center',
      }}>
        <h3 style={{
          margin: 0,
          color: '#fff',
          fontSize: '0.85rem',
          fontWeight: 700,
          letterSpacing: '3px',
          textTransform: 'uppercase',
        }}>
          OPTICINE E-TICKET
        </h3>
      </div>

      {/* Body */}
      <div style={{ padding: '1.5rem' }}>
        {/* Seat - Large display */}
        <div style={{ textAlign: 'center', marginBottom: '1.25rem' }}>
          <p style={{
            margin: 0,
            color: '#94a3b8',
            fontSize: '0.75rem',
            textTransform: 'uppercase',
            letterSpacing: '1px',
            marginBottom: '0.25rem',
          }}>Ghế</p>
          <p style={{
            margin: 0,
            color: '#f8fafc',
            fontSize: '2.5rem',
            fontWeight: 800,
            letterSpacing: '2px',
          }}>{ticket.seatLabel || 'N/A'}</p>
        </div>

        {/* Info grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '1rem',
          marginBottom: '1.25rem',
        }}>
          <div>
            <p style={{ margin: 0, color: '#94a3b8', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '1px' }}>
              Giờ chiếu
            </p>
            <p style={{ margin: '0.25rem 0 0', color: '#e2e8f0', fontSize: '0.9rem', fontWeight: 600 }}>
              {formatTime(ticket.startTime)}
            </p>
          </div>
          <div style={{ textAlign: 'right' }}>
            <p style={{ margin: 0, color: '#94a3b8', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '1px' }}>
              Giá vé
            </p>
            <p style={{ margin: '0.25rem 0 0', color: '#22c55e', fontSize: '0.9rem', fontWeight: 600 }}>
              {formatPrice(ticket.price)} VND
            </p>
          </div>
        </div>

        {/* Dashed separator with circle cutouts */}
        <div style={{ position: 'relative', margin: '1rem -1.5rem' }}>
          <div style={{
            borderTop: '2px dashed rgba(255,255,255,0.15)',
            width: '100%',
          }} />
          {/* Left circle */}
          <div style={{
            position: 'absolute', top: '-12px', left: '-12px',
            width: '24px', height: '24px',
            background: '#000', borderRadius: '50%',
          }} />
          {/* Right circle */}
          <div style={{
            position: 'absolute', top: '-12px', right: '-12px',
            width: '24px', height: '24px',
            background: '#000', borderRadius: '50%',
          }} />
        </div>

        {/* QR Code section */}
        {hasBreakdown && (
          <div style={{
            borderTop: '1px solid rgba(255,255,255,0.1)',
            paddingTop: '1rem',
            marginTop: '1rem',
            color: '#cbd5e1',
            fontSize: '0.82rem',
          }}>
            <Line label="Tiền vé" value={`${formatPrice(ticket.ticketTotal)} VND`} />
            {Number(ticket.comboTotal || 0) > 0 && <Line label="Bắp nước" value={`${formatPrice(ticket.comboTotal)} VND`} />}
            <Line label="Tạm tính" value={`${formatPrice(ticket.grossTotal || Number(ticket.ticketTotal || 0) + Number(ticket.comboTotal || 0))} VND`} />
            {Number(ticket.voucherDiscountAmount || 0) > 0 && <Line label={`Voucher ${ticket.promotionCode || ''}`} value={`-${formatPrice(ticket.voucherDiscountAmount)} VND`} positive />}
            {Number(ticket.membershipDiscountAmount || 0) > 0 && (
              <Line
                label={`Ưu đãi thành viên ${ticket.membershipTierName || ''} (${Number(ticket.membershipDiscountPercent || 0).toLocaleString('vi-VN')}%)`}
                value={`-${formatPrice(ticket.membershipDiscountAmount)} VND`}
                positive
              />
            )}
            {Number(ticket.discountAmount || 0) > 0 && <Line label="Tổng giảm giá" value={`-${formatPrice(ticket.discountAmount)} VND`} positive />}
            <Line label="Tổng thanh toán" value={`${formatPrice(ticket.finalAmount || ticket.grossTotal)} VND`} strong />
          </div>
        )}

        <div style={{
          textAlign: 'center',
          padding: '1.25rem 0 0.5rem',
        }}>
          <div style={{
            display: 'inline-flex',
            flexDirection: 'column',
            alignItems: 'center',
            background: '#fff',
            borderRadius: '0.75rem',
            padding: '1rem',
          }}>
            <QRCodeSVG value={ticket.qrCode || 'invalid'} size={132} level="Q" includeMargin={false} />
          </div>
          <p style={{
            margin: '0.75rem 0 0',
            color: '#64748b',
            fontSize: '0.7rem',
            fontFamily: 'monospace',
            wordBreak: 'break-all',
          }}>
            {ticket.qrCode || 'N/A'}
          </p>
          {ticket.status && (
            <p style={{ margin: '0.35rem 0 0', color: ticket.status === 'VALID' ? '#22c55e' : '#f97316', fontSize: '0.72rem', fontWeight: 700 }}>
              {ticket.status}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

function Line({ label, value, strong, positive }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 6 }}>
      <span style={{ color: '#94a3b8' }}>{label}</span>
      <strong style={{ color: positive ? '#22c55e' : strong ? '#f8fafc' : '#cbd5e1' }}>{value}</strong>
    </div>
  );
}
