# 🎬 Opticine – Cinema Booking System

A full-stack cinema booking platform built with **Spring Boot** (backend) and **React + Vite + Tailwind CSS** (frontend), backed by **MySQL 8.0**.

**Opticine** means **Optimization + Cinema**. Beyond booking tickets, the admin system includes automatic showtime scheduling to suggest room/movie/time assignments that optimize expected revenue and room utilization.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Docker & Docker Compose | 20.x+ |
| Java | 17 |
| Maven | optional if `backend/mvnw` can download Maven, otherwise 3.8+ |
| Node.js | 18+ |
| npm | 9+ |

---

## 1. Start the Database

```bash
# From the root of the project
docker compose up -d
```

This spins up a MySQL 8.0 container (`opticine_mysql`) on port **3306** with:
- Root password: `root`
- Database: `opticine_db`

Verify it's running:
```bash
docker ps
# or connect directly
docker exec -it opticine_mysql mysql -uroot -proot opticine_db
```

---

## 2. Start the Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at **http://localhost:8080**

The Maven wrapper in this repo is the script-only wrapper and downloads Maven 3.9.6 on first use. If wrapper download is blocked, use an installed Maven instead:

```bash
cd backend
mvn spring-boot:run
```

Flyway is currently disabled in `application.yml`; Hibernate `ddl-auto: update` and the `DataInitializer` seed the development database.

On startup the backend automatically creates demo roles, `admin`/`user` accounts, movies, rooms, seats, time slots, showtimes for today plus the next 7 days, and showtime-seat rows. The seeder checks existing records first, so restarting the backend will not duplicate demo data or wipe bookings.

---

## 3. Start the Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev
```

The app will be available at **http://localhost:5173**

### Google OAuth setup

Opticine uses the backend OAuth2 redirect flow. The Google client secret is read only by Spring Boot and must never be added to a frontend `.env` file.

Create an OAuth 2.0 Web application in Google Cloud Console and configure:

- Authorized JavaScript origin: `http://localhost:5173`
- Authorized redirect URI: `http://localhost:8080/api/login/oauth2/code/google`

The `/api` part is required because this project runs the entire Spring application under the `/api` servlet context. Start the backend from the same PowerShell terminal in which you set:

```powershell
$env:GOOGLE_CLIENT_ID="your_google_client_id"
$env:GOOGLE_CLIENT_SECRET="your_google_client_secret"
$env:APP_FRONTEND_URL="http://localhost:5173"
mvn spring-boot:run
```

The frontend starts Google authentication at `http://localhost:8080/api/oauth2/authorization/google`. If either Google variable is absent, the backend logs a clear warning at startup; Google login cannot work until both values are configured.

## Local environment setup

Keep machine-specific credentials in ignored local files so they remain available between PowerShell sessions without entering each variable again.

Create the backend launcher from the safe template:

```powershell
Copy-Item backend/run-local.example.ps1 backend/run-local.ps1
```

Edit `backend/run-local.ps1` and replace the placeholders for `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, and `APP_FRONTEND_URL`. Fill in `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` when email delivery is needed, and update the `VIETQR_*` values when payment QR support is needed. Then run:

```powershell
cd backend
.\run-local.ps1
```

Create the frontend local environment file and start Vite:

```powershell
Copy-Item frontend/.env.example frontend/.env.local
cd frontend
npm install
npm run dev
```

For Google Cloud, create an OAuth client with application type **Web application** and configure:

- Authorized JavaScript origin: `http://localhost:5173`
- Authorized redirect URI: `http://localhost:8080/api/login/oauth2/code/google`
- Google login start URL: `http://localhost:8080/api/oauth2/authorization/google`

Do not commit `backend/run-local.ps1` or `frontend/.env.local`; these local files may contain secrets. Commit only the corresponding example files.

Default seeded accounts:

- Admin: `admin` / `123456`
- Customer: `user` / `123456`
- Staff: `staff` / `123456`

VNPay sandbox values live in `backend/src/main/resources/application.yml`. The return URL is configured for the local backend and redirects back to the Vite frontend.

Customer booking flow:

1. Open `http://localhost:5173`.
2. Click `Đặt vé` on a movie card or in the navbar.
3. Pick a showtime on `/showtimes` or `/showtimes?movieId={id}`.
4. Log in as `user / 123456` if prompted; the app returns to the selected seat map.
5. Select seats, confirm the lock, review `/booking/confirm`, and choose VNPay or VietQR.
6. VNPay returns to `/payment-result`; VietQR opens `/payment/vietqr?bookingId={id}` for bank transfer.
7. After payment is confirmed, view the e-ticket from the result page or later from `Vé của tôi`.

