package com.apextech.biteexpress.service;

import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.request.CreateRestaurantRequest;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.dto.response.RestaurantResponse;

public interface RestaurantService {

    RestaurantResponse create(CreateRestaurantRequest request);

    RestaurantResponse getById(String id);

    PagedResponse<RestaurantResponse> list(RestaurantFilter filter);

    PagedResponse<RestaurantResponse> search(String q, RestaurantFilter filter);
}
