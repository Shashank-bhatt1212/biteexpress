package com.apextech.biteexpress.dto;

import com.apextech.biteexpress.model.Restaurant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantWithDistance {

    private Restaurant restaurant;
    private double distance;
}
