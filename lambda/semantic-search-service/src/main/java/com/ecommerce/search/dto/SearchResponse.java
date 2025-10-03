package com.ecommerce.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SearchResponse {

    @JsonProperty("products")
    private List<Product> products;

    @JsonProperty("total")
    private Long total;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("processingTimeMs")
    private Long processingTimeMs;

    public SearchResponse() {}

    public SearchResponse(List<Product> products, Long total, Integer limit, Integer offset, Long processingTimeMs) {
        this.products = products;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
        this.processingTimeMs = processingTimeMs;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
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

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
}