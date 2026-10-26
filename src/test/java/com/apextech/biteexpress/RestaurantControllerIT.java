package com.apextech.biteexpress;

import com.apextech.biteexpress.model.Restaurant;
import com.apextech.biteexpress.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RestaurantControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepository repository;

    @BeforeEach
    void seedFixtures() {
        repository.deleteAll();
        repository.saveAll(deterministicFixtures());
    }

    /**
     * Small hand-built set with known coords/ratings/cuisines/cities so that
     * every assertion below is stable and independent of the bundled CSV.
     * Connaught Place (New Delhi) is the geo reference point used by the
     * distance tests; Spice Villa / Curry House / Bay Breeze are placed at
     * increasing distances from it, and two Gurgaon rows exercise city/name
     * filtering away from the geo cluster.
     */
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
                        .build(),
                Restaurant.builder()
                        .restaurantId(5L)
                        .name("Gurgaon Garden")
                        .city("Gurgaon")
                        .location(new GeoJsonPoint(77.0366, 28.4695))
                        .cuisines(List.of("North Indian"))
                        .averageCostForTwo(1200)
                        .aggregateRating(4.2)
                        .build());
    }

    // --- POST create ---

    @Test
    @DisplayName("POST valid body -> 201 with body and Location header")
    void createReturns201() throws Exception {
        String body = """
                {
                  "restaurantId": 100,
                  "name": "Fresh Bites",
                  "countryCode": 1,
                  "city": "Pune",
                  "address": "12 MG Road",
                  "latitude": 18.5204,
                  "longitude": 73.8567,
                  "cuisines": ["Cafe"],
                  "averageCostForTwo": 600,
                  "aggregateRating": 4.1
                }
                """;

        mockMvc.perform(post("/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        matchesPattern(".*/v1/restaurants/[a-f0-9]+$")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.Name").value("Fresh Bites"))
                .andExpect(jsonPath("$.City").value("Pune"))
                .andExpect(jsonPath("$.Country").value("India"));
    }

    @Test
    @DisplayName("POST invalid body -> 400 with fieldErrors (bad latitude, rating>5, missing name)")
    void createInvalidReturns400() throws Exception {
        String body = """
                {
                  "restaurantId": 101,
                  "countryCode": 1,
                  "city": "Pune",
                  "address": "12 MG Road",
                  "latitude": 120.0,
                  "longitude": 73.8567,
                  "cuisines": ["Cafe"],
                  "aggregateRating": 7.5
                }
                """;

        mockMvc.perform(post("/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        everyItem(notNullValue())));
    }

    // --- GET by id ---

    @Test
    @DisplayName("GET /{id} hit -> 200")
    void getByIdHit() throws Exception {
        String id = repository.findAll().getFirst().getId();

        mockMvc.perform(get("/v1/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("GET /{id} miss -> 404 with ErrorResponse shape")
    void getByIdMiss() throws Exception {
        mockMvc.perform(get("/v1/restaurants/{id}", "doesnotexist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("doesnotexist")))
                .andExpect(jsonPath("$.path").value("/v1/restaurants/doesnotexist"))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // --- Listing: individual filters ---

    @Test
    @DisplayName("filter by name (partial, case-insensitive)")
    void filterByNamePartialCaseInsensitive() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("name", "gurgaon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].Name",
                        everyItem(containsString("Gurgaon"))));
    }

    @Test
    @DisplayName("filter by city (exact, case-insensitive)")
    void filterByCityExactCaseInsensitive() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("city", "new delhi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].City",
                        everyItem(matchesPattern("(?i)new delhi"))));
    }

    @Test
    @DisplayName("filter by minRating")
    void filterByMinRating() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("minRating", "4.2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].['Average Rating']",
                        everyItem(greaterThanOrEqualTo(4.2))));
    }

    @Test
    @DisplayName("filter by cuisines uses ANY semantics")
    void filterByCuisinesAny() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("cuisines", "South Indian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    @DisplayName("combined filters: city + cuisines")
    void combinedFilters() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("city", "Gurgaon")
                        .param("cuisines", "South Indian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].Name").value("Gurgaon Grill"));
    }

    // --- Listing: sorting ---

    @Test
    @DisplayName("sort by name ascending")
    void sortByNameAsc() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("sort", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].Name").value("Bay Breeze"))
                .andExpect(jsonPath("$.content[4].Name").value("Spice Villa"));
    }

    @Test
    @DisplayName("sort by name descending")
    void sortByNameDesc() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("sort", "name")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].Name").value("Spice Villa"))
                .andExpect(jsonPath("$.content[4].Name").value("Bay Breeze"));
    }

    @Test
    @DisplayName("sort by rating ascending")
    void sortByRatingAsc() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("sort", "rating")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].Name").value("Gurgaon Grill"))
                .andExpect(jsonPath("$.content[4].Name").value("Bay Breeze"));
    }

    @Test
    @DisplayName("sort by rating descending (default direction)")
    void sortByRatingDesc() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].Name").value("Bay Breeze"))
                .andExpect(jsonPath("$.content[4].Name").value("Gurgaon Grill"));
    }

    // --- Listing: pagination ---

    @Test
    @DisplayName("pagination returns totals and the requested page")
    void paginationTotals() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("sort", "name")
                        .param("direction", "asc")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("second page returns the next slice")
    void paginationSecondPage() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("sort", "name")
                        .param("direction", "asc")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    @DisplayName("size is capped at 100")
    void sizeCappedAt100() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    // --- Distance ---

    @Test
    @DisplayName("lat/lng + radius: results in-range, each carries a distance, nearest-first")
    void distanceWithinRadius() throws Exception {
        // Reference = Connaught Place, New Delhi. Spice Villa and Curry House
        // are within ~5km; the Mumbai and Gurgaon rows are well outside.
        mockMvc.perform(get("/v1/restaurants")
                        .param("lat", "28.6328")
                        .param("lng", "77.2197")
                        .param("radius", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].Distance",
                        everyItem(notNullValue())))
                .andExpect(jsonPath("$.content[0].Name").value("Spice Villa"))
                .andExpect(jsonPath("$.content[1].Name").value("Curry House"));
    }

    @Test
    @DisplayName("distance field is reported in meters near zero for a co-located point")
    void distanceIsMeters() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("lat", "28.6310")
                        .param("lng", "77.2200")
                        .param("radius", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].Name").value("Spice Villa"))
                .andExpect(jsonPath("$.content[0].Distance", closeTo(0.0, 5.0)));
    }

    @Test
    @DisplayName("sort=distance without coords -> 400")
    void sortDistanceWithoutCoords() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("sort", "distance"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("lat")));
    }

    // --- Query-param range validation (clean 400, never 500) ---

    @Test
    @DisplayName("negative radius -> 400, not 500")
    void negativeRadiusReturns400() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("lat", "28.6328")
                        .param("lng", "77.2197")
                        .param("radius", "-100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("radius")));
    }

    @Test
    @DisplayName("zero radius -> 400, not 500")
    void zeroRadiusReturns400() throws Exception {
        mockMvc.perform(get("/v1/restaurants")
                        .param("lat", "28.6328")
                        .param("lng", "77.2197")
                        .param("radius", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("radius")));
    }

    @Test
    @DisplayName("minRating above 5.0 -> 400")
    void minRatingAboveMaxReturns400() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("minRating", "6.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("minRating")));
    }

    @Test
    @DisplayName("minRating below 0.0 -> 400")
    void minRatingBelowMinReturns400() throws Exception {
        mockMvc.perform(get("/v1/restaurants").param("minRating", "-1.0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("minRating")));
    }

    // --- /search (NL §4 worked examples) ---

    @Test
    @DisplayName("search example 1: city + South Indian filters take effect")
    void searchCityAndCuisine() throws Exception {
        mockMvc.perform(get("/v1/restaurants/search")
                        .param("q", "Best restaurants in Gurgaon with South Indian Food"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].Name").value("Gurgaon Grill"));
    }

    @Test
    @DisplayName("search example 2: city + price cap filters take effect")
    void searchCityAndPriceCap() throws Exception {
        mockMvc.perform(get("/v1/restaurants/search")
                        .param("q", "Restaurants in Gurgaon under Rs. 1000 for two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].Name").value("Gurgaon Grill"));
    }

    @Test
    @DisplayName("search example 3: nearest-without-coords falls back to rating sort")
    void searchNearestFallsBackToRating() throws Exception {
        // "Nearest ... South Indian" with no coords: nearest intent degrades to
        // rating desc while still applying the cuisine filter. Two South Indian
        // rows exist; Curry House (4.0) outranks Gurgaon Grill (3.9).
        mockMvc.perform(get("/v1/restaurants/search")
                        .param("q", "Nearest place that serves South Indian"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].Name").value("Curry House"))
                .andExpect(jsonPath("$.content[1].Name").value("Gurgaon Grill"));
    }
}
