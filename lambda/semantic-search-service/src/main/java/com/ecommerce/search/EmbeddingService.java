package com.ecommerce.search;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final BedrockRuntimeClient bedrockClient;
    private final String modelId = "amazon.titan-embed-text-v1";

    public EmbeddingService() {
        this.objectMapper = new ObjectMapper();
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    public List<Double> generateEmbedding(String text) {
        try {
            System.out.println("EmbeddingService: Attempting Bedrock embedding for text: " + text);
            if (text == null || text.trim().isEmpty()) {
                text = "empty query";
            }

            // Create the request payload for Titan Text Embeddings
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputText", text);
            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            System.out.println("Bedrock request payload: " + jsonRequest);

            // Use AWS SDK v2 for proper authentication
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(jsonRequest))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            System.out.println("Bedrock response received, body length: " + responseBody.length());

            // Parse the response to extract embedding
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = jsonResponse.get("embedding");

            System.out.println("Embedding node found: " + (embeddingNode != null));
            if (embeddingNode != null) {
                System.out.println("Embedding node is array: " + embeddingNode.isArray());
                System.out.println("Embedding array size: " + embeddingNode.size());
            }

            List<Double> embedding = new ArrayList<>();
            if (embeddingNode != null && embeddingNode.isArray()) {
                for (JsonNode node : embeddingNode) {
                    embedding.add(node.asDouble());
                }
            }

            System.out.println("Final embedding size: " + embedding.size());
            return embedding;

        } catch (Exception e) {
            System.out.println("Bedrock embedding failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Bedrock error cause: " + e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to generate Bedrock embedding", e);
        }
    }
}