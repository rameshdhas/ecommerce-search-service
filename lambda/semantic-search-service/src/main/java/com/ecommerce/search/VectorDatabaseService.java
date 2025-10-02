package com.ecommerce.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorDatabaseService {

    private final ElasticsearchClient elasticsearchClient;
    private final ObjectMapper objectMapper;
    private final String indexName;
    private final EmbeddingService embeddingService;

    @Autowired
    public VectorDatabaseService(
            ElasticsearchClient elasticsearchClient,
            EmbeddingService embeddingService,
            @Value("${elasticsearch.index:ecommerce-products}") String indexName) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService;
        this.indexName = indexName;
        this.objectMapper = new ObjectMapper();
    }

    public List<com.ecommerce.search.SearchResponse.Product> semanticSearch(String query, Integer limit, Integer offset, com.ecommerce.search.SearchRequest.SearchFilters filters) {
        try {
            // Try vector search first, fallback to text search if vector fields don't exist
            SearchRequest searchRequest;
            try {
                searchRequest = buildVectorSearchRequest(query, limit, offset, filters);
                SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
                return parseSearchResults(response);
            } catch (Exception vectorError) {
                System.out.println("Vector search failed, falling back to text search: " + vectorError.getMessage());
                System.out.println("Vector error details: " + vectorError.getClass().getSimpleName());
                if (vectorError.getCause() != null) {
                    System.out.println("Vector error cause: " + vectorError.getCause().getMessage());
                }
                // Fallback to text-based search
                searchRequest = buildTextSearchRequest(query, limit, offset, filters);
                SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
                return parseSearchResults(response);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to execute search: " + e.getMessage(), e);
        }
    }

    public long getTotalCount(String query, com.ecommerce.search.SearchRequest.SearchFilters filters) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0)
                    .query(buildTextQuery(query, filters))
            );

            SearchResponse<Object> response = elasticsearchClient.search(searchRequest, Object.class);
            return response.hits().total().value();

        } catch (IOException e) {
            System.out.println("Failed to get total count: " + e.getMessage());
            return 0L;
        }
    }

    private SearchRequest buildVectorSearchRequest(String query, Integer limit, Integer offset, com.ecommerce.search.SearchRequest.SearchFilters filters) {
        System.out.println("Generating embedding for query: " + query);
        List<Double> queryVector = embeddingService.generateEmbedding(query);
        if (queryVector == null || queryVector.isEmpty()) {
            System.out.println("Embedding generation failed - got null or empty vector");
            throw new RuntimeException("Failed to generate embedding for query: " + query);
        }
        System.out.println("Embedding generated successfully - dimension: " + queryVector.size());
        List<Float> queryVectorFloats = queryVector.stream().map(Double::floatValue).collect(Collectors.toList());

        var knnBuilder = SearchRequest.of(s -> {
            var searchBuilder = s.index(indexName)
                    .size(limit)
                    .from(offset);

            // Build the KNN query
            var knnQuery = searchBuilder.knn(knn -> {
                var kb = knn.field("embeddings")
                        .queryVector(queryVectorFloats)
                        .k(limit + offset)
                        .numCandidates(Math.max(100, (limit + offset) * 2));

                // Only add filter if there are actual filters
                Query filterQuery = buildFilterQuery(filters);
                if (!isMatchAllQuery(filterQuery)) {
                    kb.filter(filterQuery);
                }
                return kb;
            });

            // Add source filtering
            return knnQuery.source(src -> src
                    .filter(f -> f
                            .includes("id", "title", "url", "image_url", "description", "metadata")
                    )
            );
        });

        return knnBuilder;
    }

    private SearchRequest buildTextSearchRequest(String query, Integer limit, Integer offset, com.ecommerce.search.SearchRequest.SearchFilters filters) {
        return SearchRequest.of(s -> s
                .index(indexName)
                .size(limit)
                .from(offset)
                .query(buildTextQuery(query, filters))
                .source(src -> src
                        .filter(f -> f
                                .includes("id", "title", "url", "image_url", "description", "metadata")
                        )
                )
        );
    }

    private Query buildTextQuery(String query, com.ecommerce.search.SearchRequest.SearchFilters filters) {
        List<Query> mustClauses = new ArrayList<>();

        // Add text search if query is provided
        if (query != null && !query.trim().isEmpty()) {
            mustClauses.add(Query.of(q -> q
                    .multiMatch(mm -> mm
                            .query(query)
                            .fields("title^2", "description")
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                    )
            ));
        }

        // Add filters
        Query filterQuery = buildFilterQuery(filters);
        if (!isMatchAllQuery(filterQuery)) {
            mustClauses.add(filterQuery);
        }

        if (mustClauses.isEmpty()) {
            return Query.of(q -> q.matchAll(ma -> ma));
        } else if (mustClauses.size() == 1) {
            return mustClauses.get(0);
        } else {
            return Query.of(q -> q.bool(b -> b.must(mustClauses)));
        }
    }

    private boolean isMatchAllQuery(Query query) {
        return query.isMatchAll();
    }

    private Query buildFilterQuery(com.ecommerce.search.SearchRequest.SearchFilters filters) {
        if (filters == null) {
            return Query.of(q -> q.matchAll(ma -> ma));
        }

        List<Query> mustClauses = new ArrayList<>();

        if (filters.getCategory() != null && !filters.getCategory().trim().isEmpty()) {
            mustClauses.add(Query.of(q -> q
                    .term(t -> t
                            .field("metadata.categories")
                            .value(filters.getCategory())
                    )
            ));
        }

        if (filters.getBrand() != null && !filters.getBrand().trim().isEmpty()) {
            // Use match query for case-insensitive brand matching
            mustClauses.add(Query.of(q -> q
                    .match(m -> m
                            .field("metadata.brand")
                            .query(filters.getBrand())
                    )
            ));
        }

        if (filters.getPriceMin() != null || filters.getPriceMax() != null) {
            mustClauses.add(Query.of(q -> q
                    .range(r -> {
                        var rangeQuery = r.field("metadata.final_price");
                        if (filters.getPriceMin() != null) {
                            rangeQuery.gte(co.elastic.clients.json.JsonData.of(filters.getPriceMin()));
                        }
                        if (filters.getPriceMax() != null) {
                            rangeQuery.lte(co.elastic.clients.json.JsonData.of(filters.getPriceMax()));
                        }
                        return rangeQuery;
                    })
            ));
        }

        if (mustClauses.isEmpty()) {
            return Query.of(q -> q.matchAll(ma -> ma));
        }

        return Query.of(q -> q
                .bool(b -> b.must(mustClauses))
        );
    }



    private List<com.ecommerce.search.SearchResponse.Product> parseSearchResults(SearchResponse<Object> response) {
        try {
            List<com.ecommerce.search.SearchResponse.Product> products = new ArrayList<>();

            for (Hit<Object> hit : response.hits().hits()) {
                Map<String, Object> source = (Map<String, Object>) hit.source();
                double score = hit.score() != null ? hit.score() : 0.0;

                Map<String, Object> metadata = (Map<String, Object>) source.get("metadata");
                if (metadata == null) {
                    metadata = new HashMap<>();
                }

                com.ecommerce.search.SearchResponse.Product product = new com.ecommerce.search.SearchResponse.Product(
                        (String) source.get("id"),
                        (String) source.get("title"),
                        (String) source.get("description"),
                        metadata.get("final_price") != null ? ((Number) metadata.get("final_price")).doubleValue() : 0.0,
                        (String) metadata.get("categories"),
                        (String) metadata.get("brand"),
                        (String) source.get("image_url"),
                        score
                );
                products.add(product);
            }

            return products;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse search results", e);
        }
    }
}