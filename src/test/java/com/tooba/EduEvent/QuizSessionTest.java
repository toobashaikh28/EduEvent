package com.tooba.EduEvent.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuizSessionTest {

    @Test
    @DisplayName("Should build QuizSession and Violation without NPE")
    void testQuizSessionAndViolationBuilder() {
        // 1. Arrange: Setup User and Quiz (Mocking IDs)
        User student = User.builder().id(10L).name("Student Ali").build();
        Quiz javaQuiz = Quiz.builder().id(1L).durationMinutes(30).build();

        // 2. Act: Build Session
        QuizSession session = QuizSession.builder()
                .user(student)
                .quiz(javaQuiz)
                .status("ONGOING")
                .build();

        // 3. Act: Build Violation
        Violation violation = Violation.builder()
                .session(session)
                .type("TAB_SWITCH")
                .build();

        // 4. Assert
        assertNotNull(session);
        assertEquals("ONGOING", session.getStatus());
        assertEquals("Student Ali", session.getUser().getName());
        
        assertNotNull(violation.getSession());
        assertEquals("TAB_SWITCH", violation.getType());
        
        System.out.println("QuizSession and Violation Builder test passed!");
    }
}