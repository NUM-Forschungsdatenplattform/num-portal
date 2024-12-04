package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, PagingAndSortingRepository<Message, Long> {

  Page<Message> findMessages(Pageable pageable);
}
