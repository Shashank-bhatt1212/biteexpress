package com.apextech.biteexpress.service;

import com.apextech.biteexpress.dto.ParsedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalLanguageQueryParserTest {

    private final NaturalLanguageQueryParser parser = new NaturalLanguageQueryParser();

    @Test
    @DisplayName("example 1: city + cuisine, no cost, not nearest")
    void example1CityAndCuisine() {
        ParsedQuery result = parser.parse("Best restaurants in Gurgaon with South Indian Food");

        assertThat(result.getCity()).isEqualTo("Gurgaon");
        assertThat(result.getCuisines()).containsExactly("South Indian");
        assertThat(result.getMaxCostForTwo()).isNull();
        assertThat(result.isNearestIntent()).isFalse();
    }

    @Test
    @DisplayName("example 2: city + price cap")
    void example2CityAndPriceCap() {
        ParsedQuery result = parser.parse("Restaurants in Gurgaon under Rs. 1000 for two");

        assertThat(result.getCity()).isEqualTo("Gurgaon");
        assertThat(result.getMaxCostForTwo()).isEqualTo(1000);
        assertThat(result.isNearestIntent()).isFalse();
    }

    @Test
    @DisplayName("example 3: nearest intent with cuisine, no city")
    void example3NearestWithCuisine() {
        ParsedQuery result = parser.parse("Nearest place that serves Vada Pav");

        assertThat(result.isNearestIntent()).isTrue();
        assertThat(result.getCuisines()).containsExactly("Vada Pav");
        assertThat(result.getCity()).isNull();
    }

    @Test
    @DisplayName("nearest phrase 'near me' is not captured as a city")
    void nearMeNotCapturedAsCity() {
        ParsedQuery result = parser.parse("Chinese food near me");

        assertThat(result.isNearestIntent()).isTrue();
        assertThat(result.getCity()).isNull();
        assertThat(result.getCuisines()).containsExactly("Chinese");
    }

    @Test
    @DisplayName("multi-word cuisine is not split into its single-word parts")
    void multiWordCuisineNotSplit() {
        ParsedQuery result = parser.parse("South Indian in Chennai");

        assertThat(result.getCuisines()).containsExactly("South Indian");
        assertThat(result.getCuisines()).doesNotContain("Indian");
        assertThat(result.getCity()).isEqualTo("Chennai");
    }

    @Test
    @DisplayName("price cap variant: 'under Rs. 1000'")
    void priceCapUnderRs() {
        assertThat(parser.parse("under Rs. 1000").getMaxCostForTwo()).isEqualTo(1000);
    }

    @Test
    @DisplayName("price cap variant: 'under 1000 for two'")
    void priceCapUnderForTwo() {
        assertThat(parser.parse("under 1000 for two").getMaxCostForTwo()).isEqualTo(1000);
    }

    @Test
    @DisplayName("price cap variant: 'under \u20B91000'")
    void priceCapUnderRupeeSymbol() {
        assertThat(parser.parse("under \u20B91000").getMaxCostForTwo()).isEqualTo(1000);
    }

    @Test
    @DisplayName("blank query yields empty parse with nearest=false")
    void blankQuery() {
        ParsedQuery result = parser.parse("   ");

        assertThat(result.isNearestIntent()).isFalse();
        assertThat(result.getCity()).isNull();
        assertThat(result.getCuisines()).isNull();
        assertThat(result.getMaxCostForTwo()).isNull();
    }

    @Test
    @DisplayName("null query yields empty parse with nearest=false")
    void nullQuery() {
        ParsedQuery result = parser.parse(null);

        assertThat(result.isNearestIntent()).isFalse();
        assertThat(result.getCity()).isNull();
    }
}
