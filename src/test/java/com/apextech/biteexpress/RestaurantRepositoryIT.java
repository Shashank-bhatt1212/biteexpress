package com.apextech.biteexpress;

import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.RestaurantWithDistance;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.model.Restaurant;
import com.apextech.biteexpress.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RestaurantRepositoryIT {

    @Autowired
    private RestaurantRepository repository;

    @BeforeEach
    void seedFixtures() {
        repository.deleteAll();
        repository.saveAll(deterministicFixtures());
    }

    private List<Restaurant> deterministicFixtures() {
        return List.of(
                Restaurant.builder()
                        .restaurantId(1L)
                        .name("Spice Villa")
                        .city("New Delhi")
                        .location(new GeoJsonPoint(77.2200, 28.6310))
                        .cuisines(List.of("North Indian", "Mughlai"))
                        .averageCostForTwo(800)
                        .aggregateRating(4.5)
                        .build(),
                Restaurant.builder()
                        .restaurantId(2L)
                        .name("Curry House")
                        .city("New Delhi")
                        .location(new GeoJsonPoint(77.2410, 28.6450))
                        .cuisines(List.of("South Indian"))
                        .averageCostForTwo(500)
                        .aggregateRating(4.0)
                        .build(),
                Restaurant.builder()
                        .restaurantId(3L)
                        .name("Bay Breeze")
                        .city("Mumbai")
                        .location(new GeoJsonPoint(72.8777, 19.0760))
                        .cuisines(List.of("Seafood", "Continental"))
                        .averageCostForTwo(1500)
                        .aggregateRating(4.8)
                        .build(),
                Restaurant.builder()
                        .restaurantId(4L)
                        .name("Gurgaon Grill")
                        .city("Gurgaon")
                        .location(new GeoJsonPoint(77.0266, 28.4595))
                        .cuisines(List.of("South Indian", "Chinese"))
                        .averageCostForTwo(900)
                        .aggregateRating(3.9)
                        .build());
    }

    private RestaurantFilter.RestaurantFilterBuilder baseFilter() {
        return RestaurantFilter.builder().page(0).size(20);
    }

    @Test
    @DisplayName("name filter is a case-insensitive partial match")
    void nameFilterPartialCaseInsensitive() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().name("spice").build());

        assertThat(result.content()).extracting(Restaurant::getName)
                .containsExactly("Spice Villa");
    }

    @Test
    @DisplayName("city filter is a case-insensitive exact match")
    void cityFilterExactCaseInsensitive() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().city("new delhi").build());

        assertThat(result.content()).extracting(Restaurant::getCity)
                .containsOnly("New Delhi");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("minRating filter keeps only ratings at or above the threshold")
    void minRatingFilter() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().minRating(4.2).build());

        assertThat(result.content()).extracting(Restaurant::getAggregateRating)
                .allMatch(r -> r >= 4.2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("cuisines filter uses ANY semantics")
    void cuisinesAnyFilter() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().cuisines(List.of("South Indian")).build());

        assertThat(result.content()).extracting(Restaurant::getName)
                .containsExactlyInAnyOrder("Curry House", "Gurgaon Grill");
    }

    @Test
    @DisplayName("combined city + minRating filters")
    void combinedFilters() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().city("New Delhi").minRating(4.2).build());

        assertThat(result.content()).extracting(Restaurant::getName)
                .containsExactly("Spice Villa");
    }

    @Test
    @DisplayName("sort by name ascending")
    void sortByNameAsc() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().sort("name").direction("asc").build());

        assertThat(result.content()).extracting(Restaurant::getName)
                .containsExactly("Bay Breeze", "Curry House", "Gurgaon Grill", "Spice Villa");
    }

    @Test
    @DisplayName("sort by rating descending")
    void sortByRatingDesc() {
        PagedResponse<Restaurant> result =
                repository.search(baseFilter().sort("rating").direction("desc").build());

        assertThat(result.content()).extracting(Restaurant::getName)
                .containsExactly("Bay Breeze", "Spice Villa", "Curry House", "Gurgaon Grill");
    }

    @Test
    @DisplayName("pagination reports totals and limits the page content")
    void paginationTotals() {
        PagedResponse<Restaurant> result = repository.search(
                baseFilter().sort("name").direction("asc").page(0).size(2).build());

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("geo search returns in-range results nearest-first, each with meters distance")
    void geoSearchInRangeNearestFirst() {
        // Reference = Connaught Place, New Delhi; 5km radius keeps the two
        // New Delhi rows and excludes Mumbai/Gurgaon. This exercises the
        // 2dsphere index / auto index creation.
        PagedResponse<RestaurantWithDistance> result = repository.searchNear(
                baseFilter().lat(28.6328).lng(77.2197).radius(5000.0).build());

        assertThat(result.content()).hasSize(2);
        assertThat(result.content())
                .extracting(rwd -> rwd.getRestaurant().getName())
                .containsExactly("Spice Villa", "Curry House");
        assertThat(result.content())
                .extracting(RestaurantWithDistance::getDistance)
                .allMatch(d -> d >= 0 && d <= 5000);
        // Nearest-first ordering by the computed distance.
        assertThat(result.content().get(0).getDistance())
                .isLessThanOrEqualTo(result.content().get(1).getDistance());
    }
}
