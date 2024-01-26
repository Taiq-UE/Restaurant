package com.restaurant.models;

import com.restaurant.models.Enums.ECategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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
    private Double price;

    @NotNull
    private Integer calories;

    @Enumerated(EnumType.STRING)
    private ECategory category;

    @NotNull
    private Boolean isAvailable;

    private String imageAddress;

    public Dish() {
    }

    public Dish(String dishName, Double price, Integer calories, ECategory category, String description, List<String> ingredients, Boolean isAvailable) {
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public ECategory getCategory() {
        return category;
    }

    public void setCategory(ECategory category) {
        this.category = category;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    public String getImageAddress() {
        return imageAddress;
    }

    public void setImageAddress(String imageAddress) {
        this.imageAddress = imageAddress;
    }

}