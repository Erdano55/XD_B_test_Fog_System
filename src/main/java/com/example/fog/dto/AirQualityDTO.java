package com.example.fog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityDTO {
    private String category;
    private String aqi;

    @JsonProperty("pm25")
    private String pm25;

    private String so2;
    private String no2;
    private String co;
    private String o3;
}
