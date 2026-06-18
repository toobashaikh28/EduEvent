package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.TeamAnnouncement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TeamAnnouncementRepository extends MongoRepository<TeamAnnouncement, String> {
    List<TeamAnnouncement> findAllByTeamIdOrderByCreatedAtDesc(String teamId);
}
