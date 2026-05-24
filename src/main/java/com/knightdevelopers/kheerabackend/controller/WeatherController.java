package com.knightdevelopers.kheerabackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private String currentWeather="Sunny";

    @GetMapping
    public String getWeather(){
        return currentWeather;
    }

    @PostMapping
    public ResponseEntity <?> registerWeather(@RequestBody String weather){
        this.currentWeather=weather;
        return  ResponseEntity.ok("");
    }

}
