package com.restaurant.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int dishId;

    @NotBlank
    @Size(max = 50)
    private String dishName;

    @NotNull
    private double price;

    @NotNull
    private int calories;

    @Enumerated(EnumType.STRING)
    private ECategory category;

    @NotNull
    private boolean isAvailable;

    public Dish() {
    }

    public Dish(String dishName, double price, int calories, ECategory category, String description, List<String> ingredients, boolean isAvailable) {
        this.dishName = dishName;
        this.price = price;
        this.calories = calories;
        this.category = category;
        this.isAvailable = isAvailable;
    }

    public int getDishId() {
        return dishId;
    }

    public void setDishId(int dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public ECategory getCategory() {
        return category;
    }

    public void setCategory(ECategory category) {
        this.category = category;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
}