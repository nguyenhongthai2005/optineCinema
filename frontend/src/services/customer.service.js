import axios from 'axios';
import { API_BASE_URL, authHeader } from './api';

const API_URL = `${API_BASE_URL}/customer`;

const getProfile = () =>
  axios.get(`${API_URL}/profile`, { headers: authHeader() });

const updateProfile = (data) =>
  axios.put(`${API_URL}/profile`, data, { headers: authHeader() });

const customerService = { getProfile, updateProfile };
export default customerService;
