package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TeamRepository extends MongoRepository<Team, String> {

    Optional<Team> findByJoinCode(String joinCode);

    List<Team> findAllByHackathonId(String hackathonId);
}
