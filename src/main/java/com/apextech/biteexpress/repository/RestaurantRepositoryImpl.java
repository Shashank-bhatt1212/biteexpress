package com.apextech.biteexpress.repository;

import com.apextech.biteexpress.dto.RestaurantFilter;
import com.apextech.biteexpress.dto.RestaurantWithDistance;
import com.apextech.biteexpress.dto.response.PagedResponse;
import com.apextech.biteexpress.model.Restaurant;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    private static final String COLLECTION = "restaurants";
    private static final String DISTANCE_FIELD = "distance";

    private final MongoTemplate mongoTemplate;

    @Override
    public PagedResponse<Restaurant> search(RestaurantFilter filter) {
        List<Criteria> criteria = buildCriteria(filter);

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(Query.of(query), Restaurant.class);

        query.with(buildSort(filter));
        query.skip((long) filter.getPage() * filter.getSize());
        query.limit(filter.getSize());

        List<Restaurant> content = mongoTemplate.find(query, Restaurant.class);

        return page(content, filter, total);
    }

    @Override
    public PagedResponse<RestaurantWithDistance> searchNear(RestaurantFilter filter) {
        Document geoNear = new Document("near",
                new Document("type", "Point")
                        .append("coordinates", List.of(filter.getLng(), filter.getLat())))
                .append("spherical", true)
                .append("distanceField", DISTANCE_FIELD);
        if (filter.getRadius() != null) {
            geoNear.append("maxDistance", filter.getRadius());
        }
        AggregationOperation geoNearStage = context -> new Document("$geoNear", geoNear);

        List<Criteria> criteria = buildCriteria(filter);
        MatchOperation match = criteria.isEmpty()
                ? Aggregation.match(new Criteria())
                : Aggregation.match(new Criteria().andOperator(criteria.toArray(new Criteria[0])));

        // Page pipeline: $geoNear -> $match -> $sort -> $skip -> $limit
        Aggregation pagePipeline = Aggregation.newAggregation(
                geoNearStage,
                match,
                Aggregation.sort(buildSort(filter)),
                Aggregation.skip((long) filter.getPage() * filter.getSize()),
                Aggregation.limit(filter.getSize()));

        AggregationResults<Document> pageResults =
                mongoTemplate.aggregate(pagePipeline, COLLECTION, Document.class);

        List<RestaurantWithDistance> content = new ArrayList<>();
        for (Document doc : pageResults.getMappedResults()) {
            double distance = doc.get(DISTANCE_FIELD, Number.class).doubleValue();
            Restaurant restaurant = mongoTemplate.getConverter().read(Restaurant.class, doc);
            content.add(RestaurantWithDistance.builder()
                    .restaurant(restaurant)
                    .distance(distance)
                    .build());
        }

        // Count pipeline: $geoNear -> $match -> $count
        Aggregation countPipeline = Aggregation.newAggregation(
                geoNearStage,
                match,
                Aggregation.count().as("total"));

        AggregationResults<Document> countResults =
                mongoTemplate.aggregate(countPipeline, COLLECTION, Document.class);
        Document countDoc = countResults.getUniqueMappedResult();
        long total = countDoc == null ? 0L : countDoc.get("total", Number.class).longValue();

        return page(content, filter, total);
    }

    private List<Criteria> buildCriteria(RestaurantFilter filter) {
        List<Criteria> criteria = new ArrayList<>();

        if (StringUtils.hasText(filter.getName())) {
            criteria.add(Criteria.where("name")
                    .regex(Pattern.quote(filter.getName()), "i"));
        }
        if (StringUtils.hasText(filter.getCity())) {
            criteria.add(Criteria.where("city")
                    .regex("^" + Pattern.quote(filter.getCity()) + "$", "i"));
        }
        if (filter.getMinRating() != null) {
            criteria.add(Criteria.where("aggregateRating").gte(filter.getMinRating()));
        }
        if (filter.getCuisines() != null && !filter.getCuisines().isEmpty()) {
            List<Pattern> cuisinePatterns = new ArrayList<>();
            for (String cuisine : filter.getCuisines()) {
                if (StringUtils.hasText(cuisine)) {
                    cuisinePatterns.add(Pattern.compile("^" + Pattern.quote(cuisine.trim()) + "$",
                            Pattern.CASE_INSENSITIVE));
                }
            }
            if (!cuisinePatterns.isEmpty()) {
                criteria.add(Criteria.where("cuisines").in(cuisinePatterns));
            }
        }
        if (filter.getMaxCostForTwo() != null) {
            criteria.add(Criteria.where("averageCostForTwo").lte(filter.getMaxCostForTwo()));
        }

        return criteria;
    }

    private Sort buildSort(RestaurantFilter filter) {
        String sort = filter.getSort();
        Sort.Direction direction = resolveDirection(filter.getDirection(),
                Sort.Direction.ASC);

        if ("rating".equalsIgnoreCase(sort)) {
            return Sort.by(resolveDirection(filter.getDirection(), Sort.Direction.DESC),
                    "aggregateRating");
        }
        if ("name".equalsIgnoreCase(sort)) {
            return Sort.by(direction, "name");
        }
        if ("distance".equalsIgnoreCase(sort)) {
            return Sort.by(direction, DISTANCE_FIELD);
        }
        // Default: distance asc on geo path, rating desc otherwise.
        if (filter.getLat() != null && filter.getLng() != null) {
            return Sort.by(Sort.Direction.ASC, DISTANCE_FIELD);
        }
        return Sort.by(Sort.Direction.DESC, "aggregateRating");
    }

    private Sort.Direction resolveDirection(String direction, Sort.Direction fallback) {
        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }
        return fallback;
    }

    private <T> PagedResponse<T> page(List<T> content, RestaurantFilter filter, long total) {
        int size = filter.getSize();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponse<>(content, filter.getPage(), size, total, totalPages);
    }
}
