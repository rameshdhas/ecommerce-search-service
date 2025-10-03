package com.ecommerce.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SearchFilters(
    @JsonProperty("category") String category,
    @JsonProperty("priceMin") Double priceMin,
    @JsonProperty("priceMax") Double priceMax,
    @JsonProperty("brand") String brand
) {}