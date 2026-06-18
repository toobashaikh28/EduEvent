package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.AssignedJudgeResponse;
import com.tooba.EduEvent.dto.response.JudgeHackathonResponse;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;

import java.util.List;

public interface JudgeService {
    void assignJudge(String hackathonId, String judgeUserId, String adminEmail);
    List<AssignedJudgeResponse> getAssignedJudges(String hackathonId, String adminEmail);
    void removeJudge(String hackathonId, String judgeUserId, String adminEmail);
    List<JudgeHackathonResponse> getMyHackathons(String judgeEmail);
    List<SubmissionResponse> getSubmissionsForJudge(String hackathonId, String judgeEmail);
    ScoreResponse scoreSubmission(ScoreRequest request, String judgeEmail);
    List<ScoreResponse> getMyScores(String hackathonId, String judgeEmail);
}