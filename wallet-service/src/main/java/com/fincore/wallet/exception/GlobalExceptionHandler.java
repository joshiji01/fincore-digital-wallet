package com.fincore.wallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> handleInsufficientBalance(
            InsufficientBalanceException ex) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 422,
                "error", ex.getMessage()
        );
    }

    @ExceptionHandler(WalletNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleWalletNotFound(
            WalletNotFoundException ex) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 404,
                "error", ex.getMessage()
        );
    }

    @ExceptionHandler(WalletAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleWalletAlreadyExists(
            WalletAlreadyExistsException ex) {

        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", 409,
                "error", ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Map<String, Object> handleInvalidTransfer(
        InvalidTransferException ex) {

    return Map.of(
            "timestamp", LocalDateTime.now(),
            "status", 400,
            "error", ex.getMessage()
    );
}
}