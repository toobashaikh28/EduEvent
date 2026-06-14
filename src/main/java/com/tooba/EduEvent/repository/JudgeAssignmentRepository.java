package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.JudgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Long> {
    boolean existsByJudgeIdAndHackathonId(Long judgeId, Long hackathonId);
    Optional<JudgeAssignment> findByJudgeIdAndHackathonId(Long judgeId, Long hackathonId);
}