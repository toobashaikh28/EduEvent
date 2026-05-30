package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.SubmissionRequest;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {
    SubmissionResponse submit(Long hackathonId, String userEmail, SubmissionRequest request, MultipartFile file);
    SubmissionResponse editSubmission(Long hackathonId, String userEmail, SubmissionRequest request, MultipartFile file);
    SubmissionResponse getMySubmission(Long hackathonId, String userEmail);
}