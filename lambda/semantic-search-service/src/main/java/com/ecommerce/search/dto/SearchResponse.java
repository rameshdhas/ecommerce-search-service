package com.ecommerce.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SearchResponse(
    @JsonProperty("products") List<Product> products,
    @JsonProperty("total") Long total,
    @JsonProperty("limit") Integer limit,
    @JsonProperty("offset") Integer offset,
    @JsonProperty("processingTimeMs") Long processingTimeMs
) {}