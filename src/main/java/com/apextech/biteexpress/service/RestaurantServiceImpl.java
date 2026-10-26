package com.apextech.biteexpress.service;

import com.apextech.biteexpress.dto.ParsedQuery;
import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.RestaurantWithDistance;
import com.apextech.biteexpress.dto.request.CreateRestaurantRequest;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.dto.response.RestaurantResponse;
import com.apextech.biteexpress.exception.InvalidQueryException;
import com.apextech.biteexpress.exception.RestaurantNotFoundException;
import com.apextech.biteexpress.model.Restaurant;
import com.apextech.biteexpress.repository.RestaurantRepository;
import com.apextech.biteexpress.util.CountryCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final double MIN_RATING = 0.0;
    private static final double MAX_RATING = 5.0;
    private static final Set<String> VALID_SORTS = Set.of("name", "rating", "distance");
    private static final Set<String> VALID_DIRECTIONS = Set.of("asc", "desc");

    private final RestaurantRepository repository;
    private final NaturalLanguageQueryParser queryParser;

    @Override
    public RestaurantResponse create(CreateRestaurantRequest request) {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(request.restaurantId())
                .name(request.name())
                .countryCode(request.countryCode())
                .country(CountryCodeMapper.resolve(request.countryCode()))
                .city(request.city())
                .address(request.address())
                .locality(request.locality())
                .localityVerbose(request.localityVerbose())
                .location(new GeoJsonPoint(request.longitude(), request.latitude()))
                .cuisines(request.cuisines())
                .averageCostForTwo(request.averageCostForTwo())
                .currency(request.currency())
                .priceRange(request.priceRange())
                .hasTableBooking(request.hasTableBooking())
                .hasOnlineDelivery(request.hasOnlineDelivery())
                .isDeliveringNow(request.isDeliveringNow())
                .aggregateRating(request.aggregateRating())
                .ratingText(request.ratingText())
                .ratingColor(request.ratingColor())
                .votes(request.votes())
                .build();

        Restaurant saved = repository.save(restaurant);
        return toResponse(saved);
    }

    @Override
    public RestaurantResponse getById(String id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    @Override
    public PagedResponse<RestaurantResponse> list(RestaurantFilter filter) {
        normalize(filter);

        boolean geo = filter.getLat() != null && filter.getLng() != null;

        if ("distance".equalsIgnoreCase(filter.getSort()) && !geo) {
            throw new InvalidQueryException(
                    "sort=distance requires both lat and lng query parameters");
        }

        if (geo) {
            PagedResponse<RestaurantWithDistance> page = repository.searchNear(filter);
            return mapGeo(page);
        }

        PagedResponse<Restaurant> page = repository.search(filter);
        return mapPlain(page);
    }

    @Override
    public PagedResponse<RestaurantResponse> search(String q, RestaurantFilter filter) {
        ParsedQuery parsed = queryParser.parse(q);
        merge(parsed, filter);

        boolean geo = filter.getLat() != null && filter.getLng() != null;
        if (parsed.isNearestIntent()) {
            if (geo) {
                if (filter.getSort() == null) {
                    filter.setSort("distance");
                }
            } else {
                // Soft NL intent without coordinates: fall back to rating sort
                // rather than erroring, still honouring the other filters.
                log.debug("Nearest intent without coordinates; falling back to rating sort");
                filter.setSort("rating");
                filter.setDirection("desc");
            }
        }

        return list(filter);
    }

    /**
     * Merges parsed NL values into the filter. Explicit structured params take
     * precedence, so a parsed value is only applied when the structured field
     * is empty.
     */
    private void merge(ParsedQuery parsed, RestaurantFilter filter) {
        if (filter.getCity() == null && parsed.getCity() != null) {
            filter.setCity(parsed.getCity());
        }
        if ((filter.getCuisines() == null || filter.getCuisines().isEmpty())
                && parsed.getCuisines() != null) {
            filter.setCuisines(parsed.getCuisines());
        }
        if (filter.getMaxCostForTwo() == null && parsed.getMaxCostForTwo() != null) {
            filter.setMaxCostForTwo(parsed.getMaxCostForTwo());
        }
    }

    private void normalize(RestaurantFilter filter) {
        if (filter.getPage() < 0) {
            throw new InvalidQueryException("page must be zero or greater");
        }
        if (filter.getSize() <= 0) {
            filter.setSize(DEFAULT_PAGE_SIZE);
        }
        if (filter.getSize() > MAX_PAGE_SIZE) {
            filter.setSize(MAX_PAGE_SIZE);
        }
        if (filter.getSort() != null && !VALID_SORTS.contains(filter.getSort().toLowerCase())) {
            throw new InvalidQueryException("Invalid sort value: " + filter.getSort());
        }
        if (filter.getDirection() != null
                && !VALID_DIRECTIONS.contains(filter.getDirection().toLowerCase())) {
            throw new InvalidQueryException("Invalid direction value: " + filter.getDirection());
        }
        if (filter.getMinRating() != null
                && (filter.getMinRating() < MIN_RATING || filter.getMinRating() > MAX_RATING)) {
            throw new InvalidQueryException(
                    "minRating must be between " + MIN_RATING + " and " + MAX_RATING);
        }
        if (filter.getRadius() != null && filter.getRadius() <= 0) {
            throw new InvalidQueryException("radius must be greater than 0");
        }
    }

    private PagedResponse<RestaurantResponse> mapPlain(PagedResponse<Restaurant> page) {
        List<RestaurantResponse> content = page.content().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(content, page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }

    private PagedResponse<RestaurantResponse> mapGeo(PagedResponse<RestaurantWithDistance> page) {
        List<RestaurantResponse> content = page.content().stream()
                .map(this::toResponse)
                .toList();
        return new PagedResponse<>(content, page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return toResponse(r, null);
    }

    private RestaurantResponse toResponse(RestaurantWithDistance rwd) {
        return toResponse(rwd.getRestaurant(), rwd.getDistance());
    }

    private RestaurantResponse toResponse(Restaurant r, Double distance) {
        return new RestaurantResponse(
                r.getId(),
                r.getRestaurantId(),
                r.getName(),
                r.getAddress(),
                r.getCity(),
                r.getCountry(),
                r.getLocality(),
                r.getCuisines(),
                r.getAggregateRating(),
                r.getRatingText(),
                r.getAverageCostForTwo(),
                r.getCurrency(),
                r.getPriceRange(),
                distance);
    }
}
