package com.restaurant.repositories;

import com.restaurant.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(value = "SELECT ((MAX(order_number) + 1) % 101) FROM orders", nativeQuery = true)
    Integer generateOrderNumber();
}