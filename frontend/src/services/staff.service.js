import axios from 'axios';
import { API_BASE_URL, authHeader } from './api';

const API_URL = `${API_BASE_URL}/staff`;
const request = (config) => axios({ ...config, headers: { ...authHeader(), ...(config.headers || {}) } });

const staffService = {
  getDashboard: () => request({ url: `${API_URL}/dashboard/summary` }),
  getTodayShowtimes: () => request({ url: `${API_URL}/showtimes/today` }),
  getShowtimes: (date) => request({ url: `${API_URL}/showtimes`, params: date ? { date } : {} }),

  getAttendanceToday: () => request({ url: `${API_URL}/attendance/today` }),
  checkIn: (payload) => request({ method: 'post', url: `${API_URL}/attendance/check-in`, data: payload }),
  checkOut: (payload) => request({ method: 'post', url: `${API_URL}/attendance/check-out`, data: payload }),
  getAttendanceHistory: (params) => request({ url: `${API_URL}/attendance/my`, params }),
  getAvailability: () => request({ url: `${API_URL}/availability` }),
  createAvailability: (payload) => request({ method: 'post', url: `${API_URL}/availability`, data: payload }),
  updateAvailability: (id, payload) => request({ method: 'put', url: `${API_URL}/availability/${id}`, data: payload }),
  deleteAvailability: (id) => request({ method: 'delete', url: `${API_URL}/availability/${id}` }),
  getMyAssignments: (params) => request({ url: `${API_URL}/assignments/my`, params }),

  searchCustomers: (keyword) => request({ url: `${API_URL}/customers/search`, params: { keyword } }),
  quickCreateCustomer: (payload) => request({ method: 'post', url: `${API_URL}/customers/quick-create`, data: payload }),
  getActiveCombos: () => request({ url: `${API_URL}/combos/active` }),
  createCounterBooking: (payload) => request({ method: 'post', url: `${API_URL}/bookings/counter`, data: payload }),
  getOrders: (params) => request({ url: `${API_URL}/bookings`, params }),
  createFoodOrder: (payload) => request({ method: 'post', url: `${API_URL}/food-orders`, data: payload }),
  getFoodOrders: (params) => request({ url: `${API_URL}/food-orders`, params }),

  getPendingPayments: () => request({ url: `${API_URL}/payments/pending` }),
  confirmVietQrPayment: (bookingId) => request({ method: 'post', url: `${API_URL}/payments/${bookingId}/confirm-vietqr` }),
  rejectVietQrPayment: (bookingId) => request({ method: 'post', url: `${API_URL}/payments/${bookingId}/reject-vietqr` }),

  verifyTicket: (qrCode) => request({ url: `${API_URL}/tickets/verify`, params: { qrCode } }),
  checkInTicket: (qrCode) => request({ method: 'post', url: `${API_URL}/tickets/check-in`, data: { qrCode } }),

  getProfile: () => request({ url: `${API_URL}/profile` }),
  updateProfile: (payload) => request({ method: 'put', url: `${API_URL}/profile`, data: payload }),
  changePassword: (payload) => request({ method: 'put', url: `${API_URL}/profile/change-password`, data: payload }),
};

export default staffService;
