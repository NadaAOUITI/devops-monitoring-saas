package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.exception.InvalidCredentialsException;
import com.n.devopsmonitoringsaas.exception.InvalidInvitationException;
import com.n.devopsmonitoringsaas.exception.OwnerOperationException;
import com.n.devopsmonitoringsaas.exception.PlanLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<String> handlePlanLimitExceeded(PlanLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    @ExceptionHandler(InvalidInvitationException.class)
    public ResponseEntity<String> handleInvalidInvitation(InvalidInvitationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(OwnerOperationException.class)
    public ResponseEntity<String> handleOwnerOperation(OwnerOperationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        if (e.getMessage() != null && e.getMessage().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
