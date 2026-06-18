package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.JudgeAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface JudgeAssignmentRepository extends MongoRepository<JudgeAssignment, String> {
    boolean existsByJudgeIdAndHackathonId(String judgeId, String hackathonId);
    Optional<JudgeAssignment> findByJudgeIdAndHackathonId(String judgeId, String hackathonId);
    List<JudgeAssignment> findByJudgeId(String judgeId);
    List<JudgeAssignment> findByHackathonId(String hackathonId);
}
