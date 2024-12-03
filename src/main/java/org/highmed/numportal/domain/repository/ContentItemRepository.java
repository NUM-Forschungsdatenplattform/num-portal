package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Content;
import org.highmed.numportal.domain.model.ContentType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentItemRepository extends JpaRepository<Content, Long> {

  Optional<List<Content>> findByType(ContentType type);
}
