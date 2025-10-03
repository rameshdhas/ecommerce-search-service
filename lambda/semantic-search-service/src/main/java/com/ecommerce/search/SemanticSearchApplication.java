package com.ecommerce.search;

import com.ecommerce.search.dto.SearchRequest;
import com.ecommerce.search.dto.SearchResponse;
import com.ecommerce.search.service.SemanticSearchService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class SemanticSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SemanticSearchApplication.class, args);
    }

    @Bean("semanticSearch")
    public Function<SearchRequest, SearchResponse> semanticSearch(SemanticSearchService searchService) {
        return searchService::search;
    }
}