package com.restaurant.controllers;

import com.restaurant.models.Dish;
import com.restaurant.repositories.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/dishes")
public class DishController {
    private final DishRepository dishRepository;

    public DishController(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Dish> addDish(@RequestBody Dish dish) {
        Dish savedDish = dishRepository.save(dish);
        return new ResponseEntity<>(savedDish, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDish(@PathVariable Integer id) {
        dishRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Dish> updateDish(@PathVariable Integer id, @RequestBody Dish dish) {
        Optional<Dish> existingDish = dishRepository.findById(id);
        if (existingDish.isPresent()) {
            Dish updatedDish = existingDish.get();
            if (dish.getDishName() != null) {
                updatedDish.setDishName(dish.getDishName());
            }
            if (dish.getPrice() != null) {
                updatedDish.setPrice(dish.getPrice());
            }
            if (dish.getCalories() != null) {
                updatedDish.setCalories(dish.getCalories());
            }
            if (dish.getCategory() != null) {
                updatedDish.setCategory(dish.getCategory());
            }
            if (dish.getIsAvailable() != null) {
                System.out.println(dish.getIsAvailable());
                updatedDish.setIsAvailable(dish.getIsAvailable());
            }
            if (dish.getImageAddress() != null) {
                updatedDish.setImageAddress(dish.getImageAddress());
            }
            dishRepository.save(updatedDish);
            return new ResponseEntity<>(updatedDish, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<Dish>> getAvailableDishes() {
        List<Dish> dishes = dishRepository.findAll();
        dishes.removeIf(dish -> !dish.getIsAvailable());
        return new ResponseEntity<>(dishes, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<Dish>> getAllDishes() {
        List<Dish> dishes = dishRepository.findAll();
        return new ResponseEntity<>(dishes, HttpStatus.OK);
    }
}