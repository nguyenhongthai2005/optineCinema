import axios from 'axios';
import { API_BASE_URL, authHeader } from './api';

const ADMIN_URL = `${API_BASE_URL}/admin/promotions`;
const PUBLIC_URL = `${API_BASE_URL}/promotions`;

const request = (config) =>
  axios({ ...config, headers: { ...authHeader(), ...(config.headers || {}) } });

const promotionService = {
  // ── Customer: validate coupon ───────────────────────────
  validateCoupon: (code, ticketAmount, comboAmount) =>
    request({
      method: 'post',
      url: `${PUBLIC_URL}/validate`,
      data: { code, ticketAmount, comboAmount },
    }),

  getAvailable: (ticketAmount, comboAmount) =>
    request({
      url: `${PUBLIC_URL}/available`,
      params: { ticketAmount, comboAmount },
    }),

  // ── Admin CRUD ──────────────────────────────────────────
  getAll: () => request({ url: ADMIN_URL }),

  getById: (id) => request({ url: `${ADMIN_URL}/${id}` }),

  create: (payload) =>
    request({ method: 'post', url: ADMIN_URL, data: payload }),

  update: (id, payload) =>
    request({ method: 'put', url: `${ADMIN_URL}/${id}`, data: payload }),

  updateStatus: (id, status) =>
    request({ method: 'patch', url: `${ADMIN_URL}/${id}/status`, data: { status } }),

  delete: (id) => request({ method: 'delete', url: `${ADMIN_URL}/${id}` }),
};

export default promotionService;
