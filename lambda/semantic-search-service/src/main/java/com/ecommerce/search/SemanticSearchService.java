package com.ecommerce.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class SemanticSearchService {

    private final VectorDatabaseService vectorDatabaseService;

    @Autowired
    public SemanticSearchService(VectorDatabaseService vectorDatabaseService) {
        this.vectorDatabaseService = vectorDatabaseService;
    }

    public SearchResponse search(SearchRequest request) {
        long startTime = Instant.now().toEpochMilli();

        try {
            List<SearchResponse.Product> products = vectorDatabaseService.semanticSearch(
                request.getQuery(),
                request.getLimit(),
                request.getOffset(),
                request.getFilters()
            );

            long total = vectorDatabaseService.getTotalCount(request.getQuery(), request.getFilters());
            long processingTime = Instant.now().toEpochMilli() - startTime;

            return new SearchResponse(products, total, request.getLimit(), request.getOffset(), processingTime);

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to perform semantic search: " + e.getMessage(), e);
        }
    }
}