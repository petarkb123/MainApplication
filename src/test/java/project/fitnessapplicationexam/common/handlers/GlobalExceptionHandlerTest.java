package project.fitnessapplicationexam.common.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import project.fitnessapplicationexam.common.exceptions.EmptyTemplateException;
import project.fitnessapplicationexam.common.exceptions.InvalidAvatarUrlException;
import project.fitnessapplicationexam.common.exceptions.TemplateNameConflictException;
import project.fitnessapplicationexam.common.exceptions.WorkoutAlreadyFinishedException;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleEmptyTemplate_returnsBadRequest() {
        EmptyTemplateException ex = new EmptyTemplateException("Empty template");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleEmptyTemplate(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Template", response.getBody().get("error"));
        assertEquals("Empty template", response.getBody().get("message"));
    }

    @Test
    void handleTemplateNameConflict_returnsBadRequest() {
        TemplateNameConflictException ex = new TemplateNameConflictException("Template exists");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleTemplateNameConflict(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Template Name Conflict", response.getBody().get("error"));
    }

    @Test
    void handleInvalidAvatar_returnsBadRequest() {
        InvalidAvatarUrlException ex = new InvalidAvatarUrlException("Invalid URL");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleInvalidAvatar(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Avatar URL", response.getBody().get("error"));
    }

    @Test
    void handleAlreadyFinished_returnsBadRequest() {
        WorkoutAlreadyFinishedException ex = new WorkoutAlreadyFinishedException("Already finished");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleAlreadyFinished(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleAccessDenied(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    @Test
    void handleMethodNotAllowed_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleMethodNotAllowed(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Method Not Allowed", response.getBody().get("error"));
    }

    @Test
    void handleConstraintViolation_returnsBadRequest() {
        ConstraintViolationException ex = new ConstraintViolationException("Constraint violated", new HashSet<>());
        when(request.getRequestURI()).thenReturn("/api/test");
        when(request.getHeader("Accept")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        Object result = handler.handleConstraintViolation(ex, request);
        
        assertTrue(result instanceof ResponseEntity);
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Constraint Violation", response.getBody().get("error"));
    }
}

