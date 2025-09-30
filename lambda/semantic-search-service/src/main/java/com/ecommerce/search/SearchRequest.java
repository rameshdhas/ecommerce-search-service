package com.ecommerce.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class SearchRequest {

    @NotBlank(message = "Query cannot be empty")
    @JsonProperty("query")
    private String query;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit cannot exceed 100")
    @JsonProperty("limit")
    private Integer limit = 10;

    @Min(value = 0, message = "Offset cannot be negative")
    @JsonProperty("offset")
    private Integer offset = 0;

    @JsonProperty("filters")
    private SearchFilters filters;

    public SearchRequest() {}

    public SearchRequest(String query, Integer limit, Integer offset, SearchFilters filters) {
        this.query = query;
        this.limit = limit;
        this.offset = offset;
        this.filters = filters;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public SearchFilters getFilters() {
        return filters;
    }

    public void setFilters(SearchFilters filters) {
        this.filters = filters;
    }

    public static class SearchFilters {
        @JsonProperty("category")
        private String category;

        @JsonProperty("priceMin")
        private Double priceMin;

        @JsonProperty("priceMax")
        private Double priceMax;

        @JsonProperty("brand")
        private String brand;

        public SearchFilters() {}

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public Double getPriceMin() {
            return priceMin;
        }

        public void setPriceMin(Double priceMin) {
            this.priceMin = priceMin;
        }

        public Double getPriceMax() {
            return priceMax;
        }

        public void setPriceMax(Double priceMax) {
            this.priceMax = priceMax;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }
    }
}