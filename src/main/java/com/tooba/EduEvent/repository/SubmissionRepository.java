package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByTeamIdAndHackathonId(Long teamId, Long hackathonId);
    List<Submission> findAllByHackathonId(Long hackathonId);
    // --- ANALYTICS QUERIES ---
    @Query("SELECT s.hackathon.title, COUNT(s) FROM Submission s GROUP BY s.hackathon.title")
    List<Object[]> countSubmissionsPerHackathon();
}