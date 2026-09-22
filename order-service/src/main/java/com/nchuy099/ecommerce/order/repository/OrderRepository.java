package com.nchuy099.ecommerce.order.repository;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

import com.nchuy099.ecommerce.order.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query("select o.id from OrderEntity o where o.userId = :userId")
    Page<Long> findIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("select o.id from OrderEntity o where o.userId = :userId order by o.createdAt desc, o.id desc")
    List<Long> findIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select o.id from OrderEntity o
            where o.userId = :userId
              and (o.createdAt < :createdAt or (o.createdAt = :createdAt and o.id < :id))
            order by o.createdAt desc, o.id desc
            """)
    List<Long> findIdsByUserIdAfterCursor(
            @Param("userId") Long userId,
            @Param("createdAt") Instant createdAt,
            @Param("id") Long id,
            Pageable pageable
    );

    @Query("select distinct o from OrderEntity o left join fetch o.items where o.id in :ids")
    List<OrderEntity> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);
}
