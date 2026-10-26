package com.apextech.biteexpress.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestaurantResponse(

        String id,

        Long restaurantId,

        @JsonProperty("Name")
        String name,

        @JsonProperty("Address")
        String address,

        @JsonProperty("City")
        String city,

        @JsonProperty("Country")
        String country,

        String locality,

        @JsonProperty("Cuisines")
        List<String> cuisines,

        @JsonProperty("Average Rating")
        Double averageRating,

        String ratingText,

        Integer averageCostForTwo,

        String currency,

        Integer priceRange,

        @JsonProperty("Distance")
        Double distance
) {
}
