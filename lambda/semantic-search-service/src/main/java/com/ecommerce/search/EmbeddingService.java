package com.ecommerce.search;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final String modelId = "amazon.titan-embed-text-v1";
    private final String region = "us-east-1";

    public EmbeddingService() {
        this.objectMapper = new ObjectMapper();
    }

    public List<Double> generateEmbedding(String text) {
        try {
            if (text == null || text.trim().isEmpty()) {
                text = "empty query";
            }

            // Create the request payload for Titan Text Embeddings
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputText", text);

            String jsonRequest = objectMapper.writeValueAsString(requestBody);

            // Create and sign the request
            String endpoint = String.format("https://bedrock-runtime.%s.amazonaws.com/model/%s/invoke", region, modelId);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(endpoint);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Accept", "application/json");
                httpPost.setEntity(new StringEntity(jsonRequest));

                // Sign the request using AWS credentials
                AWS4Signer signer = new AWS4Signer();
                signer.setServiceName("bedrock");
                signer.setRegionName(region);

                DefaultRequest<Void> awsRequest = new DefaultRequest<>("bedrock");
                awsRequest.setHttpMethod(HttpMethodName.POST);
                awsRequest.setEndpoint(URI.create(endpoint));
                awsRequest.setContent(new ByteArrayInputStream(jsonRequest.getBytes()));
                awsRequest.getHeaders().put("Content-Type", "application/json");
                awsRequest.getHeaders().put("Accept", "application/json");

                signer.sign(awsRequest, DefaultAWSCredentialsProviderChain.getInstance().getCredentials());

                // Add signed headers to HTTP request
                awsRequest.getHeaders().forEach(httpPost::setHeader);

                CloseableHttpResponse response = httpClient.execute(httpPost);
                String responseBody = EntityUtils.toString(response.getEntity());

                // Parse the response to extract embedding
                JsonNode jsonResponse = objectMapper.readTree(responseBody);
                JsonNode embeddingNode = jsonResponse.get("embedding");

                List<Double> embedding = new ArrayList<>();
                if (embeddingNode != null && embeddingNode.isArray()) {
                    for (JsonNode node : embeddingNode) {
                        embedding.add(node.asDouble());
                    }
                }

                return embedding;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Bedrock embedding", e);
        }
    }
}