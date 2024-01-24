package com.restaurant.controllers;

import com.restaurant.models.Dish;
import com.restaurant.repositories.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Dish> addDish(@RequestBody Dish dish) {
        Dish savedDish = dishRepository.save(dish);
        return new ResponseEntity<>(savedDish, HttpStatus.CREATED);
    }

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
            updatedDish.setDishName(dish.getDishName());
            updatedDish.setPrice(dish.getPrice());
            updatedDish.setCalories(dish.getCalories());
            updatedDish.setCategory(dish.getCategory());
            updatedDish.setAvailable(dish.isAvailable());
            dishRepository.save(updatedDish);
            return new ResponseEntity<>(updatedDish, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<Dish>> getAvailableDishes() {
        List<Dish> dishes = dishRepository.findAll();
        dishes.removeIf(dish -> !dish.isAvailable());
        return new ResponseEntity<>(dishes, HttpStatus.OK);
    }
}