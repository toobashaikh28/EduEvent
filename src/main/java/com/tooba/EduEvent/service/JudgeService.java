package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.ScoreRequest;
import com.tooba.EduEvent.dto.response.ScoreResponse;
import com.tooba.EduEvent.dto.response.SubmissionResponse;

import java.util.List;

public interface JudgeService {
    void assignJudge(Long hackathonId, Long judgeUserId, String adminEmail);
    List<SubmissionResponse> getSubmissionsForJudge(Long hackathonId, String judgeEmail);
    ScoreResponse scoreSubmission(ScoreRequest request, String judgeEmail);
    List<ScoreResponse> getMyScores(Long hackathonId, String judgeEmail);
}