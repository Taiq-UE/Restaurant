package com.restaurant.controllers;

import com.restaurant.exceptions.ResourceNotFoundException;
import com.restaurant.models.Dish;
import com.restaurant.models.Order;
import com.restaurant.repositories.DishRepository;
import com.restaurant.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DishRepository dishRepository;


    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        List<Dish> dishes = new ArrayList<>();
        for (Dish dish : order.getOrderedDishes()) {
            Integer dishIdInt = dish.getDishId();
            Dish dishFromDb = dishRepository.findById(dishIdInt)
                    .orElseThrow(() -> new ResourceNotFoundException("Dish not found with id " + dishIdInt));
            dishes.add(dishFromDb);
        }
        order.setOrderNumber(orderRepository.generateOrderNumber());
        order.setOrderedDishes(dishes);
        order.setTotalCost(order.calculateTotalCost()); // calculate total cost after setting ordered dishes
        Order savedOrder = orderRepository.save(order);
        return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> new ResponseEntity<>(order, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order orderDetails) {
        return orderRepository.findById(id)
                .map(order -> {
                    // update order details here
                    Order updatedOrder = orderRepository.save(order);
                    return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        orderRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}