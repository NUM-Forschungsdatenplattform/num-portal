package de.vitagroup.num.domain.repository;

import de.vitagroup.num.domain.Content;
import de.vitagroup.num.domain.ContentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentItemRepository extends JpaRepository<Content, Long> {

  Optional<List<Content>> findByType(ContentType type);
}
