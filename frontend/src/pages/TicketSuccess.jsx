import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { QRCodeSVG } from 'qrcode.react';
import { API_BASE_URL, authHeader } from '../services/api';

export default function TicketSuccess() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const bookingId = searchParams.get('bookingId');
  const status = searchParams.get('status');
  
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!bookingId) {
      setError('Không tìm thấy thông tin đặt vé.');
      setLoading(false);
      return;
    }

    if (status === 'failed') {
      setError('Thanh toán thất bại hoặc đã bị hủy.');
      setLoading(false);
      return;
    }

    const fetchTickets = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/bookings/${bookingId}/tickets`, {
          headers: authHeader()
        });
        setTickets(response.data);
      } catch (err) {
        setError('Lỗi khi tải thông tin vé.');
      } finally {
        setLoading(false);
      }
    };

    fetchTickets();
  }, [bookingId, status]);

  if (loading) return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center text-white">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-500"></div>
    </div>
  );

  if (error) return (
    <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center text-white p-6">
      <div className="bg-red-500/20 p-6 rounded-2xl border border-red-500/50 backdrop-blur-sm max-w-md w-full text-center">
        <h2 className="text-2xl font-bold text-red-400 mb-4">Rất tiếc!</h2>
        <p className="text-gray-300 mb-6">{error}</p>
        <button onClick={() => navigate('/')} className="bg-indigo-600 hover:bg-indigo-700 text-white font-medium py-2 px-6 rounded-lg transition-colors">
          Về trang chủ
        </button>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-slate-900 text-white py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-extrabold bg-clip-text text-transparent bg-gradient-to-r from-indigo-400 to-purple-500 mb-4">
            Thanh toán thành công
          </h1>
          <p className="text-lg text-slate-400">Cảm ơn bạn đã đặt vé. Dưới đây là vé điện tử của bạn.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {tickets.map((ticket, index) => (
            <div key={ticket.id || index} className="bg-slate-800/50 backdrop-blur-xl border border-slate-700 rounded-3xl overflow-hidden shadow-2xl transition-transform hover:-translate-y-1">
              {/* Header */}
              <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-6 text-center border-b border-indigo-500 border-dashed relative">
                <div className="absolute -bottom-4 -left-4 w-8 h-8 bg-slate-900 rounded-full border border-slate-700"></div>
                <div className="absolute -bottom-4 -right-4 w-8 h-8 bg-slate-900 rounded-full border border-slate-700"></div>
                <h2 className="text-2xl font-bold tracking-tight">{ticket.movieTitle}</h2>
                <p className="text-indigo-200 mt-1 font-medium">{ticket.roomName} • Ghế {ticket.seatLabel}</p>
              </div>
              
              {/* Body */}
              <div className="p-8 flex flex-col items-center relative">
                <div className="w-full mb-8 flex justify-between text-sm text-slate-300">
                  <div className="text-left">
                    <p className="text-slate-500 mb-1">Thời gian chiếu</p>
                    <p className="font-semibold text-white">
                      {new Date(ticket.startTime).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-slate-500 mb-1">Giá vé</p>
                    <p className="font-semibold text-green-400">{ticket.price.toLocaleString('vi-VN')} đ</p>
                  </div>
                </div>

                {/* QR Code container */}
                <div className="bg-white p-4 rounded-2xl shadow-inner mb-6">
                  <QRCodeSVG 
                    value={ticket.qrCode} 
                    size={160}
                    level={"Q"}
                    includeMargin={false}
                  />
                </div>
                
                <p className="text-xs text-slate-500 font-mono tracking-widest uppercase">
                  Mã vé: {ticket.qrCode.split('-')[0]}
                </p>
              </div>
            </div>
          ))}
        </div>

        <div className="mt-12 text-center">
          <button onClick={() => window.print()} className="bg-slate-800 hover:bg-slate-700 border border-slate-600 text-white font-medium py-3 px-8 rounded-xl transition-colors shadow-lg mr-4">
            Tải vé về máy
          </button>
          <button onClick={() => navigate('/')} className="bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white font-medium py-3 px-8 rounded-xl transition-all shadow-lg shadow-indigo-500/25">
            Mua vé khác
          </button>
        </div>
      </div>
    </div>
  );
}
