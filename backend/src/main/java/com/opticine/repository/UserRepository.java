package com.opticine.repository;

import com.opticine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);

    @Query("""
            select distinct u from User u join u.roles r
            where r.name = 'ROLE_STAFF'
            and (cast(:keyword as string) is null or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(u.phone) like lower(concat('%', :keyword, '%'))
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(r.name) like lower(concat('%', :keyword, '%')))
            and (cast(:status as string) is null or upper(u.status) = upper(:status))
            and (cast(:position as string) is null or u.staffPosition = :position)
            and (cast(:contractType as string) is null or u.employmentType = :contractType)
            order by u.id desc
            """)
    List<User> searchStaff(@Param("keyword") String keyword,
                           @Param("status") String status,
                           @Param("position") String position,
                           @Param("contractType") String contractType);

    @Query("""
            select u.username from User u join u.roles r
            where r.name = 'ROLE_STAFF'
            and u.username like 'STAFF%'
            """)
    List<String> findGeneratedStaffUsernames();
}

