package org.highmed.domain.repository;

import org.highmed.domain.model.Content;
import org.highmed.domain.model.ContentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentItemRepository extends JpaRepository<Content, Long> {

  Optional<List<Content>> findByType(ContentType type);
}
