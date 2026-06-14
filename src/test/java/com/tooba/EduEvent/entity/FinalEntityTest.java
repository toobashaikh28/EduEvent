package com.tooba.EduEvent.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinalEntityTest {
    @Test
    void testTeamAndSubmission() {
        User leader = User.builder().name("Tooba").build();
        Event hackathon = Event.builder().title("AI Hack").build();
        
        Team team = Team.builder()
                .name("Alpha Squad")
                .leader(leader)
                .hackathon(hackathon)
                .build();

        Submission sub = Submission.builder()
                .team(team)
                .hackathon(hackathon)
                .title("EduEvent App")
                .build();

        assertNotNull(sub.getTeam().getLeader());
        assertEquals("Tooba", sub.getTeam().getLeader().getName());
        System.out.println("Final entities test passed!");
    }
}