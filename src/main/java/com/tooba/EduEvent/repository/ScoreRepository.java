package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Score;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScoreRepository extends MongoRepository<Score, String> {

    // Duplicate score check
    boolean existsByJudgeIdAndSubmissionId(String judgeId, String submissionId);

    // Judge views their own scores (filtered to a hackathon's submissions in the service)
    List<Score> findByJudgeIdAndSubmissionIdIn(String judgeId, List<String> submissionIds);

    // All scores for a set of submissions (used to aggregate hackathon results)
    List<Score> findBySubmissionIdIn(List<String> submissionIds);

    List<Score> findBySubmissionId(String submissionId);
}
