package com.ecommerce.search;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    public List<Double> generateEmbedding(String text) {
        return generateQueryVector(text);
    }

    private List<Double> generateQueryVector(String query) {
        if (query == null || query.trim().isEmpty()) {
            return generateRandomVector(384);
        }

        Random random = new Random(query.hashCode());
        return random.doubles(384, -1.0, 1.0)
                .boxed()
                .collect(Collectors.toList());
    }

    private List<Double> generateRandomVector(int dimensions) {
        Random random = new Random();
        return random.doubles(dimensions, -1.0, 1.0)
                .boxed()
                .collect(Collectors.toList());
    }
}