Staff module:

1. Login as `staff / 123456`; the frontend redirects to `/staff`.
2. `/staff/dashboard` shows daily operations, pending VietQR count, upcoming showtimes, and attendance quick actions.
3. `/staff/attendance` lets staff check in once per day, check out once after check-in, and view their own attendance history.
4. `/staff/showtimes` lists today's showtimes. Click `Bán vé` to open `/staff/sell-ticket?showtimeId={id}`.
5. `/staff/sell-ticket` supports counter sales: choose showtime, select seats, hold seats using the existing seat-lock API, enter optional walk-in customer info, choose `CASH` or `VIETQR`, then confirm.
6. Cash counter sales immediately confirm the booking, book the seats, create invoice/payment/tickets, and generate ticket QR codes.
7. VietQR counter sales can still move to `WAITING_CONFIRMATION` for manual staff confirmation when auto-confirm is disabled.
8. `/staff/check-in` verifies ticket QR codes and checks in valid tickets. Used tickets cannot be checked in twice.
9. `/staff/orders` lists recent bookings with filters and print action.
10. `/staff/profile` lets staff update their own profile and change password.

Admin staff account management:

- Admins can open `/admin/staff` to create and manage staff accounts.
- Admins select a fixed staff position: `TICKET_CHECKER` (`Soát vé`) or `COUNTER_SALES` (`Bán tại quầy`).
- Admins select a fixed contract type: `SEASONAL` (`Thời vụ`) or `FULL_TIME` (`Hợp đồng chính thức`).
- Staff usernames are generated by the backend in sequential format: `STAFF001`, `STAFF002`, `STAFF003`, and continue naturally after `STAFF999` as `STAFF1000`.
- Admins do not enter staff usernames or passwords. The backend creates a secure random temporary password, stores only the encoded password, and returns the raw password only once after create or reset.
- After creating or resetting a staff account, copy the generated credentials and send them to the staff member through a secure channel.
- Admins can reset a staff password from the staff list; the new temporary password is also shown only once.
- Demo staff account: `staff` / `123456`. This demo username is separate from the generated `STAFF001` sequence.

Admin movie management:

- Admins and managers can open `/admin/movies` to create, edit, search, filter, and manage movies.
- Movie records support poster URL, YouTube trailer URL, status, release date, duration, age rating, genre, and popularity score.
- Trailer links are validated as YouTube links and played inside Opticine with an embedded iframe modal; users are not redirected to YouTube.
- Movie statuses are `NOW_SHOWING` (`Đang chiếu`), `COMING_SOON` (`Sắp chiếu`), and `STOPPED` (`Ngừng chiếu`).
- `NOW_SHOWING` movies appear on the homepage and in showtime creation; `STOPPED` movies are hidden from public now-showing lists.
- If a movie has future active showtimes, the backend rejects stopping it with a clear safety message.

Staff position permissions:

- `Soát vé` staff can use dashboard, ticket QR check-in, showtimes, attendance, availability, personal schedule, and profile.
- `Bán tại quầy` staff can use dashboard, counter ticket selling, showtimes, pending VietQR confirmation, orders, attendance, availability, personal schedule, and profile.
- The frontend hides forbidden menu items, and the backend rejects forbidden staff APIs based on the staff position.

Staff availability and assignments:

- Staff can register weekly available working time at `/staff/availability`.
- Admins can view staff availability and create assignments at `/admin/staff-assignments`.
- Assignment types are `TICKET_CHECKING` (`Soát vé`) and `COUNTER_SALES` (`Bán tại quầy`), and each assignment must match the staff position.
- Assignment time must be inside the staff member's registered availability and cannot overlap another scheduled assignment.
- Staff can view their own assignments at `/staff/my-schedule`.
- Attendance check-in/check-out uses the scheduled assignment time as the shift time when an active assignment exists for the day; otherwise it falls back to the basic default shift.

Staff attendance statuses:

- `NOT_CHECKED_IN`: Chưa chấm công
- `CHECKED_IN`: Đã check-in
- `ON_TIME`: Đúng giờ
- `LATE`: Đi trễ
- `COMPLETED`: Hoàn thành ca
- `EARLY_LEAVE`: Về sớm
- `ABSENT`: Vắng mặt

Admin/manager attendance:

- Admin or manager can open `/admin/attendance` to view all staff attendance records.
- Admin or manager can create, edit, or correct attendance records when staff forget to check in or check out.

Admin seat management:

