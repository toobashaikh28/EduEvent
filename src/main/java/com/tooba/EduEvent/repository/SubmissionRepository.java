package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByTeamIdAndHackathonId(Long teamId, Long hackathonId);
    List<Submission> findAllByHackathonId(Long hackathonId);
}