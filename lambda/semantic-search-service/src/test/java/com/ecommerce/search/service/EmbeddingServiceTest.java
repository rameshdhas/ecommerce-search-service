package com.ecommerce.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmbeddingServiceTest {

    @Mock
    private BedrockRuntimeClient bedrockClient;

    private EmbeddingService embeddingService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        embeddingService = new EmbeddingService();
        // Use reflection to set the mocked client
        setBedrockClient(embeddingService, bedrockClient);
    }

    @Test
    @DisplayName("Should generate embedding successfully")
    void testGenerateEmbedding() throws Exception {
        // Given
        String text = "wireless headphones";
        List<Double> expectedEmbedding = List.of(0.1, 0.2, 0.3, 0.4, 0.5);

        String responseJson = objectMapper.writeValueAsString(Map.of(
            "embedding", expectedEmbedding
        ));

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(responseJson, StandardCharsets.UTF_8))
            .build();

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(mockResponse);

        // When
        List<Double> result = embeddingService.generateEmbedding(text);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(5);
        assertThat(result).isEqualTo(expectedEmbedding);

        verify(bedrockClient).invokeModel(any(InvokeModelRequest.class));
    }

    @Test
    @DisplayName("Should handle empty text")
    void testEmptyText() throws Exception {
        // Given
        String emptyText = "";
        List<Double> defaultEmbedding = List.of(0.0, 0.1);

        String responseJson = objectMapper.writeValueAsString(Map.of(
            "embedding", defaultEmbedding
        ));

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(responseJson, StandardCharsets.UTF_8))
            .build();

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(mockResponse);

        // When
        List<Double> result = embeddingService.generateEmbedding(emptyText);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(defaultEmbedding);
    }

    @Test
    @DisplayName("Should handle null text")
    void testNullText() throws Exception {
        // Given
        List<Double> defaultEmbedding = List.of(0.0, 0.1);

        String responseJson = objectMapper.writeValueAsString(Map.of(
            "embedding", defaultEmbedding
        ));

        InvokeModelResponse mockResponse = InvokeModelResponse.builder()
            .body(SdkBytes.fromString(responseJson, StandardCharsets.UTF_8))
            .build();

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
            .thenReturn(mockResponse);

        // When
        List<Double> result = embeddingService.generateEmbedding(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(defaultEmbedding);
    }

    @Test
    @DisplayName("Should handle Bedrock service error")
    void testBedrockServiceError() {
        // Given
        String text = "test product";

        when(bedrockClient.invokeModel(any(InvokeModelRequest.class)))
            .thenThrow(new RuntimeException("Bedrock service unavailable"));

        // When/Then
        assertThatThrownBy(() -> embeddingService.generateEmbedding(text))
            .isInstanceOf(RuntimeException.class);

        verify(bedrockClient).invokeModel(any(InvokeModelRequest.class));
    }

    // Helper method to inject mock BedrockClient using reflection
    private void setBedrockClient(EmbeddingService service, BedrockRuntimeClient client) {
        try {
            var field = EmbeddingService.class.getDeclaredField("bedrockClient");
            field.setAccessible(true);
            field.set(service, client);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set BedrockClient", e);
        }
    }
}