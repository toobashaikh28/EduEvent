package com.tooba.EduEvent.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuizHierarchyTest {

    @Test
    @DisplayName("Should build Quiz, Question, and Option chain without NPE")
    void testQuizHierarchyBuilder() {
        // 1. Create the Quiz (linked to a mock event)
        Event mockEvent = Event.builder().id(1L).title("Java Basics").build();
        Quiz quiz = Quiz.builder()
                .event(mockEvent)
                .durationMinutes(20)
                .passScore(new java.math.BigDecimal("60.00"))
                .build();

        // 2. Create a Question linked to that Quiz
        Question question = Question.builder()
                .quiz(quiz)
                .questionText("What is the parent class of all Java classes?")
                .build();

        // 3. Create an Option linked to that Question
        Option option = Option.builder()
                .question(question)
                .optionText("Object")
                .isCorrect(true)
                .build();

        // 4. Assertions to verify the chain
        assertNotNull(option.getQuestion(), "Option should have a parent Question");
        assertEquals("Object", option.getOptionText());
        
        assertNotNull(option.getQuestion().getQuiz(), "Question should have a parent Quiz");
        assertEquals(20, option.getQuestion().getQuiz().getDurationMinutes());
        
        assertNotNull(option.getQuestion().getQuiz().getEvent(), "Quiz should have a parent Event");
        assertEquals("Java Basics", option.getQuestion().getQuiz().getEvent().getTitle());

        System.out.println("Quiz Hierarchy Test: SUCCESS");
    }
}