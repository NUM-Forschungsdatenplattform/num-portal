package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByStudyId(Long studyId);
}
