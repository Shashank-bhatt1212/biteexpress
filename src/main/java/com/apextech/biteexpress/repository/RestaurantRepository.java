package com.apextech.biteexpress.repository;

import com.apextech.biteexpress.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String>, RestaurantRepositoryCustom {
}
