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
            where (cast(:from as timestamp) is null or o.createdAt >= :from)
            and (cast(:to as timestamp) is null or o.createdAt < :to)
            and (cast(:keyword as string) is null
                or cast(o.id as string) like concat('%', :keyword, '%')
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
