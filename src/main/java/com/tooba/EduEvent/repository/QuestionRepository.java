package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuestionRepository extends MongoRepository<Question, String> {
    List<Question> findByQuizId(String quizId);
}
