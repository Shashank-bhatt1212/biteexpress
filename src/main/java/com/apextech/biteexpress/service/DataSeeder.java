package com.apextech.biteexpress.service;

import com.apextech.biteexpress.model.Restaurant;
import com.apextech.biteexpress.repository.RestaurantRepository;
import com.apextech.biteexpress.util.CountryCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RestaurantRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Database is empty. Seeding data from zomato.csv...");
            seedDatabase();
        } else {
            log.info("Database already contains data. Skipping seeding.");
        }
    }

    private void seedDatabase() {
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/zomato.csv")))) {

            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);

            List<Restaurant> restaurants = new ArrayList<>();

            for (CSVRecord record : records) {
                try {
                    // Extract coordinates
                    double longitude = Double.parseDouble(record.get("Longitude"));
                    double latitude = Double.parseDouble(record.get("Latitude"));

                    // Skip invalid coordinates (0.0, 0.0 is common in dirty data)
                    GeoJsonPoint location = (longitude != 0.0 && latitude != 0.0)
                            ? new GeoJsonPoint(longitude, latitude)
                            : null;

                    int countryCode = Integer.parseInt(record.get("Country Code"));

                    Restaurant restaurant = Restaurant.builder()
                            .restaurantId(Long.parseLong(record.get("Restaurant ID")))
                            .name(record.get("Restaurant Name"))
                            .countryCode(countryCode)
                            .country(CountryCodeMapper.resolve(countryCode))
                            .city(record.get("City"))
                            .address(record.get("Address"))
                            .locality(record.get("Locality"))
                            .localityVerbose(record.get("Locality Verbose"))
                            .location(location)
                            .cuisines(Arrays.asList(record.get("Cuisines").split(",\\s*")))
                            .averageCostForTwo(Integer.parseInt(record.get("Average Cost for two")))
                            .currency(record.get("Currency"))
                            .priceRange(Integer.parseInt(record.get("Price range")))
                            .hasTableBooking(record.get("Has Table booking").equalsIgnoreCase("Yes"))
                            .hasOnlineDelivery(record.get("Has Online delivery").equalsIgnoreCase("Yes"))
                            .isDeliveringNow(record.get("Is delivering now").equalsIgnoreCase("Yes"))
                            .aggregateRating(Double.parseDouble(record.get("Aggregate rating")))
                            .ratingText(record.get("Rating text"))
                            .ratingColor(record.get("Rating color"))
                            .votes(Integer.parseInt(record.get("Votes")))
                            .build();

                    restaurants.add(restaurant);
                } catch (Exception e) {
                    log.warn("Skipping record due to parsing error: {}", record.get("Restaurant ID"), e);
                }
            }

            repository.saveAll(restaurants);
            log.info("Successfully seeded {} restaurants.", restaurants.size());

        } catch (Exception e) {
            log.error("Failed to seed database.", e);
        }
    }
}