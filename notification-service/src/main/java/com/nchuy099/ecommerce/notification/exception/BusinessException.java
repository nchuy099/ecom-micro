package com.nchuy099.ecommerce.notification.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class BusinessException extends RuntimeException {
    private final HttpStatusCode status;
    private final URI type;
    private final String title;
    private final Map<String, Object> properties;

    public BusinessException(HttpStatusCode status, String type, String title, String message) {
        this(status, type, title, message, null, Map.of());
    }

    private BusinessException(
            HttpStatusCode status,
            String type,
            String title,
            String message,
            Throwable cause,
            Map<String, Object> properties
    ) {
        super(message, cause);
        this.status = status;
        this.type = URI.create(type);
        this.title = title;
        this.properties = Map.copyOf(properties);
    }

    public static BusinessException notFound(String type, String title, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, type, title, message);
    }

    public BusinessException withProperty(String name, Object value) {
        Map<String, Object> updated = new LinkedHashMap<>(properties);
        updated.put(name, value);
        return new BusinessException(status, type.toString(), title, getMessage(), getCause(), updated);
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public URI getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
