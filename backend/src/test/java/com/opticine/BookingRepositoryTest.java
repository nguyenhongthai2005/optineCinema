package com.opticine;

import com.opticine.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BookingRepositoryTest {
    @Autowired
    BookingRepository repo;

    @Test
    public void test() {
        repo.searchStaffBookings(null, null, null, null);
    }
}
