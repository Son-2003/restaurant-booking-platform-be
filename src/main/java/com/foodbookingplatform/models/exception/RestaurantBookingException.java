package com.foodbookingplatform.models.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class RestaurantBookingException extends RuntimeException{
    private final HttpStatus status;
    private final String message;
    public RestaurantBookingException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
