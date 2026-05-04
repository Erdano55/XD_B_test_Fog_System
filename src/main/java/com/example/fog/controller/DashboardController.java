package com.example.fog.controller;

import com.example.fog.dto.AirQualityDTO;
import com.example.fog.dto.DashboardDTO;
import com.example.fog.dto.Forecast7DTO;
import com.example.fog.dto.LocationDTO;
import com.example.fog.dto.WeatherNowDTO;
import com.example.fog.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @ResponseBody
    @GetMapping("/api/location")
    public LocationDTO location() {
        return dashboardService.refreshLocation();
    }

    @ResponseBody
    @GetMapping("/api/weather/now")
    public WeatherNowDTO weatherNow() {
        return dashboardService.refreshWeatherNow();
    }

    @ResponseBody
    @GetMapping("/api/air/now")
    public AirQualityDTO airNow() {
        return dashboardService.refreshAirQuality();
    }

    @ResponseBody
    @GetMapping("/api/weather/forecast7")
    public Forecast7DTO forecast7() {
        return dashboardService.refreshForecast7();
    }

    @ResponseBody
    @GetMapping("/api/dashboard")
    public DashboardDTO dashboard() {
        return dashboardService.refreshDashboard();
    }

    @ResponseBody
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServiceException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "type", ex.getClass().getSimpleName()
                ));
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", ex.getMessage() == null ? "Unexpected server error" : ex.getMessage(),
                        "type", ex.getClass().getSimpleName()
                ));
    }
}
