import axios from 'axios';
import { API_BASE_URL } from './api';

const movieService = {
  getNowShowing: () => axios.get(`${API_BASE_URL}/movies/now-showing`),
  getComingSoon: () => axios.get(`${API_BASE_URL}/movies/coming-soon`),
  getHotMovies: () => axios.get(`${API_BASE_URL}/movies/hot`),
  getMovie: (id) => axios.get(`${API_BASE_URL}/movies/${id}`),
};

export default movieService;
