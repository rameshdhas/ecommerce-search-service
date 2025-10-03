package com.ecommerce.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Product(
    @JsonProperty("id") String id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("price") Double price,
    @JsonProperty("category") String category,
    @JsonProperty("brand") String brand,
    @JsonProperty("imageUrl") String imageUrl,
    @JsonProperty("score") Double score
) {}