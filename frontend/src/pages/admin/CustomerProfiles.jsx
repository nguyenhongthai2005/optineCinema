import React, { useEffect, useState } from 'react';
import AdminLayout from './AdminLayout';
import adminService from '../../services/admin.service';

export default function CustomerProfiles() {
  const [customers, setCustomers] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [selected, setSelected] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadCustomers = async () => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const res = await adminService.getCustomers(keyword);
      setCustomers(res.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải danh sách khách hàng.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadCustomers(); }, []);

  const openDetail = async (customer) => {
    const [detailRes, bookingRes] = await Promise.all([
      adminService.getCustomerDetail(customer.id),
      adminService.getCustomerBookings(customer.id),
    ]);
    setSelected(detailRes.data);
    setBookings(bookingRes.data);
  };

  const changeStatus = async (customer) => {
    await adminService.updateCustomerStatus(customer.id, customer.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE');
    loadCustomers();
  };

  const syncSpending = async () => {
    setSyncing(true);
    setError('');
    setMessage('');
    try {
      const res = await adminService.recalculateCustomerSpending();
      setMessage(res.data?.message || 'Đã đồng bộ chi tiêu.');
      await loadCustomers();
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể đồng bộ chi tiêu.');
    } finally {
      setSyncing(false);
    }
  };

  return (
    <AdminLayout title="Hồ sơ khách hàng">
      <section className="admin-toolbar">
        <div className="admin-search">
          <span className="material-symbols-outlined">search</span>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && loadCustomers()} placeholder="Tìm theo tên, email, số điện thoại" />
        </div>
        <button className="admin-button secondary" onClick={loadCustomers}>Tìm kiếm</button>
        <button className="admin-button" onClick={syncSpending} disabled={syncing}>
          {syncing ? 'Đang đồng bộ...' : 'Đồng bộ chi tiêu'}
        </button>
      </section>

      {error && <div className="admin-alert">{error}</div>}
      {message && <div className="admin-alert success">{message}</div>}
      {loading ? <div className="admin-empty">Loading customers...</div> : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Bookings</th><th>Total Spent</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {customers.map((customer) => (
                <tr key={customer.id}>
                  <td>{customer.fullName}</td>
                  <td>{customer.email}</td>
                  <td>{customer.phone}</td>
                  <td>{customer.totalBookings}</td>
                  <td>{Number(customer.totalSpent || 0).toLocaleString()} VND</td>
                  <td><span className={`status-pill ${customer.status?.toLowerCase()}`}>{customer.status}</span></td>
                  <td className="admin-actions">
                    <button onClick={() => openDetail(customer)} title="View"><span className="material-symbols-outlined">visibility</span></button>
                    <button onClick={() => changeStatus(customer)} title="Lock or unlock"><span className="material-symbols-outlined">{customer.status === 'ACTIVE' ? 'lock' : 'lock_open'}</span></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {selected && (
        <div className="admin-modal-backdrop" onClick={() => setSelected(null)}>
          <div className="admin-modal wide" onClick={(e) => e.stopPropagation()}>
            <button className="admin-modal-close" onClick={() => setSelected(null)}><span className="material-symbols-outlined">close</span></button>
            <h2>{selected.profile.fullName}</h2>
            <div className="detail-grid">
              <p>Email: {selected.profile.email}</p>
              <p>Phone: {selected.profile.phone}</p>
              <p>Status: {selected.profile.status}</p>
              <p>Membership: {selected.membershipName || 'None'}</p>
              <p>Total bookings: {selected.profile.totalBookings}</p>
              <p>Total spent: {Number(selected.profile.totalSpent || 0).toLocaleString()} VND</p>
            </div>
            <h3>Booking History</h3>
            <div className="admin-table-wrap compact">
              <table className="admin-table">
                <thead><tr><th>Movie</th><th>Showtime</th><th>Seats</th><th>Total</th><th>Payment</th><th>Status</th></tr></thead>
                <tbody>
                  {bookings.map((booking) => (
                    <tr key={booking.id}>
                      <td>{booking.movie}</td>
                      <td>{booking.showtime?.replace('T', ' ')}</td>
                      <td>{booking.seats?.join(', ')}</td>
                      <td>{Number(booking.totalAmount || 0).toLocaleString()} VND</td>
                      <td>{booking.paymentStatus || '-'}</td>
                      <td>{booking.bookingStatus}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
