package com.apextech.biteexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantFilter {

    private String name;
    private String city;
    private Double minRating;
    private List<String> cuisines;
    private Integer maxCostForTwo;
    private Double lat;
    private Double lng;
    private Double radius;
    private String sort;
    private String direction;
    private int page;
    private int size;
}
