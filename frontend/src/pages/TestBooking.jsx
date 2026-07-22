import React, { useState } from 'react';
import axios from 'axios';
import { API_BASE_URL, authHeader } from '../services/api';

export default function TestBooking() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleTestBooking = async () => {
    setLoading(true);
    setError(null);
    try {
      // Giả sử lấy token từ localStorage (User đã login)
      if (!authHeader().Authorization) {
        throw new Error('Vui lòng Đăng nhập trước khi test!');
      }

      // Bước 1: Gọi API tạo Booking với 2 ghế Mock (ID: 1 và 2)
      // Lưu ý: Đảm bảo trong DB đã reset trạng thái 2 ghế này về AVAILABLE
      const bookingRes = await axios.post(`${API_BASE_URL}/bookings`, {
        showtimeSeatIds: [1, 2]
      }, {
        headers: authHeader()
      });

      const bookingId = bookingRes.data.id;

      // Bước 2: Gọi API lấy URL thanh toán VNPay
      const vnpayRes = await axios.get(`${API_BASE_URL}/payment/vnpay/create-url?bookingId=${bookingId}`, {
        headers: authHeader()
      });

      // Bước 3: Chuyển hướng trình duyệt sang trang VNPay
      window.location.href = vnpayRes.data.paymentUrl;

    } catch (err) {
      console.error(err);
      setError(err.response?.data || err.message || 'Có lỗi xảy ra');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="bg-slate-800 p-8 rounded-2xl border border-slate-700 shadow-2xl max-w-md w-full text-center">
        <h1 className="text-3xl font-bold text-white mb-4">Test Luồng Đặt Vé</h1>
        <p className="text-slate-400 mb-8">
          Nhấn nút bên dưới để tự động tạo Booking với 2 ghế giả lập và chuyển hướng sang VNPay.
        </p>
        
        {error && (
          <div className="bg-red-500/20 text-red-400 p-3 rounded-lg mb-6 text-sm">
            {error}
          </div>
        )}

        <button 
          onClick={handleTestBooking}
          disabled={loading}
          className={`w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 px-4 rounded-lg transition-colors ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
        >
          {loading ? 'Đang xử lý...' : '🔥 Chạy Test VNPay Ngay 🔥'}
        </button>

        <div className="mt-6 border-t border-slate-700 pt-6">
          <button 
            onClick={async () => {
              try {
                await axios.get(`${API_BASE_URL}/bookings/reset-test-seats`, {
                  headers: authHeader()
                });
                alert("Đã reset ghế thành công! Bạn có thể test Đặt vé lại.");
              } catch (e) {
                alert("Lỗi khi reset: " + e.message);
              }
            }}
            className="text-sm text-slate-400 hover:text-white underline"
          >
            Lỗi ghế đã bị khóa? Bấm vào đây để Khôi phục (Reset)
          </button>
        </div>
      </div>
    </div>
  );
}
