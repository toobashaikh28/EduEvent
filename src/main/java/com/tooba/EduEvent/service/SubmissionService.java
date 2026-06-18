package com.tooba.EduEvent.service;

import com.tooba.EduEvent.dto.request.SubmissionRequest;
import com.tooba.EduEvent.dto.response.SubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {
    SubmissionResponse submit(String hackathonId, String userEmail, SubmissionRequest request, MultipartFile file);
    SubmissionResponse editSubmission(String hackathonId, String userEmail, SubmissionRequest request, MultipartFile file);
    SubmissionResponse getMySubmission(String hackathonId, String userEmail);
}