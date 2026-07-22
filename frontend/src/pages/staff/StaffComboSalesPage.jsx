import { useEffect, useMemo, useState } from 'react';
import staffService from '../../services/staff.service';
import { fmtDateTime, greenButton, input, mutedButton, page, section, subtitle, title, vnd } from './staffUi';

export default function StaffComboSalesPage() {
  const [combos, setCombos] = useState([]);
  const [quantities, setQuantities] = useState({});
  const [customer, setCustomer] = useState({ customerName: '', customerPhone: '' });
  const [orders, setOrders] = useState([]);
  const [result, setResult] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadCombos();
    loadOrders();
  }, []);

  const loadCombos = async () => {
    try {
      const res = await staffService.getActiveCombos();
      setCombos(res.data || []);
    } catch {
      setError('Không tải được combo đang bán.');
    }
  };

  const loadOrders = async () => {
    try {
      const res = await staffService.getFoodOrders();
      setOrders(res.data || []);
    } catch {
      setOrders([]);
    }
  };

  const selectedItems = useMemo(() => combos
    .map((combo) => ({ ...combo, quantity: Number(quantities[combo.id] || 0) }))
    .filter((combo) => combo.quantity > 0), [combos, quantities]);
  const total = selectedItems.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0);

  const changeQty = (id, qty) => setQuantities((prev) => ({ ...prev, [id]: Math.max(0, Number(qty) || 0) }));

  const submit = async () => {
    setMessage('');
    setError('');
    if (selectedItems.length === 0) {
      setError('Vui lòng chọn ít nhất một combo.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        ...customer,
        paymentMethod: 'CASH',
        combos: selectedItems.map((item) => ({ comboId: item.id, quantity: item.quantity })),
      };
      const res = await staffService.createFoodOrder(payload);
      setResult(res.data);
      setMessage('Đã bán combo thành công.');
      setQuantities({});
      loadOrders();
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data || 'Không tạo được đơn bắp nước.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={page}>
      <h1 style={title}>Bán bắp nước/combo</h1>
      <p style={subtitle}>Tạo đơn combo độc lập tại quầy, thanh toán tiền mặt.</p>
      {message && <div style={success}>{message}</div>}
      {error && <div style={danger}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 360px', gap: 16, marginTop: 18, alignItems: 'start' }}>
        <section style={section}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 }}>
            {combos.map((combo) => (
              <div key={combo.id} style={{ border: '1px solid var(--color-border)', borderRadius: 8, overflow: 'hidden', background: 'var(--surface-container-low)' }}>
                {combo.imageUrl && <img src={combo.imageUrl} alt="" style={{ width: '100%', height: 118, objectFit: 'cover' }} />}
                <div style={{ padding: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                    <strong>{combo.name}</strong>
                    <span style={{ color: '#22c55e', fontWeight: 800 }}>{vnd(combo.price)}</span>
                  </div>
                  <p style={{ minHeight: 38, color: 'var(--color-text-muted)', fontSize: 13 }}>{combo.description}</p>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <button style={mutedButton} onClick={() => changeQty(combo.id, Number(quantities[combo.id] || 0) - 1)}>-</button>
                    <input style={{ ...input, textAlign: 'center' }} value={quantities[combo.id] || 0} onChange={(e) => changeQty(combo.id, e.target.value)} />
                    <button style={mutedButton} onClick={() => changeQty(combo.id, Number(quantities[combo.id] || 0) + 1)}>+</button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        <aside style={section}>
          <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Đơn hiện tại</h2>
          <label style={label}>Tên khách</label>
          <input style={input} value={customer.customerName} onChange={(e) => setCustomer({ ...customer, customerName: e.target.value })} placeholder="Khách vãng lai" />
          <label style={label}>Số điện thoại</label>
          <input style={input} value={customer.customerPhone} onChange={(e) => setCustomer({ ...customer, customerPhone: e.target.value })} placeholder="0900000000" />
          <div style={{ marginTop: 14 }}>
            {selectedItems.length === 0 ? <div style={{ color: 'var(--color-text-muted)' }}>Chưa chọn combo.</div> : selectedItems.map((item) => (
              <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                <span>{item.name} x {item.quantity}</span>
                <strong>{vnd(Number(item.price || 0) * item.quantity)}</strong>
              </div>
            ))}
          </div>
          <hr style={{ border: 0, borderTop: '1px solid var(--color-border)', margin: '14px 0' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 18 }}>
            <strong>Tổng</strong>
            <strong style={{ color: '#22c55e' }}>{vnd(total)}</strong>
          </div>
          <button onClick={submit} disabled={loading} style={{ ...greenButton, width: '100%', marginTop: 14 }}>Thu tiền mặt</button>
          {result && (
            <div style={{ marginTop: 14, padding: 12, border: '1px solid var(--color-border)', borderRadius: 8 }}>
              <strong>Đơn #{result.orderId}</strong>
              <div>Tổng tiền: {vnd(result.totalAmount)}</div>
              <div>Trạng thái: {result.paymentStatus}</div>
            </div>
          )}
        </aside>
      </div>

      <section style={{ ...section, marginTop: 16 }}>
        <h2 style={{ margin: '0 0 12px', fontSize: 18 }}>Đơn bắp nước hôm nay</h2>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 780 }}>
            <thead><tr>{['Mã', 'Khách', 'Combo', 'Tổng', 'Thời gian'].map((head) => <th key={head} style={th}>{head}</th>)}</tr></thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.orderId}>
                  <td style={td}>#{order.orderId}</td>
                  <td style={td}>{order.customer || 'Khách vãng lai'}</td>
                  <td style={td}>{order.items?.map((item) => `${item.comboName} x${item.quantity}`).join(', ')}</td>
                  <td style={td}>{vnd(order.totalAmount)}</td>
                  <td style={td}>{fmtDateTime(order.createdAt)}</td>
                </tr>
              ))}
              {orders.length === 0 && <tr><td style={td} colSpan={5}>Chưa có đơn bắp nước hôm nay.</td></tr>}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

const label = { display: 'block', marginTop: 12, marginBottom: 6, color: 'var(--color-text-muted)', fontSize: 13, fontWeight: 800 };
const th = { padding: '12px 14px', textAlign: 'left', fontSize: 12, color: 'var(--color-text-muted)', background: 'var(--surface-container-high)', textTransform: 'uppercase', letterSpacing: 0 };
const td = { padding: '13px 14px', borderTop: '1px solid var(--color-border)', color: 'var(--color-text)', fontSize: 14, verticalAlign: 'top' };
const success = { marginTop: 14, background: 'rgba(22, 163, 74, 0.18)', color: '#bbf7d0', border: '1px solid rgba(34, 197, 94, 0.28)', padding: 12, borderRadius: 8 };
const danger = { marginTop: 14, background: 'rgba(185, 28, 28, 0.18)', color: '#fecaca', border: '1px solid rgba(248, 113, 113, 0.28)', padding: 12, borderRadius: 8 };
