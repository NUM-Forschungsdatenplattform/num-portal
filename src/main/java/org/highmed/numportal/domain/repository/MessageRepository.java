package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, CustomProjectRepository {

}
