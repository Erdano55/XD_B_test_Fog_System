package com.example.fog.service;

import com.example.fog.dto.AirQualityDTO;
import com.example.fog.dto.DashboardDTO;
import com.example.fog.dto.Forecast7DTO;
import com.example.fog.dto.LocationDTO;
import com.example.fog.dto.WeatherNowDTO;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final LocationService locationService;
    private final WeatherService weatherService;
    private final AirQualityService airQualityService;

    private LocationDTO lastLocation;
    private WeatherNowDTO lastWeatherNow;
    private AirQualityDTO lastAirQuality;
    private Forecast7DTO lastForecast7;

    public DashboardService(LocationService locationService,
                            WeatherService weatherService,
                            AirQualityService airQualityService) {
        this.locationService = locationService;
        this.weatherService = weatherService;
        this.airQualityService = airQualityService;
    }

    public synchronized LocationDTO refreshLocation() {
        lastLocation = locationService.getLocationByIp();
        return lastLocation;
    }

    public synchronized WeatherNowDTO refreshWeatherNow() {
        LocationDTO location = ensureLocation();
        lastWeatherNow = weatherService.getNowWeather(location);
        return lastWeatherNow;
    }

    public synchronized AirQualityDTO refreshAirQuality() {
        LocationDTO location = ensureLocation();
        lastAirQuality = airQualityService.getNowAirQuality(location);
        return lastAirQuality;
    }

    public synchronized Forecast7DTO refreshForecast7() {
        lastForecast7 = weatherService.getForecast7();
        return lastForecast7;
    }

    public synchronized DashboardDTO refreshDashboard() {
        LocationDTO location = refreshLocation();
        WeatherNowDTO weatherNow = weatherService.getNowWeather(location);
        AirQualityDTO airQuality = airQualityService.getNowAirQuality(location);
        Forecast7DTO forecast7 = weatherService.getForecast7();

        lastWeatherNow = weatherNow;
        lastAirQuality = airQuality;
        lastForecast7 = forecast7;

        return DashboardDTO.builder()
                .location(lastLocation)
                .weatherNow(lastWeatherNow)
                .airQuality(lastAirQuality)
                .forecast7(lastForecast7)
                .build();
    }

    private LocationDTO ensureLocation() {
        if (lastLocation == null) {
            return refreshLocation();
        }
        return lastLocation;
    }
}
