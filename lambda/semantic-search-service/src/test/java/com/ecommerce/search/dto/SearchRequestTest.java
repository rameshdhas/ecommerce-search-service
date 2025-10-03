package com.ecommerce.search.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class SearchRequestTest {

    @Test
    @DisplayName("Should create SearchRequest with all fields")
    void testSearchRequestCreation() {
        // Given
        String query = "laptop";
        Integer limit = 20;
        Integer offset = 10;
        SearchFilters filters = new SearchFilters("Electronics", 100.0, 2000.0, "Dell");

        // When
        SearchRequest request = new SearchRequest(query, limit, offset, filters);

        // Then
        assertThat(request.query()).isEqualTo(query);
        assertThat(request.limit()).isEqualTo(limit);
        assertThat(request.offset()).isEqualTo(offset);
        assertThat(request.filters()).isEqualTo(filters);
    }

    @Test
    @DisplayName("Should create SearchRequest with default values")
    void testSearchRequestWithDefaults() {
        // Given
        String query = "smartphone";

        // When
        SearchRequest request = new SearchRequest(query);

        // Then
        assertThat(request.query()).isEqualTo(query);
        assertThat(request.limit()).isEqualTo(10); // default
        assertThat(request.offset()).isEqualTo(0); // default
        assertThat(request.filters()).isNull();
    }

    @Test
    @DisplayName("Should handle null values in compact constructor")
    void testCompactConstructorWithNulls() {
        // When
        SearchRequest request = new SearchRequest("test", null, null, null);

        // Then
        assertThat(request.query()).isEqualTo("test");
        assertThat(request.limit()).isEqualTo(10); // default applied
        assertThat(request.offset()).isEqualTo(0); // default applied
        assertThat(request.filters()).isNull();
    }

    @Test
    @DisplayName("Should support equality comparison")
    void testSearchRequestEquality() {
        // Given
        SearchRequest request1 = new SearchRequest("query", 10, 0, null);
        SearchRequest request2 = new SearchRequest("query", 10, 0, null);
        SearchRequest request3 = new SearchRequest("different", 10, 0, null);

        // Then
        assertThat(request1).isEqualTo(request2);
        assertThat(request1).isNotEqualTo(request3);
    }
}