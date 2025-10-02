package com.ecommerce.search;

import org.springframework.stereotype.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryParser {

    private static final Pattern BRAND_PATTERN = Pattern.compile(
        "\\b(?:brand|from|by|in brand)\\s+([A-Za-z0-9]+(?:\\s+[A-Za-z0-9]+)*?)(?=\\s+(?:and|or|with|for|$)|$)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CATEGORY_PATTERN = Pattern.compile(
        "\\b(?:category|in category|under)\\s+([A-Za-z0-9]+(?:\\s+[A-Za-z0-9]+)*?)(?=\\s+(?:and|or|with|for|brand|$)|$)",
        Pattern.CASE_INSENSITIVE
    );

    public ParsedQuery parseQuery(String query, SearchRequest.SearchFilters existingFilters) {
        if (query == null || query.trim().isEmpty()) {
            return new ParsedQuery(query, existingFilters);
        }

        String cleanedQuery = query;
        SearchRequest.SearchFilters filters = existingFilters != null ?
            copyFilters(existingFilters) : new SearchRequest.SearchFilters();

        // Extract brand from query if not already in filters
        if (filters.getBrand() == null || filters.getBrand().isEmpty()) {
            Matcher brandMatcher = BRAND_PATTERN.matcher(query);
            if (brandMatcher.find()) {
                String brand = brandMatcher.group(1).trim();
                filters.setBrand(brand);
                // Remove the brand phrase from the query for better semantic search
                cleanedQuery = cleanedQuery.substring(0, brandMatcher.start()) +
                              cleanedQuery.substring(brandMatcher.end());
            }
        }

        // Extract category from query if not already in filters
        if (filters.getCategory() == null || filters.getCategory().isEmpty()) {
            Matcher categoryMatcher = CATEGORY_PATTERN.matcher(cleanedQuery);
            if (categoryMatcher.find()) {
                String category = categoryMatcher.group(1).trim();
                filters.setCategory(category);
                // Remove the category phrase from the query
                cleanedQuery = cleanedQuery.substring(0, categoryMatcher.start()) +
                              cleanedQuery.substring(categoryMatcher.end());
            }
        }

        // Clean up extra spaces
        cleanedQuery = cleanedQuery.replaceAll("\\s+", " ").trim();

        // If the query becomes empty after extraction, use the original query
        if (cleanedQuery.isEmpty()) {
            cleanedQuery = query;
        }

        return new ParsedQuery(cleanedQuery, filters);
    }

    private SearchRequest.SearchFilters copyFilters(SearchRequest.SearchFilters original) {
        SearchRequest.SearchFilters copy = new SearchRequest.SearchFilters();
        copy.setBrand(original.getBrand());
        copy.setCategory(original.getCategory());
        copy.setPriceMin(original.getPriceMin());
        copy.setPriceMax(original.getPriceMax());
        return copy;
    }

    public static class ParsedQuery {
        private final String cleanedQuery;
        private final SearchRequest.SearchFilters filters;

        public ParsedQuery(String cleanedQuery, SearchRequest.SearchFilters filters) {
            this.cleanedQuery = cleanedQuery;
            this.filters = filters;
        }

        public String getCleanedQuery() {
            return cleanedQuery;
        }

        public SearchRequest.SearchFilters getFilters() {
            return filters;
        }
    }
}