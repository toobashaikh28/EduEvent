package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends MongoRepository<Submission, String> {
    Optional<Submission> findByTeamIdAndHackathonId(String teamId, String hackathonId);
    List<Submission> findAllByHackathonId(String hackathonId);
}
