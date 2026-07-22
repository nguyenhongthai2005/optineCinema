import React, { useEffect, useState, useCallback } from 'react';
import promotionService from '../../services/promotion.service';

// ── helpers ──────────────────────────────────────────────────────────────────
const vnd = (n) =>
  n != null
    ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(n)
    : '—';
const fmtDate = (v) => (v ? new Date(v).toLocaleDateString('vi-VN') : '—');
const fmtDateLocal = (v) => {
  if (!v) return '';
  // convert ISO → yyyy-MM-ddTHH:mm for datetime-local input
  return new Date(v).toISOString().slice(0, 16);
};

const STATUS_COLORS = { ACTIVE: '#22c55e', INACTIVE: '#94a3b8', EXPIRED: '#ef4444' };
const STATUS_LABELS = { ACTIVE: 'Đang hoạt động', INACTIVE: 'Tắt', EXPIRED: 'Hết hạn' };
const APPLICABLE_LABELS = { ALL: 'Tất cả', TICKET: 'Chỉ vé', COMBO: 'Chỉ combo' };

const EMPTY_FORM = {
  code: '', discountType: 'PERCENT', discountValue: '',
  startDate: '', endDate: '', status: 'ACTIVE',
  maxUsage: '', maxUsagePerUser: '',
  applicableTo: 'ALL', maxDiscountAmount: '',
};

