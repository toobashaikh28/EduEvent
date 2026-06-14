package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository extends JpaRepository<Option, Long> {

    // Useful for fetching all options belonging to a specific question
    List<Option> findByQuestionId(Long questionId);
}