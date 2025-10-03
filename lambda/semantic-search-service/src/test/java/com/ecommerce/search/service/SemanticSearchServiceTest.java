package com.ecommerce.search.service;

import com.ecommerce.search.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SemanticSearchServiceTest {

    @Mock
    private VectorDatabaseService vectorDatabaseService;

    @InjectMocks
    private SemanticSearchService semanticSearchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should successfully perform semantic search with results")
    void testSearchWithResults() {
        // Given
        String query = "wireless headphones";
        SearchRequest request = new SearchRequest(query, 10, 0, null);

        List<Product> mockProducts = Arrays.asList(
            new Product("1", "Sony WH-1000XM4", "Premium wireless headphones", 299.99,
                       "Electronics", "Sony", "http://example.com/sony.jpg", 0.95),
            new Product("2", "Bose QuietComfort", "Noise cancelling headphones", 279.99,
                       "Electronics", "Bose", "http://example.com/bose.jpg", 0.92)
        );

        when(vectorDatabaseService.semanticSearch(query, 10, 0, null))
            .thenReturn(mockProducts);
        when(vectorDatabaseService.getTotalCount(query, null))
            .thenReturn(2L);

        // When
        SearchResponse response = semanticSearchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(2);
        assertThat(response.total()).isEqualTo(2L);
        assertThat(response.limit()).isEqualTo(10);
        assertThat(response.offset()).isEqualTo(0);
        assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);

        verify(vectorDatabaseService).semanticSearch(query, 10, 0, null);
        verify(vectorDatabaseService).getTotalCount(query, null);
    }

    @Test
    @DisplayName("Should handle search with filters")
    void testSearchWithFilters() {
        // Given
        String query = "laptop";
        SearchFilters filters = new SearchFilters("Computers", 500.0, 1500.0, "Dell");
        SearchRequest request = new SearchRequest(query, 5, 0, filters);

        List<Product> mockProducts = Arrays.asList(
            new Product("3", "Dell XPS 13", "Ultrabook laptop", 999.99,
                       "Computers", "Dell", "http://example.com/dell.jpg", 0.88)
        );

        when(vectorDatabaseService.semanticSearch(query, 5, 0, filters))
            .thenReturn(mockProducts);
        when(vectorDatabaseService.getTotalCount(query, filters))
            .thenReturn(1L);

        // When
        SearchResponse response = semanticSearchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).name()).isEqualTo("Dell XPS 13");
        assertThat(response.total()).isEqualTo(1L);

        verify(vectorDatabaseService).semanticSearch(query, 5, 0, filters);
        verify(vectorDatabaseService).getTotalCount(query, filters);
    }

    @Test
    @DisplayName("Should handle empty results")
    void testSearchWithEmptyResults() {
        // Given
        String query = "non-existent product xyz123";
        SearchRequest request = new SearchRequest(query, 10, 0, null);

        when(vectorDatabaseService.semanticSearch(query, 10, 0, null))
            .thenReturn(Arrays.asList());
        when(vectorDatabaseService.getTotalCount(query, null))
            .thenReturn(0L);

        // When
        SearchResponse response = semanticSearchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);

        verify(vectorDatabaseService).semanticSearch(query, 10, 0, null);
        verify(vectorDatabaseService).getTotalCount(query, null);
    }

    @Test
    @DisplayName("Should handle pagination")
    void testSearchWithPagination() {
        // Given
        String query = "smartphone";
        SearchRequest request = new SearchRequest(query, 20, 10, null);

        List<Product> mockProducts = Arrays.asList(
            new Product("4", "iPhone 14", "Latest iPhone", 999.99,
                       "Electronics", "Apple", "http://example.com/iphone.jpg", 0.97)
        );

        when(vectorDatabaseService.semanticSearch(query, 20, 10, null))
            .thenReturn(mockProducts);
        when(vectorDatabaseService.getTotalCount(query, null))
            .thenReturn(50L);

        // When
        SearchResponse response = semanticSearchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.offset()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(50L);

        verify(vectorDatabaseService).semanticSearch(query, 20, 10, null);
    }

    @Test
    @DisplayName("Should handle exception and return empty response")
    void testSearchWithException() {
        // Given
        String query = "test";
        SearchRequest request = new SearchRequest(query, 10, 0, null);

        when(vectorDatabaseService.semanticSearch(anyString(), anyInt(), anyInt(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When
        SearchResponse response = semanticSearchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);
        assertThat(response.limit()).isEqualTo(10);
        assertThat(response.offset()).isEqualTo(0);
        assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
    }
}