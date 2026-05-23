package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    // Useful for fetching all options belonging to a specific question
    List<Option> findByQuestionId(Long questionId);
}