- Admins/managers can open `/admin/seats` to manage the seat map for each room.
- Click a seat to quickly cycle its type: `Ghế thường` → `Ghế VIP` → `Ghế đôi`.
- Hold `Ctrl` or `Cmd` and click to select multiple specific seats.
- Use row/column selectors to select many seats at once, then apply a seat type from the right panel.
- Use `Bảo trì` to mark selected seats as unavailable for sale; maintenance seats appear red.
- Customer seat maps show maintenance seats as `Bảo trì`, and those seats cannot be selected, locked, or booked.

Staff attendance location check:

- Staff check-in/check-out uses the browser location permission prompt.
- The backend validates the submitted latitude/longitude against a configured workplace location and radius.
- When staff has an assigned shift, check-in is allowed up to 15 minutes before the shift by default.
- Configure timing with `ATTENDANCE_EARLY_CHECKIN_MINUTES=15` and `ATTENDANCE_LATE_GRACE_MINUTES=5`.
- Staff checking in after the late grace window is marked late; checking out before shift end is allowed and records early-leave minutes.
- Only check-in/check-out location samples are stored; the app does not continuously track staff location.
- Staff must run the frontend on `localhost` or HTTPS for browser geolocation to work.
- For a demo at home, set the workplace latitude/longitude to your current location and use a radius such as 500-1000 meters.

PowerShell setup example:

```powershell
$env:ATTENDANCE_LOCATION_CHECK_ENABLED="true"
$env:ATTENDANCE_LOCATION_NAME="Opticine Cinema"
$env:ATTENDANCE_LATITUDE="17.467256"
$env:ATTENDANCE_LONGITUDE="106.587302"
$env:ATTENDANCE_RADIUS_METERS="5000"
$env:ATTENDANCE_MAX_ACCURACY_METERS="5000"
$env:ATTENDANCE_STRICT_ACCURACY_CHECK="false"
$env:ATTENDANCE_EARLY_CHECKIN_MINUTES="15"
$env:ATTENDANCE_LATE_GRACE_MINUTES="5"
```

Start the backend from the same terminal after setting those variables. With `ATTENDANCE_STRICT_ACCURACY_CHECK=false`, low-accuracy laptop/desktop location can pass for demo and records a warning. Set it to `true` to reject low-accuracy samples. To temporarily allow attendance without location, set `ATTENDANCE_LOCATION_CHECK_ENABLED=false`.

To reset demo data completely, stop the backend, remove the Docker volume, and start again:

```bash
docker compose down -v
docker compose up -d
```

Then start the backend; `DataInitializer` will seed a fresh database.

---

## Email Setup

Ticket email uses Spring Boot Mail. Configure these environment variables before starting the backend:

```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
```

