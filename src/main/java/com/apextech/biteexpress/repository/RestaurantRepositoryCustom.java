package com.apextech.biteexpress.repository;

import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.RestaurantWithDistance;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.model.Restaurant;

public interface RestaurantRepositoryCustom {

    /**
     * Non-distance search path. Builds a dynamic {@code Criteria}/{@code Query}
     * from the supplied filter, then applies sort and pagination.
     */
    PagedResponse<Restaurant> search(RestaurantFilter filter);

    /**
     * Distance-aware search path. Runs a {@code $geoNear} aggregation (distance
     * in meters) against the 2dsphere index, filtering, computing distance and
     * paging in two pipelines.
     */
    PagedResponse<RestaurantWithDistance> searchNear(RestaurantFilter filter);
}
