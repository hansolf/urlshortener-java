package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFoundException(ResourceNotFoundException e, Model model) {
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("errorCode", 404);
        model.addAttribute("timestamp", java.time.LocalDateTime.now());
        return "error/custom-error";
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDeniedException(AccessDeniedException e, Model model) {
        model.addAttribute("errorMessage", "У вас нет доступа к этому ресурсу");
        model.addAttribute("errorCode", 403);
        model.addAttribute("timestamp", java.time.LocalDateTime.now());
        return "error/custom-error";
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception e, Model model) {
        e.printStackTrace();
        model.addAttribute("errorMessage", "Произошла внутренняя ошибка сервера");
        model.addAttribute("errorDetails", e.getMessage());
        model.addAttribute("errorCode", 500);
        model.addAttribute("timestamp", java.time.LocalDateTime.now());
        return "error/custom-error";
    }
} 