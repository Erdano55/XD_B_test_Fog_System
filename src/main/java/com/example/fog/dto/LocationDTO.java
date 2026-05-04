package com.example.fog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private Double lon;
    private Double lat;
    private String city;
    private String rawCity;
    private String regionName;
    private String country;
    private String ip;
}
