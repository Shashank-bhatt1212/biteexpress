package com.apextech.biteexpress.util;

import java.util.Map;

public final class CountryCodeMapper {

    private static final Map<Integer, String> COUNTRY_NAMES = Map.ofEntries(
            Map.entry(1, "India"),
            Map.entry(14, "Australia"),
            Map.entry(30, "Brazil"),
            Map.entry(37, "Canada"),
            Map.entry(94, "Indonesia"),
            Map.entry(148, "New Zealand"),
            Map.entry(162, "Philippines"),
            Map.entry(166, "Qatar"),
            Map.entry(184, "Singapore"),
            Map.entry(189, "South Africa"),
            Map.entry(191, "Sri Lanka"),
            Map.entry(208, "Turkey"),
            Map.entry(214, "UAE"),
            Map.entry(215, "United Kingdom"),
            Map.entry(216, "United States")
    );

    private CountryCodeMapper() {
    }

    public static String resolve(Integer code) {
        return COUNTRY_NAMES.getOrDefault(code, "Unknown");
    }
}
