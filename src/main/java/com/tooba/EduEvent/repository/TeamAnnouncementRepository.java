package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.TeamAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamAnnouncementRepository extends JpaRepository<TeamAnnouncement, Long> {
    List<TeamAnnouncement> findAllByTeamIdOrderByCreatedAtDesc(Long teamId);
}