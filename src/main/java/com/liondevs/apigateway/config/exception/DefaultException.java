package com.liondevs.apigateway.config.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultException extends  RuntimeException{
    private final String message;
    private final HttpStatus httpStatus;
    public DefaultException(String message, HttpStatus httpStatus){
        super(message);
        this.message = message;
        this.httpStatus = httpStatus;

    }
}
