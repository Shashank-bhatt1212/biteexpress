package com.apextech.biteexpress.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "restaurants")
public class Restaurant {

    @Id
    private String id;

    private Long restaurantId;
    private String name;
    private Integer countryCode;
    private String city;
    private String address;
    private String locality;
    private String localityVerbose;
    private String country;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;

    private List<String> cuisines;
    private Integer averageCostForTwo;
    private String currency;
    private Integer priceRange;
    private Boolean hasTableBooking;
    private Boolean hasOnlineDelivery;
    private Boolean isDeliveringNow;
    private Double aggregateRating;
    private String ratingText;
    private String ratingColor;
    private Integer votes;
}
