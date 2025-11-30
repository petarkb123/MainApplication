package project.fitnessapplicationexam.common.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import project.fitnessapplicationexam.common.exceptions.EmptyTemplateException;
import project.fitnessapplicationexam.common.exceptions.InvalidAvatarUrlException;
import project.fitnessapplicationexam.common.exceptions.TemplateNameConflictException;
import project.fitnessapplicationexam.common.exceptions.WorkoutAlreadyFinishedException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmptyTemplateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleEmptyTemplate(EmptyTemplateException ex, HttpServletRequest request) {
        log.warn("Empty/invalid template: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Template", ex.getMessage());
        }
        return buildErrorView(400, "Invalid Template", ex.getMessage(), request);
    }

    @ExceptionHandler(TemplateNameConflictException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleTemplateNameConflict(TemplateNameConflictException ex, HttpServletRequest request) {
        log.warn("Template name conflict: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Template Name Conflict", ex.getMessage());
        }
        return buildErrorView(400, "Template Name Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAvatarUrlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleInvalidAvatar(InvalidAvatarUrlException ex, HttpServletRequest request) {
        log.warn("Invalid avatar URL: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Avatar URL", ex.getMessage());
        }
        return buildErrorView(400, "Invalid Avatar URL", ex.getMessage(), request);
    }

    @ExceptionHandler(WorkoutAlreadyFinishedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleAlreadyFinished(WorkoutAlreadyFinishedException ex, HttpServletRequest request) {
        log.warn("Workout already finished: {}", ex.getMessage());
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
        }
        return buildErrorView(400, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        for (FieldError e : errors) {
            fieldErrors.put(e.getField(), e.getDefaultMessage());
        }
        
        if (isApiRequest(request)) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Validation Failed");
            body.put("fieldErrors", fieldErrors);
            return ResponseEntity.badRequest().body(body);
        }
        
        ModelAndView modelAndView = new ModelAndView();
        String viewName = determineViewName(request);
        modelAndView.setViewName(viewName);
        
        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult.getTarget() != null) {
            modelAndView.addObject("form", bindingResult.getTarget());
        }
        
        modelAndView.addObject("fieldErrors", fieldErrors);
        modelAndView.addObject("errorMessage", "Please fix the validation errors below.");
        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        
        return modelAndView;
    }
    
    private String determineViewName(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path != null) {
            if (path.contains("/register")) return "register";
            if (path.contains("/templates/create")) return "create";
            if (path.contains("/templates") && path.contains("/edit")) return "edit";
        }
        return "error";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Object handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Constraint Violation", ex.getMessage());
        }
        return buildErrorView(400, "Constraint Violation", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Object handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = ex.getMethod() + " method is not supported for this endpoint.";
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", message);
        }
        return buildErrorView(405, "Method Not Allowed", message, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Object handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String reason = ex.getReason() != null ? ex.getReason() : "An error occurred";
        log.warn("ResponseStatusException: {} - {}", status, reason);
        
        if (isApiRequest(request)) {
            return buildErrorResponse(status, status.getReasonPhrase(), reason);
        }
        return buildErrorView(status.value(), status.getReasonPhrase(), reason, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "The requested page does not exist.");
        }
        return buildErrorView(404, "Not Found", "The requested page does not exist.", request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Object handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        
        String message = "An unexpected error occurred. Please try again later.";
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message);
        }
        return buildErrorView(500, "Internal Server Error", message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "You are not allowed to perform this operation.");
        }
        return buildErrorView(403, "Forbidden", "You are not allowed to perform this operation.", request);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");
        
        return (path != null && path.startsWith("/api/")) ||
               (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private ModelAndView buildErrorView(int status, String error, String message, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.setStatus(HttpStatus.valueOf(status));
        modelAndView.addObject("status", status);
        modelAndView.addObject("error", error);
        modelAndView.addObject("message", message);
        return modelAndView;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}

