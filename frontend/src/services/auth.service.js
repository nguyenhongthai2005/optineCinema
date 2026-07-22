import axios from 'axios';
import { API_BASE_URL } from './api';

const API_URL = `${API_BASE_URL}/auth/`;

const login = async (identifier, password) => {
  const response = await axios.post(API_URL + 'login', {
    identifier,
    password,
  });

  if (response.data.token) {
    localStorage.setItem('user', JSON.stringify(response.data));
  }

  return response.data;
};

const register = async (fullName, email, phone, password) => {
  return axios.post(API_URL + 'register', {
    fullName,
    email,
    phone,
    password,
  });
};

const logout = () => {
  localStorage.removeItem('user');
};

const getCurrentUser = () => {
  return JSON.parse(localStorage.getItem('user'));
};

const getGoogleLoginUrl = () => `${API_BASE_URL}/oauth2/authorization/google`;

const forgotPassword = async (email) => {
  return axios.post(API_URL + 'forgot-password', { email });
};

const resetPassword = async (token, newPassword) => {
  return axios.post(API_URL + 'reset-password', { token, newPassword });
};

const completeGoogleLogin = async (token) => {
  const response = await axios.get(`${API_BASE_URL}/auth/me`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const user = { ...response.data, token, type: 'Bearer' };
  localStorage.setItem('user', JSON.stringify(user));
  return user;
};

const authService = {
  login,
  register,
  logout,
  getCurrentUser,
  getGoogleLoginUrl,
  completeGoogleLogin,
  forgotPassword,
  resetPassword,
};

export default authService;
