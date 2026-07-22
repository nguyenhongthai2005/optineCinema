import axios from 'axios';
import { API_BASE_URL, authHeader } from './api';

const showtimeService = {

  // Lấy danh sách suất chiếu
  getShowtimes: (movieId = null, date = null) => {
    const params = {};
    if (movieId) params.movieId = movieId;
    if (date)    params.date    = date;
    return axios.get(`${API_BASE_URL}/showtimes`, { params });
  },

  getShowtime: (showtimeId) =>
    axios.get(`${API_BASE_URL}/showtimes/${showtimeId}`),

  // Lấy sơ đồ ghế của 1 suất chiếu
  getSeats: (showtimeId) =>
    axios.get(`${API_BASE_URL}/showtimes/${showtimeId}/seats`, { headers: authHeader() }),

  // Lock ghế (cần đăng nhập)
  lockSeats: (showtimeId, seatIds) =>
    axios.post(
      `${API_BASE_URL}/showtimes/${showtimeId}/seats/lock`,
      { seatIds },
      { headers: authHeader() }
    ),

  // Huỷ lock
  releaseSeats: (showtimeId) =>
    axios.delete(
      `${API_BASE_URL}/showtimes/${showtimeId}/seats/lock`,
      { headers: authHeader() }
    ),
};

export default showtimeService;