For Gmail, use an app password, not your normal Gmail password. Optional overrides:

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
FRONTEND_URL=http://localhost:5173
VNPAY_RETURN_URL=http://localhost:8080/api/payment/vnpay/return
```

If mail sending fails, payment still stays confirmed. Customers can use `Gửi lại email` from the payment result or `Vé của tôi`.

---

## VNPay Sandbox Testing

Use VNPay sandbox card payment for the local demo. VNPay QR payment scanning may not behave like production on localhost.

- Bank: `NCB`
- Card number: `9704198526191432198`
- Cardholder: `NGUYEN VAN A`
- Issue date: `07/15`
- OTP: `123456`

For mobile/public callback testing, expose the backend with ngrok, Cloudflare Tunnel, or deployment, then set `VNPAY_RETURN_URL` to that public backend URL.

---

## VietQR Payment Demo

VietQR is available as an additional payment method. It does not replace VNPay.

Configure your bank account with environment variables before starting the backend:

```bash
VIETQR_BANK_ID=MB
VIETQR_ACCOUNT_NO=your-account-number
VIETQR_ACCOUNT_NAME=YOUR ACCOUNT NAME
VIETQR_TEMPLATE=compact2
```

Do not commit your real bank account number if this repository is public. Use environment variables or a local-only config file.

Demo flow:

1. Customer chooses `VietQR Bank Transfer` on `/booking/confirm`.
2. The system creates a real VietQR image URL with a unique transfer content like `OPTI123`.
3. Customer scans the QR using a real banking app.
4. In demo mode, customer transfers the reduced demo amount with the exact transfer content.
5. Customer clicks `Tôi đã chuyển khoản`.
6. If `VIETQR_AUTO_CONFIRM_ENABLED=true`, the system marks payment `PAID`, confirms the booking, books seats, creates one invoice, creates ticket QR codes, and sends the ticket email.
7. If `VIETQR_AUTO_CONFIRM_ENABLED=false`, booking moves to `WAITING_CONFIRMATION`; admin/staff can confirm or reject it at `/admin/payments/pending`.
8. On reject, the booking becomes `CANCELLED` and seats are released.

Automatic VietQR confirmation here is demo-only. Production should use a bank transaction webhook/API or manual reconciliation.

---

## Ticket Email And QR Check-In

After VNPay success, auto-confirmed VietQR payment, or admin-confirmed VietQR payment, the backend confirms the booking, books the seats, creates tickets, generates QR codes, and sends an HTML e-ticket email to the customer. The email includes QR images and the raw QR string as fallback.

Admin/staff check-in flow:

1. Login as `admin / 123456` or a staff account.
2. Open `/admin/ticket-checkin`.
3. Scan the customer QR with the camera or paste the raw QR string.
4. Click `Xác minh` to view ticket details.
5. Click `Check in` to mark the ticket as `USED`.
6. Scanning the same QR again returns a clear “ticket already used” message.

---

## Auto Showtime Scheduling

Admins/managers can open `/admin/auto-schedule` to generate a draft optimized schedule before creating real showtimes.

Flow:

1. Choose date range, optimization mode, opening/closing time, and cleaning buffer.
2. Click `Generate schedule`.
3. Review the draft plan: estimated revenue, expected tickets, room utilization, and proposed showtimes.
4. Click `Áp dụng lịch chiếu`.
5. The backend creates real `Showtime` records and `ShowtimeSeat` rows.
6. Applied showtimes appear in admin showtime management and customer showtime pages.

The first implementation uses a practical greedy scoring algorithm:

- Candidate = movie + room + start time.
- Demand prediction uses movie popularity, day factor, time factor, room factor, and simple genre/title hints.
- Revenue estimate uses expected tickets multiplied by average active-seat price, room multiplier, and matching time-slot multiplier.
- Constraints prevent room overlaps, enforce cleaning buffer, keep showtimes inside opening hours, and preserve existing showtimes.
- Candidates are sorted by score and selected greedily.
- Modes:
  - `MAX_REVENUE`: prioritizes expected revenue.
  - `BALANCED`: combines revenue, occupancy, and movie diversity.
  - `MAX_UTILIZATION`: prioritizes occupancy and room usage.

This is intentionally explainable for the SBA project report and can later be upgraded to a more advanced optimization solver.

---

## Combo and Counter Sales

Admins/managers can manage popcorn, drink, and combo items at `/admin/combos`.

Customer booking flow:

1. Select seats.
2. Add or skip combos at `/booking/combos`.
3. Confirm payment with tickets plus combo totals.
4. Booking history and ticket emails show combo lines.

Counter staff (`COUNTER_SALES`) can:

- Add combos while selling tickets at `/staff/sell-ticket`.
- Sell standalone cash food orders at `/staff/combo-sales`.

Demo combos are seeded automatically when the combo table is empty.

---

## VietQR Demo Mode

For demo VietQR testing, the real order value is still displayed and stored, but the QR transfer amount can be reduced.

PowerShell:

```powershell
$env:VIETQR_AUTO_CONFIRM_ENABLED="true"
$env:VIETQR_DEMO_AMOUNT_ENABLED="true"
$env:VIETQR_DEMO_AMOUNT_RATE="0.1"
$env:VIETQR_DEMO_ROUND_TO="1000"
$env:VIETQR_DEMO_MIN_AMOUNT="1000"
```

Behavior:

- Original order price remains visible in checkout, receipt, and booking detail.
- VietQR QR code uses the reduced demo transfer amount.
- Clicking `Tôi đã chuyển khoản` auto-confirms the booking, books seats, and creates tickets.

For production-style manual reconciliation:

```powershell
$env:VIETQR_AUTO_CONFIRM_ENABLED="false"
$env:VIETQR_DEMO_AMOUNT_ENABLED="false"
```

Demo auto-confirm is for testing only. Production should use a payment gateway callback, bank transaction API, or manual reconciliation.

---

## Project Structure

```
opticine-system/
├── docker-compose.yml          # MySQL container
├── .gitignore
├── README.md
├── backend/                    # Spring Boot (Maven, Java 17)
│   └── src/main/
│       ├── java/com/opticine/booking/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── entity/
│       │   ├── dto/
│       │   ├── config/
│       │   └── exception/
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               └── V1__init_database.sql
└── frontend/                   # React + Vite + Tailwind CSS
    └── src/
        ├── components/
        ├── pages/
        ├── layouts/
        ├── services/
        └── utils/
```

---

## Environment Variables

Copy `.env.example` to `.env` in the `frontend/` directory and update as needed.

---

## Tech Stack

- **Backend:** Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Lombok, MySQL 8.0, VNPay, WebSocket
- **Frontend:** React 18, Vite 5, Tailwind CSS 3, React Router DOM, Axios
