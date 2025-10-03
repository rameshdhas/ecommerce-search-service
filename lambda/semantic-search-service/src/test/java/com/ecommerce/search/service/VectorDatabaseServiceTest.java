package com.ecommerce.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import com.ecommerce.search.dto.Product;
import com.ecommerce.search.dto.SearchFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VectorDatabaseServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private EmbeddingService embeddingService;

    private VectorDatabaseService vectorDatabaseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        vectorDatabaseService = new VectorDatabaseService(
            elasticsearchClient,
            embeddingService,
            "test-index"
        );
    }

    @Test
    @DisplayName("Should perform semantic search successfully")
    void testSemanticSearchSuccess() throws Exception {
        // Given
        String query = "laptop";
        Integer limit = 10;
        Integer offset = 0;

        List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3);
        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);

        Map<String, Object> source = new HashMap<>();
        source.put("id", "1");
        source.put("title", "Dell Laptop");
        source.put("description", "High performance laptop");
        source.put("image_url", "http://example.com/image.jpg");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("final_price", 999.99);
        metadata.put("categories", "Computers");
        metadata.put("brand", "Dell");
        source.put("metadata", metadata);

        List<Hit<Object>> hits = Arrays.asList(
            createMockHit(source, 0.95)
        );

        SearchResponse<Object> mockResponse = createMockSearchResponse(hits, 1L);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        List<Product> results = vectorDatabaseService.semanticSearch(query, limit, offset, null);

        // Then
        assertThat(results).hasSize(1);
        Product product = results.get(0);
        assertThat(product.id()).isEqualTo("1");
        assertThat(product.name()).isEqualTo("Dell Laptop");
        assertThat(product.price()).isEqualTo(999.99);
        assertThat(product.category()).isEqualTo("Computers");
        assertThat(product.brand()).isEqualTo("Dell");
        assertThat(product.score()).isEqualTo(0.95);

        verify(embeddingService).generateEmbedding(query);
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Object.class));
    }

    @Test
    @DisplayName("Should handle search with filters")
    void testSemanticSearchWithFilters() throws Exception {
        // Given
        String query = "headphones";
        SearchFilters filters = new SearchFilters("Electronics", 100.0, 500.0, "Sony");

        List<Double> mockEmbedding = Arrays.asList(0.4, 0.5, 0.6);
        when(embeddingService.generateEmbedding(query)).thenReturn(mockEmbedding);

        Map<String, Object> source = new HashMap<>();
        source.put("id", "2");
        source.put("title", "Sony Headphones");
        source.put("description", "Wireless headphones");
        source.put("image_url", "http://example.com/sony.jpg");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("final_price", 299.99);
        metadata.put("categories", "Electronics");
        metadata.put("brand", "Sony");
        source.put("metadata", metadata);

        List<Hit<Object>> hits = Arrays.asList(
            createMockHit(source, 0.89)
        );

        SearchResponse<Object> mockResponse = createMockSearchResponse(hits, 1L);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        List<Product> results = vectorDatabaseService.semanticSearch(query, 10, 0, filters);

        // Then
        assertThat(results).hasSize(1);
        Product product = results.get(0);
        assertThat(product.name()).isEqualTo("Sony Headphones");
        assertThat(product.brand()).isEqualTo("Sony");
        assertThat(product.price()).isEqualTo(299.99);

        verify(embeddingService).generateEmbedding(query);
    }

    @Test
    @DisplayName("Should fallback to text search when vector search fails")
    void testFallbackToTextSearch() throws Exception {
        // Given
        String query = "keyboard";
        when(embeddingService.generateEmbedding(query))
            .thenThrow(new RuntimeException("Embedding service unavailable"));

        Map<String, Object> source = new HashMap<>();
        source.put("id", "3");
        source.put("title", "Mechanical Keyboard");
        source.put("description", "Gaming keyboard");
        source.put("image_url", "http://example.com/keyboard.jpg");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("final_price", 149.99);
        metadata.put("categories", "Accessories");
        metadata.put("brand", "Logitech");
        source.put("metadata", metadata);

        List<Hit<Object>> hits = Arrays.asList(
            createMockHit(source, 0.75)
        );

        SearchResponse<Object> mockResponse = createMockSearchResponse(hits, 1L);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        List<Product> results = vectorDatabaseService.semanticSearch(query, 10, 0, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Mechanical Keyboard");

        verify(elasticsearchClient, times(2)).search(any(SearchRequest.class), eq(Object.class));
    }

    @Test
    @DisplayName("Should get total count correctly")
    void testGetTotalCount() throws Exception {
        // Given
        String query = "monitor";
        SearchResponse<Object> mockResponse = createMockSearchResponse(Collections.emptyList(), 42L);

        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        long count = vectorDatabaseService.getTotalCount(query, null);

        // Then
        assertThat(count).isEqualTo(42L);
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(Object.class));
    }

    @Test
    @DisplayName("Should handle empty search results")
    void testEmptySearchResults() throws Exception {
        // Given
        String query = "nonexistent";
        when(embeddingService.generateEmbedding(query)).thenReturn(Arrays.asList(0.7, 0.8));

        SearchResponse<Object> mockResponse = createMockSearchResponse(Collections.emptyList(), 0L);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        List<Product> results = vectorDatabaseService.semanticSearch(query, 10, 0, null);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle missing metadata gracefully")
    void testMissingMetadata() throws Exception {
        // Given
        String query = "product";
        when(embeddingService.generateEmbedding(query)).thenReturn(Arrays.asList(0.9, 1.0));

        Map<String, Object> source = new HashMap<>();
        source.put("id", "4");
        source.put("title", "Unknown Product");
        source.put("description", "Test product");
        // No metadata field

        List<Hit<Object>> hits = Arrays.asList(
            createMockHit(source, 0.5)
        );

        SearchResponse<Object> mockResponse = createMockSearchResponse(hits, 1L);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Object.class)))
            .thenReturn(mockResponse);

        // When
        List<Product> results = vectorDatabaseService.semanticSearch(query, 10, 0, null);

        // Then
        assertThat(results).hasSize(1);
        Product product = results.get(0);
        assertThat(product.price()).isEqualTo(0.0);
        assertThat(product.category()).isNull();
        assertThat(product.brand()).isNull();
    }

    // Helper methods
    private Hit<Object> createMockHit(Map<String, Object> source, double score) {
        @SuppressWarnings("unchecked")
        Hit<Object> hit = mock(Hit.class);
        when(hit.source()).thenReturn(source);
        when(hit.score()).thenReturn(score);
        return hit;
    }

    private SearchResponse<Object> createMockSearchResponse(List<Hit<Object>> hits, long total) {
        @SuppressWarnings("unchecked")
        SearchResponse<Object> response = mock(SearchResponse.class);

        @SuppressWarnings("unchecked")
        HitsMetadata<Object> hitsMetadata = mock(HitsMetadata.class);

        TotalHits totalHits = new TotalHits.Builder()
            .value(total)
            .relation(TotalHitsRelation.Eq)
            .build();

        when(hitsMetadata.hits()).thenReturn(hits);
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(response.hits()).thenReturn(hitsMetadata);

        return response;
    }
}