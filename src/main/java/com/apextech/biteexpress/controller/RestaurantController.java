package com.apextech.biteexpress.controller;

import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.request.CreateRestaurantRequest;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.dto.response.RestaurantResponse;
import com.apextech.biteexpress.service.RestaurantService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/v1/restaurants")
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody CreateRestaurantRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        RestaurantResponse created = restaurantService.create(request);
        URI location = uriBuilder.path("/v1/restaurants/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(restaurantService.getById(id));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<RestaurantResponse>> list(RestaurantFilter filter) {
        return ResponseEntity.ok(restaurantService.list(filter));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<RestaurantResponse>> search(@RequestParam @NotBlank String q,
                                                                    RestaurantFilter filter) {
        return ResponseEntity.ok(restaurantService.search(q, filter));
    }
}
