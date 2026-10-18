package io.worldloom.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);
    public record ErrorBody(String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorBody> invalid(IllegalArgumentException e) { return error(400, e.getMessage()); }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> missingBody() { return error(400, "A world JSON document is required"); }

    @ExceptionHandler(ConditionalCheckFailedException.class)
    ResponseEntity<ErrorBody> duplicate() {
        return error(409, "That world ID already exists. Choose a different ID to save a new world.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorBody> status(ResponseStatusException e) {
        return error(e.getStatusCode().value(), e.getReason());
    }

    @ExceptionHandler({DynamoDbException.class, SdkClientException.class})
    ResponseEntity<ErrorBody> unavailable(Exception e) {
        LOG.warn("DynamoDB request failed: {}", e.getClass().getSimpleName());
        return error(503, "Cannot access DynamoDB. Check the backend's AWS credentials, region, table, and permissions.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> unexpected(Exception e) {
        LOG.error("World request failed", e);
        return error(500, "The world could not be processed. Check the backend logs.");
    }

    private ResponseEntity<ErrorBody> error(int status, String message) {
        return ResponseEntity.status(status).body(new ErrorBody(message));
    }
}
