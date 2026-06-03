package com.tooba.EduEvent.repository;

import com.tooba.EduEvent.entity.AnnouncementComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementCommentRepository extends JpaRepository<AnnouncementComment, Long> {
}