// ── Modal ─────────────────────────────────────────────────────────────────────
function PromotionModal({ open, onClose, onSave, initial }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => {
    if (open) {
      setErr(null);
      setForm(initial
        ? {
            ...initial,
            discountValue: initial.discountValue ?? '',
            maxUsage: initial.maxUsage ?? '',
            maxUsagePerUser: initial.maxUsagePerUser ?? '',
            maxDiscountAmount: initial.maxDiscountAmount ?? '',
            startDate: fmtDateLocal(initial.startDate),
            endDate: fmtDateLocal(initial.endDate),
          }
        : EMPTY_FORM
      );
    }
  }, [open, initial]);

  if (!open) return null;

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSave = async () => {
    if (!form.code.trim()) return setErr('Mã không được để trống.');
    if (!form.discountValue || Number(form.discountValue) <= 0)
      return setErr('Giá trị giảm phải lớn hơn 0.');
    setSaving(true);
    setErr(null);
    const payload = {
      ...form,
      code: form.code.trim().toUpperCase(),
      discountValue: Number(form.discountValue),
      maxUsage: form.maxUsage ? Number(form.maxUsage) : null,
      maxUsagePerUser: form.maxUsagePerUser ? Number(form.maxUsagePerUser) : null,
      maxDiscountAmount: form.maxDiscountAmount ? Number(form.maxDiscountAmount) : null,
      startDate: form.startDate ? new Date(form.startDate).toISOString() : null,
      endDate: form.endDate ? new Date(form.endDate).toISOString() : null,
    };
    try {
      await onSave(payload);
      onClose();
    } catch (e) {
      setErr(e.response?.data || e.message || 'Có lỗi xảy ra');
    } finally {
      setSaving(false);
    }
  };

  const inputStyle = {
    width: '100%', background: 'rgba(255,255,255,0.06)',
    border: '1px solid rgba(255,255,255,0.12)', color: '#e2e8f0',
    borderRadius: 8, padding: '8px 12px', outline: 'none', fontSize: 14,
  };
  const labelStyle = { display: 'block', color: '#94a3b8', fontSize: 12, marginBottom: 4, fontWeight: 600 };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 9999,
      background: 'rgba(0,0,0,0.75)', backdropFilter: 'blur(6px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
    }}>
      <div style={{
        background: 'linear-gradient(135deg,#1e1b4b,#1a1a2e)',
        border: '1px solid rgba(99,102,241,0.3)',
        borderRadius: 16, padding: 28, width: '100%', maxWidth: 520,
        boxShadow: '0 20px 60px rgba(0,0,0,0.6)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <h5 style={{ color: '#e2e8f0', margin: 0, fontWeight: 700 }}>
            {initial ? '✏️ Chỉnh sửa khuyến mãi' : '➕ Tạo mã khuyến mãi mới'}
          </h5>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#94a3b8', fontSize: 20, cursor: 'pointer' }}>✕</button>
        </div>

        {err && (
          <div style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid rgba(239,68,68,0.4)', borderRadius: 8, padding: '8px 12px', color: '#fca5a5', marginBottom: 12, fontSize: 13 }}>
            {err}
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div style={{ gridColumn: '1 / -1' }}>
            <label style={labelStyle}>Mã voucher *</label>
            <input style={{ ...inputStyle, textTransform: 'uppercase', letterSpacing: 2 }}
              value={form.code} onChange={(e) => set('code', e.target.value.toUpperCase())}
              placeholder="VD: SUMMER20" />
          </div>

          <div>
            <label style={labelStyle}>Loại giảm *</label>
            <select style={inputStyle} value={form.discountType} onChange={(e) => set('discountType', e.target.value)}>
              <option value="PERCENT">% Phần trăm</option>
              <option value="FIXED">₫ Cố định</option>
            </select>
          </div>
          <div>
            <label style={labelStyle}>{form.discountType === 'PERCENT' ? 'Phần trăm (%)' : 'Số tiền (VNĐ)'} *</label>
            <input style={inputStyle} type="number" min="0"
              value={form.discountValue} onChange={(e) => set('discountValue', e.target.value)}
              placeholder={form.discountType === 'PERCENT' ? '20' : '50000'} />
          </div>

          {form.discountType === 'PERCENT' && (
            <div style={{ gridColumn: '1 / -1' }}>
              <label style={labelStyle}>Giảm tối đa (VNĐ) – để trống nếu không giới hạn</label>
              <input style={inputStyle} type="number" min="0"
                value={form.maxDiscountAmount} onChange={(e) => set('maxDiscountAmount', e.target.value)}
                placeholder="100000" />
            </div>
          )}

          <div>
            <label style={labelStyle}>Áp dụng cho</label>
            <select style={inputStyle} value={form.applicableTo} onChange={(e) => set('applicableTo', e.target.value)}>
              <option value="ALL">Tất cả</option>
              <option value="TICKET">Chỉ vé</option>
              <option value="COMBO">Chỉ combo</option>
            </select>
          </div>
          <div>
            <label style={labelStyle}>Trạng thái</label>
            <select style={inputStyle} value={form.status} onChange={(e) => set('status', e.target.value)}>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Tắt</option>
            </select>
          </div>

          <div>
            <label style={labelStyle}>Ngày bắt đầu</label>
            <input style={inputStyle} type="datetime-local"
              value={form.startDate} onChange={(e) => set('startDate', e.target.value)} />
          </div>
          <div>
            <label style={labelStyle}>Ngày kết thúc</label>
            <input style={inputStyle} type="datetime-local"
              value={form.endDate} onChange={(e) => set('endDate', e.target.value)} />
          </div>

          <div>
            <label style={labelStyle}>Tổng lượt dùng tối đa</label>
            <input style={inputStyle} type="number" min="1"
              value={form.maxUsage} onChange={(e) => set('maxUsage', e.target.value)}
              placeholder="Không giới hạn" />
          </div>
          <div>
            <label style={labelStyle}>Lượt/user tối đa</label>
            <input style={inputStyle} type="number" min="1"
              value={form.maxUsagePerUser} onChange={(e) => set('maxUsagePerUser', e.target.value)}
              placeholder="Không giới hạn" />
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 20, justifyContent: 'flex-end' }}>
          <button onClick={onClose} disabled={saving} style={{
            background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.15)',
            color: '#94a3b8', borderRadius: 8, padding: '9px 20px', cursor: 'pointer',
          }}>
            Huỷ
          </button>
          <button onClick={handleSave} disabled={saving} style={{
            background: 'linear-gradient(90deg,#6366f1,#8b5cf6)', border: 'none',
            color: '#fff', borderRadius: 8, padding: '9px 24px', fontWeight: 700,
            cursor: saving ? 'not-allowed' : 'pointer', opacity: saving ? 0.7 : 1,
          }}>
            {saving ? 'Đang lưu...' : 'Lưu'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function PromotionManagement() {
  const [promotions, setPromotions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const res = await promotionService.getAll();
      setPromotions(res.data);
    } catch (e) {
      setErr(e.response?.data || e.message || 'Không tải được dữ liệu');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = promotions.filter((p) => {
    const matchSearch = !search || p.code.toUpperCase().includes(search.toUpperCase());
    const matchStatus = filterStatus === 'ALL' || p.status === filterStatus;
    return matchSearch && matchStatus;
  });

  const handleSave = async (payload) => {
    if (editItem) {
      await promotionService.update(editItem.id, payload);
    } else {
      await promotionService.create(payload);
    }
    await load();
  };

  const handleToggleStatus = async (promo) => {
    const next = promo.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await promotionService.updateStatus(promo.id, next);
      await load();
    } catch (e) {
      alert('Lỗi: ' + (e.response?.data || e.message));
    }
  };

  const handleDelete = async (id) => {
    try {
      await promotionService.delete(id);
      setConfirmDelete(null);
      await load();
    } catch (e) {
      alert('Lỗi xóa: ' + (e.response?.data || e.message));
    }
  };

  // ── stats ──
  const stats = {
    total: promotions.length,
    active: promotions.filter((p) => p.status === 'ACTIVE').length,
    inactive: promotions.filter((p) => p.status === 'INACTIVE').length,
    expired: promotions.filter((p) => p.status === 'EXPIRED').length,
  };

  return (
    <div style={{ padding: '24px 28px', background: 'var(--surface,#0f0f1a)', minHeight: '100vh', color: '#e2e8f0' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 28 }}>
        <div>
          <h2 style={{ margin: 0, fontWeight: 800, fontSize: 24, color: '#e2e8f0' }}>
            🎟️ Quản lý Khuyến Mãi
          </h2>
          <p style={{ color: '#64748b', margin: '4px 0 0', fontSize: 14 }}>
            Tạo và quản lý mã giảm giá cho khách hàng
          </p>
        </div>
        <button
          id="create-promotion-btn"
          onClick={() => { setEditItem(null); setModalOpen(true); }}
          style={{
            background: 'linear-gradient(90deg,#6366f1,#8b5cf6)',
            border: 'none', color: '#fff', borderRadius: 10,
            padding: '10px 20px', fontWeight: 700, cursor: 'pointer',
            fontSize: 14, display: 'flex', alignItems: 'center', gap: 6,
          }}
        >
          + Tạo mã mới
        </button>
      </div>

      {/* Stats cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gap: 14, marginBottom: 24 }}>
        {[
          { label: 'Tổng mã', value: stats.total, color: '#6366f1', icon: '🎟️' },
          { label: 'Đang hoạt động', value: stats.active, color: '#22c55e', icon: '✅' },
          { label: 'Đã tắt', value: stats.inactive, color: '#94a3b8', icon: '⏸️' },
          { label: 'Hết hạn', value: stats.expired, color: '#ef4444', icon: '⌛' },
        ].map((s) => (
          <div key={s.label} style={{
            background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 12, padding: '16px 20px', display: 'flex', alignItems: 'center', gap: 14,
          }}>
            <span style={{ fontSize: 28 }}>{s.icon}</span>
            <div>
              <div style={{ fontSize: 22, fontWeight: 800, color: s.color }}>{s.value}</div>
              <div style={{ fontSize: 12, color: '#64748b' }}>{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
        <input
          id="promotion-search"
          type="text"
          placeholder="Tìm mã khuyến mãi..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.12)',
            color: '#e2e8f0', borderRadius: 8, padding: '8px 14px', outline: 'none', fontSize: 14, minWidth: 220,
          }}
        />
        {['ALL', 'ACTIVE', 'INACTIVE', 'EXPIRED'].map((s) => (
          <button key={s} onClick={() => setFilterStatus(s)} style={{
            background: filterStatus === s ? 'rgba(99,102,241,0.2)' : 'rgba(255,255,255,0.04)',
            border: filterStatus === s ? '1px solid rgba(99,102,241,0.5)' : '1px solid rgba(255,255,255,0.1)',
            color: filterStatus === s ? '#a5b4fc' : '#94a3b8',
            borderRadius: 8, padding: '8px 16px', cursor: 'pointer', fontSize: 13, fontWeight: 600,
          }}>
            {s === 'ALL' ? 'Tất cả' : STATUS_LABELS[s]}
          </button>
        ))}
      </div>

      {err && (
        <div style={{ background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)', borderRadius: 10, padding: '10px 16px', color: '#fca5a5', marginBottom: 16 }}>
          {err}
        </div>
      )}

      {/* Table */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: 60, color: '#64748b' }}>Đang tải...</div>
      ) : filtered.length === 0 ? (
        <div style={{ textAlign: 'center', padding: 60, color: '#64748b' }}>
          <div style={{ fontSize: 40, marginBottom: 12 }}>🎟️</div>
          <div>Không có mã khuyến mãi nào</div>
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: '0 6px' }}>
            <thead>
              <tr style={{ color: '#64748b', fontSize: 12, textTransform: 'uppercase', letterSpacing: 1 }}>
                {['Mã code', 'Loại giảm', 'Giá trị', 'Áp dụng', 'Thời hạn', 'Lượt dùng', 'Trạng thái', 'Thao tác'].map((h) => (
                  <th key={h} style={{ padding: '8px 12px', textAlign: 'left', fontWeight: 700 }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {filtered.map((promo) => {
                const usagePercent = promo.maxUsage
                  ? Math.min(100, Math.round(((promo.currentUsage || 0) / promo.maxUsage) * 100))
                  : null;
                return (
                  <tr key={promo.id} style={{
                    background: 'rgba(255,255,255,0.03)',
                    borderRadius: 10, transition: 'background 0.15s',
                  }}>
                    <td style={{ padding: '12px 12px', borderRadius: '10px 0 0 10px' }}>
                      <span style={{
                        fontFamily: 'monospace', fontWeight: 800, fontSize: 15,
                        color: '#a5b4fc', letterSpacing: 2,
                      }}>
                        {promo.code}
                      </span>
                    </td>
                    <td style={{ padding: '12px 12px' }}>
                      <span style={{
                        background: promo.discountType === 'PERCENT' ? 'rgba(99,102,241,0.15)' : 'rgba(245,158,11,0.15)',
                        color: promo.discountType === 'PERCENT' ? '#a5b4fc' : '#fcd34d',
                        borderRadius: 6, padding: '3px 8px', fontSize: 12, fontWeight: 700,
                      }}>
                        {promo.discountType === 'PERCENT' ? '% Phần trăm' : '₫ Cố định'}
                      </span>
                    </td>
                    <td style={{ padding: '12px 12px', fontWeight: 700, color: '#e2e8f0' }}>
                      {promo.discountType === 'PERCENT'
                        ? `${promo.discountValue}%${promo.maxDiscountAmount ? ` (tối đa ${vnd(promo.maxDiscountAmount)})` : ''}`
                        : vnd(promo.discountValue)}
                    </td>
                    <td style={{ padding: '12px 12px', color: '#94a3b8', fontSize: 13 }}>
                      {APPLICABLE_LABELS[promo.applicableTo] || promo.applicableTo}
                    </td>
                    <td style={{ padding: '12px 12px', fontSize: 12, color: '#94a3b8' }}>
                      <div>{fmtDate(promo.startDate)}</div>
                      <div>→ {fmtDate(promo.endDate)}</div>
                    </td>
                    <td style={{ padding: '12px 12px', minWidth: 120 }}>
                      {promo.maxUsage ? (
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#94a3b8', marginBottom: 3 }}>
                            <span>{promo.currentUsage || 0}/{promo.maxUsage}</span>
                            <span>{usagePercent}%</span>
                          </div>
                          <div style={{ height: 5, background: 'rgba(255,255,255,0.1)', borderRadius: 99, overflow: 'hidden' }}>
                            <div style={{
                              width: `${usagePercent}%`, height: '100%', borderRadius: 99,
                              background: usagePercent >= 90 ? '#ef4444' : usagePercent >= 60 ? '#f59e0b' : '#22c55e',
                              transition: 'width 0.4s',
                            }} />
                          </div>
                        </div>
                      ) : (
                        <span style={{ color: '#64748b', fontSize: 12 }}>{promo.currentUsage || 0} / ∞</span>
                      )}
                    </td>
                    <td style={{ padding: '12px 12px' }}>
                      <button
                        onClick={() => handleToggleStatus(promo)}
                        disabled={promo.status === 'EXPIRED'}
                        title={promo.status === 'EXPIRED' ? 'Đã hết hạn' : 'Click để đổi trạng thái'}
                        style={{
                          background: 'none', border: 'none', cursor: promo.status === 'EXPIRED' ? 'default' : 'pointer',
                          padding: 0,
                        }}
                      >
                        <span style={{
                          display: 'inline-block',
                          background: `${STATUS_COLORS[promo.status]}20`,
                          color: STATUS_COLORS[promo.status],
                          border: `1px solid ${STATUS_COLORS[promo.status]}60`,
                          borderRadius: 20, padding: '3px 10px', fontSize: 12, fontWeight: 700,
                        }}>
                          {STATUS_LABELS[promo.status] || promo.status}
                        </span>
                      </button>
                    </td>
                    <td style={{ padding: '12px 12px', borderRadius: '0 10px 10px 0' }}>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          id={`edit-promotion-${promo.id}`}
                          onClick={() => { setEditItem(promo); setModalOpen(true); }}
                          title="Chỉnh sửa"
                          style={{
                            background: 'rgba(99,102,241,0.15)', border: '1px solid rgba(99,102,241,0.3)',
                            color: '#a5b4fc', borderRadius: 7, padding: '5px 10px', cursor: 'pointer', fontSize: 13,
                          }}
                        >
                          ✏️
                        </button>
                        <button
                          id={`delete-promotion-${promo.id}`}
                          onClick={() => setConfirmDelete(promo)}
                          title="Xóa"
                          style={{
                            background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.3)',
                            color: '#fca5a5', borderRadius: 7, padding: '5px 10px', cursor: 'pointer', fontSize: 13,
                          }}
                        >
                          🗑️
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Create/Edit Modal */}
      <PromotionModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSave}
        initial={editItem}
      />

      {/* Delete Confirm Modal */}
      {confirmDelete && (
        <div style={{
          position: 'fixed', inset: 0, zIndex: 9999,
          background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            background: '#1e1b4b', border: '1px solid rgba(239,68,68,0.4)',
            borderRadius: 14, padding: 28, maxWidth: 380, width: '90%',
            boxShadow: '0 10px 40px rgba(0,0,0,0.5)',
          }}>
            <h5 style={{ color: '#e2e8f0', marginBottom: 10 }}>⚠️ Xác nhận xóa</h5>
            <p style={{ color: '#94a3b8', fontSize: 14 }}>
              Bạn có chắc muốn xóa mã <strong style={{ color: '#a5b4fc' }}>{confirmDelete.code}</strong>? Hành động này không thể hoàn tác.
            </p>
            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 18 }}>
              <button onClick={() => setConfirmDelete(null)} style={{
                background: 'rgba(255,255,255,0.06)', border: '1px solid rgba(255,255,255,0.15)',
                color: '#94a3b8', borderRadius: 8, padding: '8px 18px', cursor: 'pointer',
              }}>Huỷ</button>
              <button onClick={() => handleDelete(confirmDelete.id)} style={{
                background: 'rgba(239,68,68,0.8)', border: 'none',
                color: '#fff', borderRadius: 8, padding: '8px 18px', fontWeight: 700, cursor: 'pointer',
              }}>Xóa</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
