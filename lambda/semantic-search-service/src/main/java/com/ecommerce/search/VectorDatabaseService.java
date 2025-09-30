package com.ecommerce.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorDatabaseService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String vectorDbEndpoint;
    private final String indexName;

    public VectorDatabaseService(
            @Value("${vector.db.endpoint}") String vectorDbEndpoint,
            @Value("${vector.db.index:products}") String indexName) {
        this.vectorDbEndpoint = vectorDbEndpoint;
        this.indexName = indexName;
        this.webClient = WebClient.builder()
                .baseUrl(vectorDbEndpoint)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<SearchResponse.Product> semanticSearch(String query, Integer limit, Integer offset, SearchRequest.SearchFilters filters) {
        try {
            Map<String, Object> searchQuery = buildVectorSearchQuery(query, limit, offset, filters);

            String response = webClient.post()
                    .uri("/{index}/_search", indexName)
                    .bodyValue(searchQuery)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResults(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute vector search", e);
        }
    }

    public long getTotalCount(String query, SearchRequest.SearchFilters filters) {
        try {
            Map<String, Object> countQuery = buildCountQuery(query, filters);

            String response = webClient.post()
                    .uri("/{index}/_count", indexName)
                    .bodyValue(countQuery)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            return jsonResponse.get("count").asLong();

        } catch (Exception e) {
            return 0L;
        }
    }

    private Map<String, Object> buildVectorSearchQuery(String query, Integer limit, Integer offset, SearchRequest.SearchFilters filters) {
        Map<String, Object> searchQuery = new HashMap<>();
        searchQuery.put("size", limit);
        searchQuery.put("from", offset);

        Map<String, Object> queryObj = new HashMap<>();

        Map<String, Object> knnQuery = new HashMap<>();
        knnQuery.put("field", "description_vector");
        knnQuery.put("query_vector", generateQueryVector(query));
        knnQuery.put("k", limit + offset);
        knnQuery.put("num_candidates", Math.max(100, (limit + offset) * 2));

        if (filters != null) {
            List<Map<String, Object>> filterConditions = buildFilterConditions(filters);
            if (!filterConditions.isEmpty()) {
                Map<String, Object> boolQuery = new HashMap<>();
                boolQuery.put("must", filterConditions);
                knnQuery.put("filter", boolQuery);
            }
        }

        queryObj.put("knn", knnQuery);
        searchQuery.put("query", queryObj);

        Map<String, Object> source = new HashMap<>();
        source.put("includes", Arrays.asList("id", "name", "description", "price", "category", "brand", "image_url"));
        searchQuery.put("_source", source);

        return searchQuery;
    }

    private Map<String, Object> buildCountQuery(String query, SearchRequest.SearchFilters filters) {
        Map<String, Object> countQuery = new HashMap<>();

        if (filters != null) {
            List<Map<String, Object>> filterConditions = buildFilterConditions(filters);
            if (!filterConditions.isEmpty()) {
                Map<String, Object> boolQuery = new HashMap<>();
                boolQuery.put("must", filterConditions);
                countQuery.put("query", Map.of("bool", boolQuery));
            }
        }

        if (countQuery.isEmpty()) {
            countQuery.put("query", Map.of("match_all", Map.of()));
        }

        return countQuery;
    }

    private List<Map<String, Object>> buildFilterConditions(SearchRequest.SearchFilters filters) {
        List<Map<String, Object>> conditions = new ArrayList<>();

        if (filters.getCategory() != null && !filters.getCategory().trim().isEmpty()) {
            conditions.add(Map.of("term", Map.of("category.keyword", filters.getCategory())));
        }

        if (filters.getBrand() != null && !filters.getBrand().trim().isEmpty()) {
            conditions.add(Map.of("term", Map.of("brand.keyword", filters.getBrand())));
        }

        if (filters.getPriceMin() != null || filters.getPriceMax() != null) {
            Map<String, Object> rangeCondition = new HashMap<>();
            if (filters.getPriceMin() != null) {
                rangeCondition.put("gte", filters.getPriceMin());
            }
            if (filters.getPriceMax() != null) {
                rangeCondition.put("lte", filters.getPriceMax());
            }
            conditions.add(Map.of("range", Map.of("price", rangeCondition)));
        }

        return conditions;
    }

    private List<Double> generateQueryVector(String query) {
        Random random = new Random(query.hashCode());
        return random.doubles(768, -1.0, 1.0)
                .boxed()
                .collect(Collectors.toList());
    }

    private List<SearchResponse.Product> parseSearchResults(String response) {
        try {
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode hits = jsonResponse.get("hits").get("hits");

            List<SearchResponse.Product> products = new ArrayList<>();
            for (JsonNode hit : hits) {
                JsonNode source = hit.get("_source");
                double score = hit.get("_score").asDouble();

                SearchResponse.Product product = new SearchResponse.Product(
                        source.get("id").asText(),
                        source.get("name").asText(),
                        source.get("description").asText(),
                        source.get("price").asDouble(),
                        source.get("category").asText(),
                        source.get("brand").asText(),
                        source.has("image_url") ? source.get("image_url").asText() : null,
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