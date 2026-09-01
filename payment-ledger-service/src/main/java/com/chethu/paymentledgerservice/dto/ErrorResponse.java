package com.chethu.paymentledgerservice.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path,
            Map<String, String> fieldErrors) {
        this(timestamp, status, error, message, path);
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp(){
        return this.timestamp;
    }

    public int getStatus(){
        return this.status;
    }

    public String getError(){
        return this.error;
    }

    public String getMessage(){
        return this.message;
    }

    public String getPath(){
        return this.path;
    }

    public Map<String, String> getFieldErrors() {
        return this.fieldErrors;
    }
}
