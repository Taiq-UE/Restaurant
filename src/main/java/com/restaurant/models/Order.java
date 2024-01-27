package com.restaurant.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.restaurant.models.Enums.*;
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

    @Enumerated(EnumType.STRING)
    private EOrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private EPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    private EPaymentStatus paymentStatus;

    @NotNull
    private double totalCost;

    @Enumerated(EnumType.STRING)
    private EDeliveryMethod deliveryMethod;

    @Size(max = 255)
    private String additionalNotes;

    private String deliveryInfo;

    private static int orderCounter = 0;

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.orderNumber = generateOrderNumber();
    }

    public Order(List<String> orderedDishes, EOrderStatus orderStatus, EPaymentType paymentType, EPaymentStatus paymentStatus, double totalCost, EDeliveryMethod deliveryMethod, String additionalNotes, String deliveryInfo) {
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

    public EOrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(EOrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public EPaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(EPaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public EPaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(EPaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public EDeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(EDeliveryMethod deliveryMethod) {
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