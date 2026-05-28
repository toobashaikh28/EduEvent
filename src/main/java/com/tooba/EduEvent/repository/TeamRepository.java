package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.Team; 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; // ADD THIS IMPORT
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    Optional<Team> findByJoinCode(String joinCode);

    List<Team> findAllByHackathonId(Long hackathonId); // Uses List here
}