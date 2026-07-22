export const page = { padding: 24, fontFamily: 'Inter, system-ui, sans-serif', maxWidth: 1440, margin: '0 auto', width: '100%', minWidth: 0 };
export const title = { margin: 0, fontSize: 28, fontWeight: 850, color: 'var(--color-text)' };
export const subtitle = { margin: '6px 0 0', color: 'var(--color-text-muted)', fontSize: 14 };
export const section = { background: 'var(--color-surface)', border: '1px solid var(--color-border)', borderRadius: 8, padding: 18, boxShadow: 'var(--shadow-card)', minWidth: 0 };
export const input = { background: 'var(--surface-container-low)', border: '1px solid var(--color-border)', borderRadius: 8, color: 'var(--color-text)', padding: '10px 11px', fontSize: 14, width: '100%' };
export const primaryButton = { border: 'none', borderRadius: 8, padding: '10px 14px', background: 'var(--color-primary)', color: 'var(--on-secondary-container)', fontWeight: 800, cursor: 'pointer' };
export const darkButton = { ...primaryButton, background: 'var(--surface-container-high)', color: 'var(--color-text)' };
export const greenButton = { ...primaryButton, background: '#16a34a' };
export const dangerButton = { ...primaryButton, background: '#dc2626' };
export const mutedButton = { border: '1px solid var(--color-border)', borderRadius: 8, padding: '10px 14px', background: 'var(--surface-container-high)', color: 'var(--color-text)', fontWeight: 800, cursor: 'pointer' };
export const table = { width: '100%', borderCollapse: 'collapse', minWidth: 900 };
export const th = { padding: '12px 14px', textAlign: 'left', fontSize: 12, color: 'var(--color-text-muted)', background: 'var(--surface-container-high)', textTransform: 'uppercase', letterSpacing: 0 };
export const td = { padding: '13px 14px', borderTop: '1px solid var(--color-border)', color: 'var(--color-text)', fontSize: 14, verticalAlign: 'top' };

export const vnd = (value) => `${Number(value || 0).toLocaleString('vi-VN')} VND`;
export const fmtDateTime = (value) => value ? new Date(value).toLocaleString('vi-VN') : '-';
export const fmtDate = (value) => value ? new Date(value).toLocaleDateString('vi-VN') : '-';
export const minutes = (value) => `${Number(value || 0)} phút`;

export const statusLabel = (status) => ({
  PENDING_PAYMENT: 'Chờ thanh toán',
  WAITING_CONFIRMATION: 'Chờ xác nhận',
  CONFIRMED: 'Đã thanh toán',
  CANCELLED: 'Đã hủy',
  VALID: 'Hợp lệ',
  USED: 'Đã sử dụng',
  NOT_CHECKED_IN: 'Chưa chấm công',
  CHECKED_IN: 'Đã check-in',
  ON_TIME: 'Đúng giờ',
  LATE: 'Đi trễ',
  COMPLETED: 'Hoàn thành ca',
  EARLY_LEAVE: 'Về sớm',
  ABSENT: 'Vắng mặt',
  MANUAL_ADJUSTED: 'Đã chỉnh sửa thủ công',
  PAID: 'Đã thanh toán',
  PENDING: 'Chờ thanh toán',
  FAILED: 'Thất bại',
}[status] || status || '-');
