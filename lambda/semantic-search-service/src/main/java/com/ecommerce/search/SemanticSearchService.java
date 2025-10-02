package com.ecommerce.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class SemanticSearchService {

    private final VectorDatabaseService vectorDatabaseService;
    private final QueryParser queryParser;

    @Autowired
    public SemanticSearchService(VectorDatabaseService vectorDatabaseService, QueryParser queryParser) {
        this.vectorDatabaseService = vectorDatabaseService;
        this.queryParser = queryParser;
    }

    public SearchResponse search(SearchRequest request) {
        long startTime = Instant.now().toEpochMilli();

        try {
            // Parse the query to extract brand and category mentions
            QueryParser.ParsedQuery parsedQuery = queryParser.parseQuery(
                request.getQuery(),
                request.getFilters()
            );

            String cleanedQuery = parsedQuery.getCleanedQuery();
            SearchRequest.SearchFilters enhancedFilters = parsedQuery.getFilters();

            // Log the parsed query and extracted filters
            System.out.println("Original query: " + request.getQuery());
            System.out.println("Cleaned query: " + cleanedQuery);
            if (enhancedFilters != null) {
                if (enhancedFilters.getBrand() != null) {
                    System.out.println("Extracted brand filter: " + enhancedFilters.getBrand());
                }
                if (enhancedFilters.getCategory() != null) {
                    System.out.println("Extracted category filter: " + enhancedFilters.getCategory());
                }
            }

            List<SearchResponse.Product> products = vectorDatabaseService.semanticSearch(
                cleanedQuery,
                request.getLimit(),
                request.getOffset(),
                enhancedFilters
            );

            long total = vectorDatabaseService.getTotalCount(cleanedQuery, enhancedFilters);
            long processingTime = Instant.now().toEpochMilli() - startTime;

            return new SearchResponse(products, total, request.getLimit(), request.getOffset(), processingTime);

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to perform semantic search: " + e.getMessage(), e);
        }
    }
}