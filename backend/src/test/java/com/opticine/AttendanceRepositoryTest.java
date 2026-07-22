package com.opticine;

import com.opticine.repository.AttendanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AttendanceRepositoryTest {
    @Autowired
    AttendanceRepository repo;

    @Test
    public void test() {
        repo.search(null, null, null, null);
    }
}
