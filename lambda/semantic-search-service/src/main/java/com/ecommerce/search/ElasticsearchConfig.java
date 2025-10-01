package com.ecommerce.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.endpoint}")
    private String endpoint;

    @Value("${elasticsearch.apikey}")
    private String apiKey;

    @Bean
    @org.springframework.context.annotation.Lazy
    public ElasticsearchClient elasticsearchClient() {
        String hostname = extractHostname(endpoint);
        int port = extractPort(endpoint);

        RestClient restClient = RestClient.builder(
                new HttpHost(hostname, port, "https"))
                .setDefaultHeaders(new Header[]{
                    new BasicHeader("Authorization", "ApiKey " + apiKey)
                })
                .build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }

    private String extractHostname(String endpoint) {
        String cleanUrl = endpoint.replace("https://", "").replace("http://", "");
        int colonIndex = cleanUrl.indexOf(':');
        if (colonIndex > 0) {
            return cleanUrl.substring(0, colonIndex);
        }
        return cleanUrl;
    }

    private int extractPort(String endpoint) {
        String cleanUrl = endpoint.replace("https://", "").replace("http://", "");
        int colonIndex = cleanUrl.indexOf(':');
        if (colonIndex > 0) {
            String portStr = cleanUrl.substring(colonIndex + 1);
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                return 443;
            }
        }
        return 443;
    }
}