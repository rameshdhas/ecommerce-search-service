package com.ecommerce.search.controller;

import com.ecommerce.search.dto.*;
import com.ecommerce.search.service.SemanticSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SemanticSearchService searchService;

    private SearchRequest validRequest;
    private SearchResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = new SearchRequest("laptop", 10, 0, null);

        List<Product> products = Arrays.asList(
            new Product("1", "Dell XPS 13", "Ultrabook", 999.99,
                       "Computers", "Dell", "http://example.com/dell.jpg", 0.95),
            new Product("2", "MacBook Pro", "Professional laptop", 1999.99,
                       "Computers", "Apple", "http://example.com/apple.jpg", 0.93)
        );

        mockResponse = new SearchResponse(products, 2L, 10, 0, 150L);
    }

    @Test
    @DisplayName("Should perform semantic search successfully")
    void testSemanticSearchSuccess() throws Exception {
        // Given
        when(searchService.search(any(SearchRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products", hasSize(2)))
            .andExpect(jsonPath("$.products[0].name", is("Dell XPS 13")))
            .andExpect(jsonPath("$.products[1].name", is("MacBook Pro")))
            .andExpect(jsonPath("$.total", is(2)))
            .andExpect(jsonPath("$.limit", is(10)))
            .andExpect(jsonPath("$.offset", is(0)))
            .andExpect(jsonPath("$.processingTimeMs", is(150)));

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("Should handle search with filters")
    void testSearchWithFilters() throws Exception {
        // Given
        SearchFilters filters = new SearchFilters("Electronics", 100.0, 500.0, "Sony");
        SearchRequest requestWithFilters = new SearchRequest("headphones", 5, 0, filters);

        Product product = new Product("3", "Sony WH-1000XM4", "Wireless headphones", 349.99,
                                     "Electronics", "Sony", "http://example.com/sony.jpg", 0.88);
        SearchResponse filteredResponse = new SearchResponse(Arrays.asList(product), 1L, 5, 0, 100L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(filteredResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestWithFilters)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products", hasSize(1)))
            .andExpect(jsonPath("$.products[0].name", is("Sony WH-1000XM4")))
            .andExpect(jsonPath("$.products[0].brand", is("Sony")))
            .andExpect(jsonPath("$.products[0].price", is(349.99)));

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testEmptySearchResults() throws Exception {
        // Given
        SearchResponse emptyResponse = new SearchResponse(Arrays.asList(), 0L, 10, 0, 50L);
        when(searchService.search(any(SearchRequest.class))).thenReturn(emptyResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.products", hasSize(0)))
            .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    @DisplayName("Should validate required query field")
    void testMissingQueryValidation() throws Exception {
        // Given
        String invalidRequest = "{\"limit\": 10, \"offset\": 0}";

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());

        verify(searchService, never()).search(any());
    }

    @Test
    @DisplayName("Should validate limit range")
    void testInvalidLimitValidation() throws Exception {
        // Given
        SearchRequest invalidRequest = new SearchRequest("test", 0, 0, null); // limit = 0 is invalid

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\": \"test\", \"limit\": 0, \"offset\": 0}"))
            .andExpect(status().isBadRequest());

        verify(searchService, never()).search(any());
    }

    @Test
    @DisplayName("Should handle pagination")
    void testPagination() throws Exception {
        // Given
        SearchRequest paginatedRequest = new SearchRequest("smartphone", 20, 40, null);
        SearchResponse paginatedResponse = new SearchResponse(Arrays.asList(), 100L, 20, 40, 75L);

        when(searchService.search(any(SearchRequest.class))).thenReturn(paginatedResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paginatedRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.limit", is(20)))
            .andExpect(jsonPath("$.offset", is(40)))
            .andExpect(jsonPath("$.total", is(100)));

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("Should handle CORS preflight request")
    void testCorsPreflightRequest() throws Exception {
        mockMvc.perform(options("/api/search/semantic")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should handle special characters in query")
    void testSpecialCharactersInQuery() throws Exception {
        // Given
        SearchRequest specialRequest = new SearchRequest("laptop & \"gaming\" (high-end)", 10, 0, null);
        when(searchService.search(any(SearchRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(specialRequest)))
            .andExpect(status().isOk());

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void testServiceException() throws Exception {
        // Given
        when(searchService.search(any(SearchRequest.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isInternalServerError());

        verify(searchService).search(any(SearchRequest.class));
    }

    @Test
    @DisplayName("Should accept valid JSON content type")
    void testContentTypeValidation() throws Exception {
        // Given
        when(searchService.search(any(SearchRequest.class))).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(post("/api/search/semantic")
                .contentType("application/json;charset=UTF-8")
                .content(objectMapper.writeValueAsString(validRequest)))
            .andExpect(status().isOk());
    }
}