package com.ecommerce.search;

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

    public static class Product {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("price")
        private Double price;

        @JsonProperty("category")
        private String category;

        @JsonProperty("brand")
        private String brand;

        @JsonProperty("imageUrl")
        private String imageUrl;

        @JsonProperty("score")
        private Double score;

        public Product() {}

        public Product(String id, String name, String description, Double price,
                      String category, String brand, String imageUrl, Double score) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.category = category;
            this.brand = brand;
            this.imageUrl = imageUrl;
            this.score = score;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }
    }
}