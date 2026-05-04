package com.example.fog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private LocationDTO location;
    private WeatherNowDTO weatherNow;
    private AirQualityDTO airQuality;
    private Forecast7DTO forecast7;
}
