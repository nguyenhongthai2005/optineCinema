export const normalizeShowtimeSeat = (rawSeat = {}) => {
  const rowLabel = rawSeat.rowLabel || rawSeat.row || '';
  const columnNumber = rawSeat.columnNumber ?? rawSeat.column ?? '';
  const maintenance = Boolean(rawSeat.maintenance || rawSeat.isMaintenance || rawSeat.status === 'MAINTENANCE');
  const status = maintenance ? 'MAINTENANCE' : (rawSeat.displayStatus || rawSeat.status || 'AVAILABLE');
  const seatLabel = rawSeat.seatLabel || `${rowLabel}${columnNumber}`;

  return {
    ...rawSeat,
    rowLabel,
    columnNumber,
    seatLabel,
    status,
    statusLabel: rawSeat.statusLabel || statusLabel(status),
    displayStatus: rawSeat.displayStatus || status,
    maintenance,
    lockedByCurrentUser: Boolean(rawSeat.lockedByCurrentUser),
    finalPrice: rawSeat.finalPrice ?? rawSeat.price ?? rawSeat.basePrice ?? 0,
  };
};

export const getSeatDisplayStatus = (seat = {}) => {
  if (seat.maintenance || seat.isMaintenance || seat.status === 'MAINTENANCE') return 'MAINTENANCE';
  return seat.displayStatus || seat.status || 'AVAILABLE';
};

export const isSeatSelectableForCounter = (seat = {}, lockData = null) => {
  const status = getSeatDisplayStatus(seat);
  const lockedByCurrentStaff = seat.lockedByCurrentUser || lockData?.lockedShowtimeSeatIds?.includes(seat.showtimeSeatId);
  return status === 'AVAILABLE' || (status === 'LOCKED' && lockedByCurrentStaff);
};

const statusLabel = (status) => {
  if (status === 'MAINTENANCE') return 'Bảo trì';
  if (status === 'BOOKED') return 'Đã bán';
  if (status === 'LOCKED') return 'Đang được giữ';
  if (status === 'INACTIVE') return 'Ngưng sử dụng';
  return 'Có thể chọn';
};
