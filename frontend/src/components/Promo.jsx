import React from 'react';

const promos = [
  {
    category: 'Membership',
    title: 'Gia Nhập OPTICINE Club',
    desc: 'Nhận ngay 1 vé miễn phí và bỏng nước cho thành viên mới.',
    linkText: 'Tìm hiểu thêm',
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuC0FXRPZmyxoS8jwKQGxxN_8RLbl7m53IA3dPAzuoVmzUEW5AL5eMOMb9LwAn6nflwFuVouxoT6Z0P34jN3SBC4vVEJkv_XTroIaImPWe9weYgh685dV46Bu7B7h4wPlLz--56zsml3Bp2tYSwL-Tg39N4i5gF7UXAym9nC6oI4CpbCkUtnvKFjavojK3nB77cexMoPrawrdR2pQmth-qoWL7r89dRYsc6Q3gSMgXGs2xT_p0fDZFxPLXn_X4GNmne_qKvWBkYyaw'
  },
  {
    category: 'Promotion',
    title: 'Combo Couple 199k',
    desc: 'Tiết kiệm 30% khi mua gói bắp nước dành cho hai người.',
    linkText: 'Săn deal ngay',
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuCE4UMADr6T56jAg2E0_qxmRqHnGhQlF8XFkm_Il6zh_Lj-jn7Xy_glhUldJbeqGNXUYpzjEX5A-Z2zxEKW1jTfH7wvgEA6lKoHGelfmlbF01by8-5sD8GaCiV_uvO3h_CBfsGgcYv_lNPz5BdqU4zoMKVzfcDfbBbl4sgFGehQffAUNPIrmEhOLsK7_SsVSdgldTJOeHBfk4wT-GdsJXQl6x4Ih_Cs7i5YbLq0fYARQ5mHD5VJHvA742ScWZHM7QiUnQ2K4YRO1g'
  },
  {
    category: 'Special Event',
    title: 'Suất Chiếu Sớm',
    desc: 'Trở thành người đầu tiên trải nghiệm siêu phẩm Marvel tuần này.',
    linkText: 'Xem lịch chiếu',
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuBcADSiN1bDKFT6mE84pLqUJNhGWNNhPjmQGoxEs8j_oEt5CwwnyN_4ULNusUqZPiNoh0YT1uLoKiyPTXEk5sxn_zg442cO36m5H3rX3EqZZ9MaVbDbBZvgBND_nhRvKJW8fb3xN-h2yGsS6QDL7qWzLxGKxETgcAnH-VXnEdROG4rMYnZtnR8pptCZvJF0GsO0v6dG_jWPp5rgPTSZLeeyJNKAoSM-KJYQ32Wh4SnpPac37QXg8gqPrfbWzeWR56zcKo42Eu1itg'
  }
];

export default function Promo() {
  return (
    <section className="promo-section">
      <div className="container-fluid" style={{ maxWidth: 1440, margin: '0 auto', padding: '0 48px' }}>
        <div className="d-flex align-items-center gap-4 mb-5">
          <h2 className="section-title mb-0">Ưu Đãi & Sự Kiện</h2>
          <div className="flex-grow-1" style={{ height: '1px', background: 'rgba(69,70,77,0.2)' }}></div>
        </div>

        <div className="promo-scroll">
          {promos.map((promo, index) => (
            <div className="promo-card" key={index}>
              <img src={promo.img} alt={promo.title} />
              <div className="promo-gradient" style={{ position: 'absolute', inset: 0 }}></div>
              <div className="promo-card-content">
                <span className="promo-category">{promo.category}</span>
                <h3 className="promo-title">{promo.title}</h3>
                <p className="promo-desc">{promo.desc}</p>
                <button className="promo-link">
                  {promo.linkText} <span className="material-symbols-outlined ms-1">chevron_right</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
