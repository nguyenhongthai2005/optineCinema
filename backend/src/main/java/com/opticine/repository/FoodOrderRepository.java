package com.opticine.repository;

import com.opticine.entity.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    @Query("""
            select distinct o from FoodOrder o
            left join o.items i
            where (coalesce(cast(:from as String), '') = '' or o.createdAt >= :from)
            and (coalesce(cast(:to as String), '') = '' or o.createdAt < :to)
            and (coalesce(:keyword, '') = ''
                or cast(o.id as String) like concat('%', :keyword, '%')
                or lower(o.customerName) like lower(concat('%', :keyword, '%'))
                or lower(o.customerPhone) like lower(concat('%', :keyword, '%'))
                or lower(i.comboNameSnapshot) like lower(concat('%', :keyword, '%')))
            order by o.createdAt desc
            """)
    List<FoodOrder> search(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword
    );
}
