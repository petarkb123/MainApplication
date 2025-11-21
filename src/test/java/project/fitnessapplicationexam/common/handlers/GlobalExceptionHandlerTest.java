package project.fitnessapplicationexam.common.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void handleEmptyTemplate_returnsBadRequest() {
        EmptyTemplateException ex = new EmptyTemplateException("Empty template");
        
        ResponseEntity<Map<String, Object>> response = handler.handleEmptyTemplate(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Template", response.getBody().get("error"));
        assertEquals("Empty template", response.getBody().get("message"));
    }

    @Test
    void handleTemplateNameConflict_returnsBadRequest() {
        TemplateNameConflictException ex = new TemplateNameConflictException("Template exists");
        
        ResponseEntity<Map<String, Object>> response = handler.handleTemplateNameConflict(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Template Name Conflict", response.getBody().get("error"));
    }

    @Test
    void handleInvalidAvatar_returnsBadRequest() {
        InvalidAvatarUrlException ex = new InvalidAvatarUrlException("Invalid URL");
        
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidAvatar(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid Avatar URL", response.getBody().get("error"));
    }

    @Test
    void handleAlreadyFinished_returnsBadRequest() {
        WorkoutAlreadyFinishedException ex = new WorkoutAlreadyFinishedException("Already finished");
        
        ResponseEntity<Map<String, Object>> response = handler.handleAlreadyFinished(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bad Request", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_returnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(ex);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().get("error"));
    }

    @Test
    void handleMethodNotAllowed_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        
        ResponseEntity<Map<String, Object>> response = handler.handleMethodNotAllowed(ex);
        
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Method Not Allowed", response.getBody().get("error"));
    }

    @Test
    void handleConstraintViolation_returnsBadRequest() {
        ConstraintViolationException ex = new ConstraintViolationException("Constraint violated", new HashSet<>());
        
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Constraint Violation", response.getBody().get("error"));
    }
}

