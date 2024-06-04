package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByProjectId(Long projectId);
}
