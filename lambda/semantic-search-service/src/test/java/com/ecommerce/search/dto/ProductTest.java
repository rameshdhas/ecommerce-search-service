package com.ecommerce.search.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    @Test
    @DisplayName("Should create Product record with all fields")
    void testProductCreation() {
        // Given
        String id = "1";
        String name = "iPhone 14";
        String description = "Latest iPhone";
        Double price = 999.99;
        String category = "Electronics";
        String brand = "Apple";
        String imageUrl = "http://example.com/iphone.jpg";
        Double score = 0.95;

        // When
        Product product = new Product(id, name, description, price, category, brand, imageUrl, score);

        // Then
        assertThat(product.id()).isEqualTo(id);
        assertThat(product.name()).isEqualTo(name);
        assertThat(product.description()).isEqualTo(description);
        assertThat(product.price()).isEqualTo(price);
        assertThat(product.category()).isEqualTo(category);
        assertThat(product.brand()).isEqualTo(brand);
        assertThat(product.imageUrl()).isEqualTo(imageUrl);
        assertThat(product.score()).isEqualTo(score);
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testProductEquality() {
        // Given
        Product product1 = new Product("1", "Test", "Description", 100.0, "Cat", "Brand", "url", 0.8);
        Product product2 = new Product("1", "Test", "Description", 100.0, "Cat", "Brand", "url", 0.8);
        Product product3 = new Product("2", "Test", "Description", 100.0, "Cat", "Brand", "url", 0.8);

        // Then
        assertThat(product1).isEqualTo(product2);
        assertThat(product1).isNotEqualTo(product3);
    }

    @Test
    @DisplayName("Should generate toString representation")
    void testProductToString() {
        // Given
        Product product = new Product("1", "Test Product", "Description", 50.0, "Category", "Brand", "url", 0.9);

        // When
        String toString = product.toString();

        // Then
        assertThat(toString).contains("Test Product");
        assertThat(toString).contains("50.0");
        assertThat(toString).contains("Category");
    }
}