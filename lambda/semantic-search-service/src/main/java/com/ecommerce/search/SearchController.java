package com.ecommerce.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SearchController {

    private final SemanticSearchService searchService;

    @Autowired
    public SearchController(SemanticSearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/semantic")
    public SearchResponse semanticSearch(@RequestBody SearchRequest request) {
        return searchService.search(request);
    }

}