// ── Helper ──────────────────────────────────────────
const today = new Date()
const fmtDate = (d) => d.toISOString().split('T')[0]

// ── Theater Management Mock Data ────────────────────
export const MOCK_THEATERS = [
  { id: 1, name: 'CGV Vincom Center',       address: '72 Lê Thánh Tôn, Q.1', city: 'TP.HCM',   phone: '028 3925 0000', rooms: 15, status: 'active' },
  { id: 2, name: 'Galaxy Nguyễn Du',         address: '116 Nguyễn Du, Q.1',  city: 'TP.HCM',   phone: '028 3822 4000', rooms: 12, status: 'active' },
  { id: 3, name: 'Lotte Cinema Nowzone',     address: '235 Nguyễn Văn Cừ, Q.5', city: 'TP.HCM', phone: '028 3923 3333', rooms: 10, status: 'active' },
  { id: 4, name: 'BHD Star Phạm Hùng',      address: '9 Phạm Hùng, Q.8',    city: 'TP.HCM',   phone: '028 6293 4000', rooms: 8,  status: 'inactive' },
  { id: 5, name: 'Mega GS Cao Thắng',       address: '271 Cao Thắng, Q.3',  city: 'TP.HCM',   phone: '028 3847 0000', rooms: 6,  status: 'active' },
]

// ── Seat Management Mock Data ───────────────────────
export const SEAT_THEATERS = [
  { id: 1, name: 'CGV Vincom Center' },
  { id: 2, name: 'Galaxy Nguyễn Du' },
  { id: 3, name: 'Lotte Cinema Nowzone' },
]

export const SEAT_ROOMS = {
  1: [{ id: 'R1', name: 'Phòng 1 - 2D', totalSeats: 80 }, { id: 'R2', name: 'Phòng 2 - 3D', totalSeats: 60 }],
  2: [{ id: 'R3', name: 'Phòng 1 - IMAX', totalSeats: 120 }],
  3: [{ id: 'R4', name: 'Phòng 1 - 4DX', totalSeats: 50 }],
}

export const SEAT_TYPES = {
  normal: { label: 'Thường',   color: '#3b82f6', bg: '#eff6ff', price: '70.000đ' },
  vip:    { label: 'VIP',      color: '#a855f7', bg: '#faf5ff', price: '100.000đ' },
  couple: { label: 'Đôi',      color: '#ec4899', bg: '#fdf2f8', price: '150.000đ' },
  empty:  { label: 'Trống',    color: '#e2e8f0', bg: '#f8fafc', price: '' },
}

// ── Showtime Management Mock Data ───────────────────
export const SHOWTIME_MOVIES = [
  'Avengers: Doomsday', 
  'Mission Impossible 8', 
  'Lilo & Stitch', 
  'Paddington in Peru', 
  'The Accountant 2'
]

export const SHOWTIME_THEATERS = [
  'CGV Vincom Center', 
  'Galaxy Nguyễn Du', 
  'Lotte Cinema Nowzone'
]

export const SHOWTIME_ROOMS = { 
  'CGV Vincom Center': ['Phòng 1 - 2D', 'Phòng 2 - 3D', 'Phòng 3 - IMAX'], 
  'Galaxy Nguyễn Du': ['Phòng 1 - 2D', 'Phòng 2 - 4DX'], 
  'Lotte Cinema Nowzone': ['Phòng 1 - 2D', 'Phòng 2 - VIP'] 
}

export const SHOWTIME_TYPES = ['2D', '3D', 'IMAX', '4DX', 'VIP']

export const MOCK_SHOWTIMES = [
  { id: 1, movie: 'Avengers: Doomsday',    theater: 'CGV Vincom Center',    room: 'Phòng 2 - 3D',   type: '3D',   date: fmtDate(today), startTime: '09:00', endTime: '11:30', price: 120000, status: 'active' },
  { id: 2, movie: 'Avengers: Doomsday',    theater: 'CGV Vincom Center',    room: 'Phòng 3 - IMAX', type: 'IMAX', date: fmtDate(today), startTime: '14:00', endTime: '16:30', price: 180000, status: 'active' },
  { id: 3, movie: 'Mission Impossible 8',  theater: 'Galaxy Nguyễn Du',     room: 'Phòng 1 - 2D',   type: '2D',   date: fmtDate(today), startTime: '10:30', endTime: '13:00', price: 90000,  status: 'active' },
  { id: 4, movie: 'Lilo & Stitch',         theater: 'CGV Vincom Center',    room: 'Phòng 1 - 2D',   type: '2D',   date: fmtDate(today), startTime: '19:00', endTime: '21:00', price: 90000,  status: 'cancelled' },
  { id: 5, movie: 'Paddington in Peru',    theater: 'Lotte Cinema Nowzone', room: 'Phòng 2 - VIP',  type: 'VIP',  date: fmtDate(today), startTime: '16:00', endTime: '18:00', price: 150000, status: 'active' },
  { id: 6, movie: 'The Accountant 2',      theater: 'Galaxy Nguyễn Du',     room: 'Phòng 2 - 4DX',  type: '4DX',  date: fmtDate(today), startTime: '20:30', endTime: '23:00', price: 160000, status: 'active' },
]
