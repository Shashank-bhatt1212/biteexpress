package com.apextech.biteexpress.service;

import com.apextech.biteexpress.dto.ParsedQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local, dependency-free natural-language query parser. Extracts a city,
 * cuisine keywords, a cost cap and a "nearest" intent from a free-text query
 * using only regular expressions and small static keyword sets. No database
 * lookups and no network calls, so parsing is deterministic and fast.
 */
@Component
public class NaturalLanguageQueryParser {

    /** Prominent dataset cities used to cross-check a captured city candidate. */
    private static final Set<String> KNOWN_CITIES = Set.of(
            "gurgaon", "new delhi", "noida", "faridabad", "ghaziabad",
            "mumbai", "bangalore", "chennai", "hyderabad", "kolkata",
            "pune", "ahmedabad", "jaipur", "lucknow", "makati city",
            "pasay city", "mandaluyong city", "dubai", "abu dhabi",
            "doha", "singapore", "london", "birmingham"
    );

    /**
     * Known cuisine / dish keywords. Multi-word entries are checked before
     * single words so e.g. "South Indian" is not split into "Indian".
     */
    private static final List<String> KNOWN_CUISINES = List.of(
            "North Indian", "South Indian", "Modern Indian", "Vada Pav",
            "Street Food", "Fast Food", "Ice Cream", "Bubble Tea",
            "Chinese", "Italian", "Japanese", "Mexican", "Thai",
            "Continental", "Mughlai", "Bakery", "Desserts", "Cafe",
            "Pizza", "Burger", "Biryani", "Seafood", "Mithai", "Indian"
    );

    private static final Pattern CITY_PATTERN = Pattern.compile(
            "\\b(?:in|near)\\s+([a-z][a-z ]*?)(?=\\s+(?:with|under|that|serving|serves|for)\\b|$)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "under\\s+(?:rs\\.?|₹)?\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NEAREST_PATTERN = Pattern.compile(
            "near me|nearby|nearest|closest",
            Pattern.CASE_INSENSITIVE);

    public ParsedQuery parse(String q) {
        ParsedQuery.ParsedQueryBuilder builder = ParsedQuery.builder();

        if (q == null || q.isBlank()) {
            return builder.nearestIntent(false).build();
        }

        String working = q;

        // Rule 4 — nearest intent. Detect first, then strip the phrases so the
        // city regex below can never capture "me" as a city.
        boolean nearestIntent = NEAREST_PATTERN.matcher(working).find();
        if (nearestIntent) {
            working = NEAREST_PATTERN.matcher(working).replaceAll(" ");
        }
        builder.nearestIntent(nearestIntent);

        // Rule 2 — cuisines (multi-word before single-word, ANY semantics).
        builder.cuisines(extractCuisines(q));

        // Rule 3 — price cap.
        Matcher priceMatcher = PRICE_PATTERN.matcher(q);
        if (priceMatcher.find()) {
            builder.maxCostForTwo(Integer.valueOf(priceMatcher.group(1)));
        }

        // Rule 1 — city (run on the nearest-stripped text).
        builder.city(extractCity(working));

        return builder.build();
    }

    private List<String> extractCuisines(String q) {
        String haystack = q.toLowerCase();
        List<String> found = new ArrayList<>();
        for (String cuisine : KNOWN_CUISINES) {
            String needle = cuisine.toLowerCase();
            if (containsWord(haystack, needle) && !alreadyCovered(found, cuisine)) {
                found.add(cuisine);
            }
        }
        return found.isEmpty() ? null : found;
    }

    /**
     * Avoids adding a shorter cuisine that is a substring of one already
     * matched (e.g. skip "Indian" once "South Indian" is captured).
     */
    private boolean alreadyCovered(List<String> found, String candidate) {
        String lower = candidate.toLowerCase();
        for (String existing : found) {
            if (existing.toLowerCase().contains(lower)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWord(String haystack, String needle) {
        Pattern p = Pattern.compile("\\b" + Pattern.quote(needle) + "\\b");
        return p.matcher(haystack).find();
    }

    private String extractCity(String q) {
        Matcher matcher = CITY_PATTERN.matcher(q);
        if (!matcher.find()) {
            return null;
        }
        String candidate = matcher.group(1).trim();
        if (candidate.isEmpty() || "me".equalsIgnoreCase(candidate)) {
            return null;
        }
        // Cross-check against the known-city set. A recognised city is kept as
        // captured; an unknown candidate is still passed through best-effort
        // (the downstream case-insensitive exact match degrades gracefully).
        if (KNOWN_CITIES.contains(candidate.toLowerCase())) {
            return candidate;
        }
        return candidate;
    }
}
