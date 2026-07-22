package com.opticine.repository;

import com.opticine.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);

    @Query("""
            select c from Customer c
            where (cast(:keyword as String) is null or lower(c.fullName) like lower(concat('%', :keyword, '%'))
                or lower(c.email) like lower(concat('%', :keyword, '%'))
                or lower(c.phone) like lower(concat('%', :keyword, '%')))
            order by c.id desc
            """)
    List<Customer> searchCustomers(@Param("keyword") String keyword);
}

