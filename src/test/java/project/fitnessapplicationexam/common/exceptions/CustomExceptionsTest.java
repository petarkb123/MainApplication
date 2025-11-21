package project.fitnessapplicationexam.common.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionsTest {

    @Test
    void emptyTemplateException_hasMessage() {
        EmptyTemplateException ex = new EmptyTemplateException("Test message");
        
        assertEquals("Test message", ex.getMessage());
        assertNotNull(ex);
    }

    @Test
    void templateNameConflictException_hasMessage() {
        TemplateNameConflictException ex = new TemplateNameConflictException("Test message");
        
        assertEquals("Test message", ex.getMessage());
        assertNotNull(ex);
    }

    @Test
    void invalidAvatarUrlException_hasMessage() {
        InvalidAvatarUrlException ex = new InvalidAvatarUrlException("Test message");
        
        assertEquals("Test message", ex.getMessage());
        assertNotNull(ex);
    }

    @Test
    void workoutAlreadyFinishedException_hasMessage() {
        WorkoutAlreadyFinishedException ex = new WorkoutAlreadyFinishedException("Test message");
        
        assertEquals("Test message", ex.getMessage());
        assertNotNull(ex);
    }
}

