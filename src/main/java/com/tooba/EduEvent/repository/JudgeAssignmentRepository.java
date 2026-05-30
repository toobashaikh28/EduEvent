package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.JudgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JudgeAssignmentRepository extends JpaRepository<JudgeAssignment, Long> {
    boolean existsByJudgeIdAndHackathonId(Long judgeId, Long hackathonId);
    Optional<JudgeAssignment> findByJudgeIdAndHackathonId(Long judgeId, Long hackathonId);
}