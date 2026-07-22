import axios from 'axios';
import { API_BASE_URL } from './api';

const comboService = {
  getActiveCombos: () => axios.get(`${API_BASE_URL}/combos/active`),
};

export default comboService;
