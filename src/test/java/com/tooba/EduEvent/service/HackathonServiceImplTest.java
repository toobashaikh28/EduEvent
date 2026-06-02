package com.tooba.EduEvent.service;

import com.tooba.EduEvent.entity.*;
import com.tooba.EduEvent.repository.*;
import com.tooba.EduEvent.service.impl.HackathonServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HackathonServiceImplTest {

    @Mock
    private LeaderboardRepository leaderboardRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private CertificateService certificateService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private HackathonServiceImpl hackathonService;

    @Test
    void testAnnounceWinners() {
        // Given
        Long hackathonId = 1L;
        Event hackathon = new Event();
        hackathon.setId(hackathonId);
        hackathon.setTitle("Test Hackathon");

        Team team1 = new Team();
        team1.setId(10L);
        team1.setName("Team Alpha");

        Team team2 = new Team();
        team2.setId(20L);
        team2.setName("Team Beta");

        User user1 = new User();
        user1.setId(100L);
        TeamMember member1 = new TeamMember();
        member1.setUser(user1);
        member1.setTeam(team1);

        User user2 = new User();
        user2.setId(200L);
        TeamMember member2 = new TeamMember();
        member2.setUser(user2);
        member2.setTeam(team2);

        when(leaderboardRepository.existsByHackathonId(hackathonId)).thenReturn(false);
        when(eventRepository.findById(hackathonId)).thenReturn(Optional.of(hackathon));

        List<Object[]> scores = Arrays.asList(
                new Object[]{10L, 95L}, // team1
                new Object[]{20L, 85L}  // team2
        );
        when(leaderboardRepository.aggregateScoresByTeam(hackathonId)).thenReturn(scores);

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team1));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team2));

        when(teamMemberRepository.findAllByTeamIdAndStatus(10L, "ACCEPTED")).thenReturn(Arrays.asList(member1));
        when(teamMemberRepository.findAllByTeamIdAndStatus(20L, "ACCEPTED")).thenReturn(Arrays.asList(member2));

        // When
        hackathonService.announceWinners(hackathonId);

        // Then
        ArgumentCaptor<Leaderboard> leaderboardCaptor = ArgumentCaptor.forClass(Leaderboard.class);
        verify(leaderboardRepository, times(2)).save(leaderboardCaptor.capture());

        List<Leaderboard> savedLeaderboards = leaderboardCaptor.getAllValues();
        assertEquals(2, savedLeaderboards.size());
        
        assertEquals(10L, savedLeaderboards.get(0).getTeam().getId());
        assertEquals(1, savedLeaderboards.get(0).getRank());
        
        assertEquals(20L, savedLeaderboards.get(1).getTeam().getId());
        assertEquals(2, savedLeaderboards.get(1).getRank());

        verify(certificateService, times(1)).generate(eq(user1), eq(hackathon), eq(1));
        verify(certificateService, times(1)).generate(eq(user2), eq(hackathon), eq(2));

        verify(notificationService, times(1)).send(eq(100L), anyString(), contains("#1"));
        verify(notificationService, times(1)).send(eq(200L), anyString(), contains("#2"));
    }
}
