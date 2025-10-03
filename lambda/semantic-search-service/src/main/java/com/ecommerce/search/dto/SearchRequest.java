package com.ecommerce.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record SearchRequest(
    @NotBlank(message = "Query cannot be empty")
    @JsonProperty("query") String query,

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @JsonProperty("limit") Integer limit,

    @Min(value = 0, message = "Offset cannot be negative")
    @JsonProperty("offset") Integer offset,

    @JsonProperty("filters") SearchFilters filters
) {
    // Constructor with default values
    public SearchRequest(String query) {
        this(query, 10, 0, null);
    }

    // Constructor without filters
    public SearchRequest(String query, Integer limit, Integer offset) {
        this(query, limit, offset, null);
    }

    // Default constructor with defaults for nullable fields
    public SearchRequest {
        if (limit == null) {
            limit = 10;
        }
        if (offset == null) {
            offset = 0;
        }
    }
}