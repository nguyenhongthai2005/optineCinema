import axios from 'axios';
import { API_BASE_URL, authHeader } from './api';

const API_URL = `${API_BASE_URL}/admin`;

const request = (config) => axios({ ...config, headers: { ...authHeader(), ...(config.headers || {}) } });

const adminService = {
  getStaff: (keyword, status, position, contractType) => request({ url: `${API_URL}/staff`, params: { keyword, status, position, contractType } }),
  getStaffById: (id) => request({ url: `${API_URL}/staff/${id}` }),
  getStaffAvailability: (id) => request({ url: `${API_URL}/staff/${id}/availability` }),
  searchStaffAvailability: (params) => request({ url: `${API_URL}/staff/availability`, params }),
  getNextStaffUsername: () => request({ url: `${API_URL}/staff/next-username` }),
  createStaff: (payload) => request({ method: 'post', url: `${API_URL}/staff`, data: payload }),
  updateStaff: (id, payload) => request({ method: 'put', url: `${API_URL}/staff/${id}`, data: payload }),
  updateStaffStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/staff/${id}/status`, data: { status } }),
  resetStaffPassword: (id) => request({ method: 'post', url: `${API_URL}/staff/${id}/reset-password` }),
  revokeStaff: (id) => request({ method: 'post', url: `${API_URL}/staff/${id}/revoke` }),
  getStaffAssignments: (params) => request({ url: `${API_URL}/staff-assignments`, params }),
  getStaffAssignmentSuggestions: (params) => request({ url: `${API_URL}/staff-assignments/suggestions`, params }),
  createStaffAssignment: (payload) => request({ method: 'post', url: `${API_URL}/staff-assignments`, data: payload }),
  updateStaffAssignment: (id, payload) => request({ method: 'put', url: `${API_URL}/staff-assignments/${id}`, data: payload }),
  cancelStaffAssignment: (id) => request({ method: 'patch', url: `${API_URL}/staff-assignments/${id}/cancel` }),

  getAttendance: (params) => request({ url: `${API_URL}/attendance`, params }),
  getAttendanceSummary: (params) => request({ url: `${API_URL}/attendance/monthly-summary`, params }),
  createAttendance: (payload) => request({ method: 'post', url: `${API_URL}/attendance`, data: payload }),
  updateAttendance: (id, payload) => request({ method: 'put', url: `${API_URL}/attendance/${id}`, data: payload }),
  deleteAttendance: (id) => request({ method: 'delete', url: `${API_URL}/attendance/${id}` }),

  getCustomers: (keyword) => request({ url: `${API_URL}/customers`, params: { keyword } }),
  getCustomerDetail: (id) => request({ url: `${API_URL}/customers/${id}` }),
  getCustomerBookings: (id) => request({ url: `${API_URL}/customers/${id}/bookings` }),
  updateCustomerStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/customers/${id}/status`, data: { status } }),
  recalculateCustomerSpending: () => request({ method: 'post', url: `${API_URL}/customers/recalculate-spending` }),

  verifyTicket: (qrCode) => request({ url: `${API_URL}/tickets/verify`, params: { qrCode } }),
  checkInTicket: (qrCode) => request({ method: 'post', url: `${API_URL}/tickets/check-in`, data: { qrCode } }),
  getPendingPayments: () => request({ url: `${API_URL}/payments/pending` }),
  confirmVietQrPayment: (bookingId) => request({ method: 'post', url: `${API_URL}/payments/${bookingId}/confirm-vietqr` }),
  rejectVietQrPayment: (bookingId) => request({ method: 'post', url: `${API_URL}/payments/${bookingId}/reject-vietqr` }),
  getAnalyticsOverview: (params) => request({ url: `${API_URL}/analytics/overview`, params }),
  getBookingReport: (params) => request({ url: `${API_URL}/reports/bookings`, params }),
  exportBookingReport: (params) => request({ url: `${API_URL}/reports/bookings/export`, params, responseType: 'blob' }),
  exportRevenueReport: (params) => request({ url: `${API_URL}/reports/revenue/export`, params, responseType: 'blob' }),

  // ── Movies ─────────────────────────────────────────────────────
  getMovies: (params) => request({ url: `${API_URL}/movies`, params }),
  getMovieById: (id) => request({ url: `${API_URL}/movies/${id}` }),
  createMovie: (payload) => request({ method: 'post', url: `${API_URL}/movies`, data: payload }),
  updateMovie: (id, payload) => request({ method: 'put', url: `${API_URL}/movies/${id}`, data: payload }),
  updateMovieStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/movies/${id}/status`, data: { status } }),
  deleteMovie: (id) => request({ method: 'delete', url: `${API_URL}/movies/${id}` }),

  // ── Combos ─────────────────────────────────────────────────────
  getCombos: (params) => request({ url: `${API_URL}/combos`, params }),
  getComboById: (id) => request({ url: `${API_URL}/combos/${id}` }),
  createCombo: (payload) => request({ method: 'post', url: `${API_URL}/combos`, data: payload }),
  updateCombo: (id, payload) => request({ method: 'put', url: `${API_URL}/combos/${id}`, data: payload }),
  updateComboStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/combos/${id}/status`, data: { status } }),
  deleteCombo: (id) => request({ method: 'delete', url: `${API_URL}/combos/${id}` }),

  // ── Rooms ──────────────────────────────────────────────────────
  getRooms: (keyword) => request({ url: `${API_URL}/rooms`, params: keyword ? { keyword } : {} }),
  getRoomById: (id) => request({ url: `${API_URL}/rooms/${id}` }),
  createRoom: (payload) => request({ method: 'post', url: `${API_URL}/rooms`, data: payload }),
  updateRoom: (id, payload) => request({ method: 'put', url: `${API_URL}/rooms/${id}`, data: payload }),
  updateRoomStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/rooms/${id}/status`, data: { status } }),
  generateRoomSeats: (id, rowCount, columnCount) => request({ method: 'post', url: `${API_URL}/rooms/${id}/generate-seats`, data: { rowCount, columnCount } }),

  // ── Seats ──────────────────────────────────────────────────────
  getSeatsByRoom: (roomId) => request({ url: `${API_URL}/rooms/${roomId}/seats` }),
  getSeatById: (id) => request({ url: `${API_URL}/seats/${id}` }),
  createSeat: (roomId, payload) => request({ method: 'post', url: `${API_URL}/rooms/${roomId}/seats`, data: payload }),
  updateSeat: (id, payload) => request({ method: 'put', url: `${API_URL}/seats/${id}`, data: payload }),
  updateSeatStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/seats/${id}/status`, data: { status } }),
  updateSeatType: (id, seatType) => request({ method: 'patch', url: `${API_URL}/seats/${id}/type`, data: { seatType } }),
  updateSeatTypes: (seatIds, seatType) => request({ method: 'patch', url: `${API_URL}/seats/bulk/type`, data: { seatIds, seatType } }),
  updateSeatMaintenance: (id, maintenance) => request({ method: 'patch', url: `${API_URL}/seats/${id}/maintenance`, data: { maintenance } }),
  updateSeatMaintenanceBulk: (seatIds, maintenance) => request({ method: 'patch', url: `${API_URL}/seats/bulk/maintenance`, data: { seatIds, maintenance } }),
  deleteSeat: (id) => request({ method: 'delete', url: `${API_URL}/seats/${id}` }),

  // ── Showtimes ──────────────────────────────────────────────────
  getShowtimes: (params) => request({ url: `${API_URL}/showtimes`, params }),
  getShowtimeById: (id) => request({ url: `${API_URL}/showtimes/${id}` }),
  createShowtime: (payload) => request({ method: 'post', url: `${API_URL}/showtimes`, data: payload }),
  updateShowtime: (id, payload) => request({ method: 'put', url: `${API_URL}/showtimes/${id}`, data: payload }),
  updateShowtimeStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/showtimes/${id}/status`, data: { status } }),

  // ── Auto Schedule ───────────────────────────────────────────────
  generateSchedulePlan: (payload) => request({ method: 'post', url: `${API_URL}/schedule-plans/generate`, data: payload }),
  getSchedulePlans: () => request({ url: `${API_URL}/schedule-plans` }),
  getSchedulePlan: (id) => request({ url: `${API_URL}/schedule-plans/${id}` }),
  applySchedulePlan: (id) => request({ method: 'post', url: `${API_URL}/schedule-plans/${id}/apply` }),
  cancelSchedulePlan: (id) => request({ method: 'post', url: `${API_URL}/schedule-plans/${id}/cancel` }),

  // ── Timeslots ──────────────────────────────────────────────────
  getTimeslots: () => request({ url: `${API_URL}/timeslots` }),
  getTimeslotById: (id) => request({ url: `${API_URL}/timeslots/${id}` }),
  createTimeslot: (payload) => request({ method: 'post', url: `${API_URL}/timeslots`, data: payload }),
  updateTimeslot: (id, payload) => request({ method: 'put', url: `${API_URL}/timeslots/${id}`, data: payload }),
  updateTimeslotStatus: (id, status) => request({ method: 'patch', url: `${API_URL}/timeslots/${id}/status`, data: { status } }),
};

export default adminService;
