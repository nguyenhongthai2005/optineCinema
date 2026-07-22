import { Routes, Route, Navigate } from 'react-router-dom'

// Pages — Home
import Home from './pages/Home/Home'

// Pages — Authentication
import Login from './pages/Authentication/Login'
import Register from './pages/Authentication/Register'
import OAuth2Success from './pages/Authentication/OAuth2Success'
import ForgotPassword from './pages/Authentication/ForgotPassword'
import ResetPassword from './pages/Authentication/ResetPassword'

// Pages — Showtimes
import ShowtimePage from './pages/Showtimes/ShowtimePage'
import SeatMapPage from './pages/Showtimes/SeatMapPage'

// Pages — Booking
import TicketSuccess from './pages/TicketSuccess'
import TestBooking from './pages/TestBooking'
import PaymentResult from './pages/PaymentResult'
import VietQrPaymentPage from './pages/VietQrPaymentPage'
import BookingConfirmPage from './pages/BookingConfirmPage'
import ComboSelectionPage from './pages/ComboSelectionPage'
import MyBookings from './pages/MyBookings'
import CustomerProfilePage from './pages/CustomerProfilePage'

// Protected Route
import ProtectedRoute from './components/ProtectedRoute'
import PublicRoute from './components/PublicRoute'

// Admin
import AdminLayout from './layouts/AdminLayout'
import AdminDashboard from './pages/admin/AdminDashboard'
import TheaterManagement from './pages/admin/TheaterManagement'
import MovieManagement from './pages/admin/MovieManagement'
import ComboManagement from './pages/admin/ComboManagement'
import SeatManagement from './pages/admin/SeatManagement'
import ShowtimeManagement from './pages/admin/ShowtimeManagement'
import StaffManagement from './pages/admin/StaffManagement'
import AttendanceManagement from './pages/admin/AttendanceManagement'
import CustomerProfiles from './pages/admin/CustomerProfiles'
import TicketCheckIn from './pages/admin/TicketCheckIn'
import PendingVietQrPayments from './pages/admin/PendingVietQrPayments'
import AutoSchedule from './pages/admin/AutoSchedule'
import StaffAssignments from './pages/admin/StaffAssignments'
import PromotionManagement from './pages/admin/PromotionManagement'
import BookingRevenueReport from './pages/admin/BookingRevenueReport'

// Staff
import StaffLayout from './layouts/StaffLayout'
import StaffDashboard from './pages/staff/StaffDashboard'
import StaffSellTicketPage from './pages/staff/StaffSellTicketPage'
import StaffComboSalesPage from './pages/staff/StaffComboSalesPage'
import StaffTicketCheckInPage from './pages/staff/StaffTicketCheckInPage'
import StaffShowtimesPage from './pages/staff/StaffShowtimesPage'
import StaffAttendancePage from './pages/staff/StaffAttendancePage'
import StaffPendingPaymentsPage from './pages/staff/StaffPendingPaymentsPage'
import StaffOrdersPage from './pages/staff/StaffOrdersPage'
import StaffProfilePage from './pages/staff/StaffProfilePage'
import StaffAvailabilityPage from './pages/staff/StaffAvailabilityPage'
import StaffMySchedulePage from './pages/staff/StaffMySchedulePage'

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<PublicRoute><Home /></PublicRoute>} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/oauth2/success" element={<OAuth2Success />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* Booking routes */}
      <Route path="/booking/success" element={<PublicRoute><TicketSuccess /></PublicRoute>} />
      <Route path="/test-booking" element={<PublicRoute><TestBooking /></PublicRoute>} />
      <Route path="/payment-result" element={<PublicRoute><PaymentResult /></PublicRoute>} />

      <Route
        path="/payment/vietqr"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <VietQrPaymentPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/booking/confirm"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <BookingConfirmPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/booking/combos"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <ComboSelectionPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/my-bookings"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <MyBookings />
          </ProtectedRoute>
        }
      />

      <Route
        path="/profile"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <CustomerProfilePage />
          </ProtectedRoute>
        }
      />

      {/* Showtime routes */}
      <Route path="/showtimes" element={<PublicRoute><ShowtimePage /></PublicRoute>} />

      <Route
        path="/showtimes/:id/seats"
        element={
          <ProtectedRoute roles={['ROLE_CUSTOMER']}>
            <SeatMapPage />
          </ProtectedRoute>
        }
      />

      {/* Admin routes */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminDashboard />} />
        <Route path="movies" element={<MovieManagement />} />
        <Route path="combos" element={<ComboManagement />} />
        <Route path="theaters" element={<TheaterManagement />} />
        <Route path="seats" element={<SeatManagement />} />
        <Route path="showtimes" element={<ShowtimeManagement />} />

        <Route
          path="auto-schedule"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
              <AutoSchedule />
            </ProtectedRoute>
          }
        />

        <Route
          path="staff"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
              <StaffManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="staff-assignments"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
              <StaffAssignments />
            </ProtectedRoute>
          }
        />

        <Route path="attendance" element={<AttendanceManagement />} />
        <Route path="customers" element={<CustomerProfiles />} />

        <Route
          path="payments/pending"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_STAFF']}>
              <PendingVietQrPayments />
            </ProtectedRoute>
          }
        />

        <Route path="ticket-checkin" element={<TicketCheckIn />} />

        <Route
          path="promotions"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
              <PromotionManagement />
            </ProtectedRoute>
          }
        />

        <Route
          path="reports"
          element={
            <ProtectedRoute roles={['ROLE_ADMIN', 'ROLE_MANAGER']}>
              <BookingRevenueReport />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Staff routes */}
      <Route
        path="/staff"
        element={
          <ProtectedRoute roles={['ROLE_STAFF', 'ROLE_ADMIN']}>
            <StaffLayout />
          </ProtectedRoute>
        }
      >
        <Route
          index
          element={<Navigate to="/staff/dashboard" replace />}
        />

        <Route path="dashboard" element={<StaffDashboard />} />

        <Route
          path="sell-ticket"
          element={
            <ProtectedRoute
              roles={['ROLE_STAFF', 'ROLE_ADMIN']}
              positions={['COUNTER_SALES']}
            >
              <StaffSellTicketPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="combo-sales"
          element={
            <ProtectedRoute
              roles={['ROLE_STAFF', 'ROLE_ADMIN']}
              positions={['COUNTER_SALES']}
            >
              <StaffComboSalesPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="check-in"
          element={
            <ProtectedRoute
              roles={['ROLE_STAFF', 'ROLE_ADMIN']}
              positions={['TICKET_CHECKER']}
            >
              <StaffTicketCheckInPage />
            </ProtectedRoute>
          }
        />

        <Route path="showtimes" element={<StaffShowtimesPage />} />
        <Route path="attendance" element={<StaffAttendancePage />} />
        <Route path="availability" element={<StaffAvailabilityPage />} />
        <Route path="my-schedule" element={<StaffMySchedulePage />} />

        <Route
          path="payments/pending"
          element={
            <ProtectedRoute
              roles={['ROLE_STAFF', 'ROLE_ADMIN']}
              positions={['COUNTER_SALES']}
            >
              <StaffPendingPaymentsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="orders"
          element={
            <ProtectedRoute
              roles={['ROLE_STAFF', 'ROLE_ADMIN']}
              positions={['COUNTER_SALES']}
            >
              <StaffOrdersPage />
            </ProtectedRoute>
          }
        />

        <Route path="profile" element={<StaffProfilePage />} />
      </Route>
    </Routes>
  )
}

export default App