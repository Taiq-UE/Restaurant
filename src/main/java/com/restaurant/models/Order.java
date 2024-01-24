package com.restaurant.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "orderId")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int orderId;

    @NotNull
    private int orderNumber;

    @NotNull
    private LocalDateTime orderDate;

    @ElementCollection
    private List<String> orderedDishes;

    @NotBlank
    private String orderStatus;

    @NotBlank
    private String paymentType;

    @NotBlank
    private String paymentStatus;

    @NotNull
    private double totalCost;

    @NotBlank
    private String deliveryMethod;

    @Size(max = 255)
    private String additionalNotes;

    @NotBlank
    private String deliveryInfo;
    private static int orderCounter = 0;

    public Order() {
    }

    public Order(int orderNumber, LocalDateTime orderDate, List<String> orderedDishes, String orderStatus, String paymentType, String paymentStatus, double totalCost, String deliveryMethod, String additionalNotes, String deliveryInfo) {
        this.orderNumber = generateOrderNumber();
        this.orderDate = LocalDateTime.now();
        this.orderedDishes = orderedDishes;
        this.orderStatus = orderStatus;
        this.paymentType = paymentType;
        this.paymentStatus = paymentStatus;
        this.totalCost = totalCost;
        this.deliveryMethod = deliveryMethod;
        this.additionalNotes = additionalNotes;
        this.deliveryInfo = deliveryInfo;
    }

    private static synchronized int generateOrderNumber() {
        orderCounter = (orderCounter % 100) + 1;
        return orderCounter;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public List<String> getOrderedDishes() {
        return orderedDishes;
    }

    public void setOrderedDishes(List<String> orderedDishes) {
        this.orderedDishes = orderedDishes;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public String getDeliveryInfo() {
        return deliveryInfo;
    }

    public void setDeliveryInfo(String deliveryInfo) {
        this.deliveryInfo = deliveryInfo;
    }
}