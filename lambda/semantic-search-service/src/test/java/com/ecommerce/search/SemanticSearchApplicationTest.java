package com.ecommerce.search;

import com.ecommerce.search.dto.*;
import com.ecommerce.search.service.SemanticSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SemanticSearchApplicationTest {

    @Autowired
    private Function<SearchRequest, SearchResponse> semanticSearch;

    @MockBean
    private SemanticSearchService searchService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should execute lambda function successfully")
    void testLambdaFunctionExecution() {
        // Given
        SearchRequest request = new SearchRequest("laptop", 10, 0, null);

        List<Product> products = Arrays.asList(
            new Product("1", "Dell XPS 13", "Premium ultrabook", 1299.99,
                       "Computers", "Dell", "http://example.com/dell.jpg", 0.95),
            new Product("2", "HP Spectre x360", "2-in-1 laptop", 1199.99,
                       "Computers", "HP", "http://example.com/hp.jpg", 0.92)
        );

        SearchResponse expectedResponse = new SearchResponse(products, 2L, 10, 0, 100L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(expectedResponse);

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(2);
        assertThat(response.total()).isEqualTo(2L);
        assertThat(response.limit()).isEqualTo(10);
        assertThat(response.offset()).isEqualTo(0);

        verify(searchService).search(request);
    }

    @Test
    @DisplayName("Should handle lambda function with complex filters")
    void testLambdaFunctionWithFilters() {
        // Given
        SearchFilters filters = new SearchFilters("Electronics", 200.0, 800.0, "Samsung");
        SearchRequest request = new SearchRequest("smartphone", 5, 0, filters);

        Product product = new Product("3", "Samsung Galaxy S23", "Latest smartphone", 699.99,
                                     "Electronics", "Samsung", "http://example.com/samsung.jpg", 0.89);

        SearchResponse expectedResponse = new SearchResponse(Arrays.asList(product), 1L, 5, 0, 75L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(expectedResponse);

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).name()).isEqualTo("Samsung Galaxy S23");
        assertThat(response.products().get(0).brand()).isEqualTo("Samsung");

        verify(searchService).search(request);
    }

    @Test
    @DisplayName("Should handle lambda function with empty results")
    void testLambdaFunctionEmptyResults() {
        // Given
        SearchRequest request = new SearchRequest("nonexistent product xyz", 10, 0, null);
        SearchResponse expectedResponse = new SearchResponse(Arrays.asList(), 0L, 10, 0, 25L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(expectedResponse);

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);

        verify(searchService).search(request);
    }

    @Test
    @DisplayName("Should handle lambda function with pagination")
    void testLambdaFunctionPagination() {
        // Given
        SearchRequest request = new SearchRequest("electronics", 20, 40, null);

        List<Product> products = Arrays.asList(
            new Product("4", "iPhone 14", "Latest iPhone", 999.99,
                       "Electronics", "Apple", "http://example.com/iphone.jpg", 0.96),
            new Product("5", "iPad Pro", "Professional tablet", 1099.99,
                       "Electronics", "Apple", "http://example.com/ipad.jpg", 0.94)
        );

        SearchResponse expectedResponse = new SearchResponse(products, 150L, 20, 40, 120L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(expectedResponse);

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(2);
        assertThat(response.total()).isEqualTo(150L);
        assertThat(response.limit()).isEqualTo(20);
        assertThat(response.offset()).isEqualTo(40);

        verify(searchService).search(request);
    }

    @Test
    @DisplayName("Should handle lambda function exception gracefully")
    void testLambdaFunctionException() {
        // Given
        SearchRequest request = new SearchRequest("test", 10, 0, null);

        when(searchService.search(any(SearchRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.products()).isEmpty();
        assertThat(response.total()).isEqualTo(0L);

        verify(searchService).search(request);
    }

    @Test
    @DisplayName("Should validate request with default values")
    void testRequestDefaultValues() {
        // Given
        SearchRequest request = new SearchRequest("headphones"); // Using constructor with defaults

        List<Product> products = Arrays.asList(
            new Product("6", "AirPods Pro", "Wireless earbuds", 249.99,
                       "Electronics", "Apple", "http://example.com/airpods.jpg", 0.93)
        );

        SearchResponse expectedResponse = new SearchResponse(products, 1L, 10, 0, 50L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(expectedResponse);

        // When
        SearchResponse response = semanticSearch.apply(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.limit()).isEqualTo(10); // Default limit
        assertThat(response.offset()).isEqualTo(0); // Default offset

        verify(searchService).search(argThat(req ->
            req.query().equals("headphones") &&
            req.limit().equals(10) &&
            req.offset().equals(0)
        ));
    }
}