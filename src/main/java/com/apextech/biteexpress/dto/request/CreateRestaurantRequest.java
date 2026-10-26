package com.apextech.biteexpress.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record CreateRestaurantRequest(

        @NotNull Long restaurantId,

        @NotBlank String name,

        @NotNull Integer countryCode,

        @NotBlank String city,

        @NotBlank String address,

        String locality,

        String localityVerbose,

        @NotNull
        @DecimalMin("-90")
        @DecimalMax("90")
        Double latitude,

        @NotNull
        @DecimalMin("-180")
        @DecimalMax("180")
        Double longitude,

        @NotEmpty List<String> cuisines,

        @PositiveOrZero Integer averageCostForTwo,

        String currency,

        @Min(1)
        @Max(4)
        Integer priceRange,

        Boolean hasTableBooking,

        Boolean hasOnlineDelivery,

        Boolean isDeliveringNow,

        @DecimalMin("0")
        @DecimalMax("5")
        Double aggregateRating,

        String ratingText,

        String ratingColor,

        @PositiveOrZero Integer votes
) {
}
