Opticine SBA301 Postman Files

1. Import collection
   - Open Postman.
   - Choose Import.
   - Select postman/Opticine_SBA301.postman_collection.json.

2. Import environment
   - Choose Import.
   - Select postman/Opticine_SBA301_environment.json.
   - Select environment "Opticine SBA301 Local".

3. Run backend
   - cd backend
   - mvn spring-boot:run

4. Base URL
   - http://localhost:8080/api

5. Recommended test order
   - Auth
   - Movies/Rooms/Seats/Showtimes
   - Booking
   - Payment
   - Membership
   - Staff
   - Reports

6. Notes
   - Replace demo credentials based on seed data before running login requests.
   - Do not paste real JWT tokens, bank keys, Google secrets, PayOS keys, SMTP passwords, or production credentials into this repository.
   - Successful login requests save tokens into collection variables: adminToken, customerToken, and staffToken.

7. Screenshot checklist
   - Collection imported
   - Login success
   - CRUD movie/room/showtime
   - Booking/payment success
   - Membership response
   - Admin revenue report
