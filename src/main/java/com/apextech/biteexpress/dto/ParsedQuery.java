package com.apextech.biteexpress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuery {

    private String city;
    private List<String> cuisines;
    private Integer maxCostForTwo;
    private boolean nearestIntent;
}
