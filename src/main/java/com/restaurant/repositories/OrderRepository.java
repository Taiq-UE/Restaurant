package com.restaurant.repositories;

import com.restaurant.models.Enums.EOrderStatus;
import com.restaurant.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(value = "SELECT ((MAX(order_number) + 1) % 101) FROM orders", nativeQuery = true)
    Integer generateOrderNumber();

    @Query("SELECT o FROM Order o WHERE o.paymentStatus = 'UNPAID' AND o.orderDate > :dateTime")
    List<Order> getUnpaidOrdersAfter(@Param("dateTime") LocalDateTime dateTime);

    List<Order> findByOrderStatus(EOrderStatus status);
}