package io.github.johneliud.order_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.johneliud.order_service.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class ProductServiceClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ProductApiResponse {
        @JsonProperty("data")
        public ProductDto data;
    }

    private final RestClient restClient;

    public ProductServiceClient(@Value("${product.service.url}") String productServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    @Retryable(retryFor = ResourceAccessException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public ProductDto getProduct(String productId) {
        log.info("Fetching product data from product service for productId: {}", productId);
        try {
            ProductApiResponse response = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(ProductApiResponse.class);
            if (response == null || response.data == null) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            return response.data;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch product {}: {}", productId, e.getMessage());
            throw new IllegalArgumentException("Product not found or unavailable: " + productId);
        }
    }

    public void decrementStock(String productId, int quantity) {
        log.info("Decrementing stock for productId: {} by {}", productId, quantity);
        try {
            restClient.patch()
                    .uri("/internal/products/{id}/stock", productId)
                    .body(java.util.Map.of("quantity", quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to decrement stock for productId: {}: {}", productId, e.getMessage());
            throw new IllegalArgumentException("Stock update failed for product: " + productId);
        }
    }
}
