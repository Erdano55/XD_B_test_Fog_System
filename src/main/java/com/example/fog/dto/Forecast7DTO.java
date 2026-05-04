package com.example.fog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Forecast7DTO {
    private List<String> dates;
    private List<Integer> maxTemps;
    private List<Integer> minTemps;
    private List<Integer> humidity;
}
