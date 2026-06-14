package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    // Judge views all their own scores for a hackathon
    List<Score> findAllByJudgeIdAndSubmissionHackathonId(Long judgeId, Long hackathonId);
    // Duplicate score check
    boolean existsByJudgeIdAndSubmissionId(Long judgeId, Long submissionId);
}