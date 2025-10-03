package com.ecommerce.search.service;

import com.ecommerce.search.dto.Product;
import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
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
            List<Product> products = vectorDatabaseService.semanticSearch(
                request.query(),
                request.limit(),
                request.offset(),
                request.filters()
            );

            long total = vectorDatabaseService.getTotalCount(request.query(), request.filters());
            long processingTime = Instant.now().toEpochMilli() - startTime;

            return new SearchResponse(products, total, request.limit(), request.offset(), processingTime);

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to perform semantic search: " + e.getMessage(), e);
        }
    }
}