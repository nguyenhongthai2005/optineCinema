import React from 'react';

export default function Footer() {
  return (
    <footer className="opticine-footer">
      <div className="container-fluid py-5" style={{ maxWidth: 1440, margin: '0 auto', padding: '0 48px' }}>
        <div className="row g-4 justify-content-between align-items-center">
          <div className="col-12 col-md-4 text-center text-md-start">
            <span className="footer-brand">OPTICINE</span>
            <p className="footer-desc mt-3 mx-auto mx-md-0">
              Hệ thống rạp chiếu phim tiêu chuẩn quốc tế với trải nghiệm điện ảnh 8K chân thực nhất.
            </p>
            <div className="d-flex gap-3 mt-3 justify-content-center justify-content-md-start">
              <a href="#" className="footer-icon-link"><span className="material-symbols-outlined">public</span></a>
              <a href="#" className="footer-icon-link"><span className="material-symbols-outlined">share</span></a>
              <a href="#" className="footer-icon-link"><span className="material-symbols-outlined">mail</span></a>
            </div>
          </div>
          
          <div className="col-12 col-md-8">
            <div className="d-flex flex-wrap justify-content-center justify-content-md-end gap-5">
              <div className="d-flex flex-column gap-2">
                <h5 className="footer-heading">Khám phá</h5>
                <a href="#" className="footer-link">Về chúng tôi</a>
                <a href="#" className="footer-link">Tuyển dụng</a>
              </div>
              <div className="d-flex flex-column gap-2">
                <h5 className="footer-heading">Hỗ trợ</h5>
                <a href="#" className="footer-link">Chăm sóc khách hàng</a>
                <a href="#" className="footer-link">Chính sách bảo mật</a>
              </div>
              <div className="d-flex flex-column gap-2">
                <h5 className="footer-heading">Pháp lý</h5>
                <a href="#" className="footer-link">Điều khoản sử dụng</a>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div className="footer-copyright">
        <p>© 2024 OPTICINE Cinema Group. All rights reserved.</p>
      </div>
    </footer>
  );
}
