package org.highmed.numportal.domain.repository;

import org.highmed.numportal.domain.model.Message;
import org.highmed.numportal.domain.model.admin.UserDetails;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, PagingAndSortingRepository<Message, Long> {

  @Query("SELECT m FROM Message m WHERE :userDetails NOT MEMBER OF m.readByUsers AND m.startDate <= :now AND m.endDate >= :now")
  List<Message> findAllActiveMessagesNotReadByUser(@Param("userDetails") UserDetails userDetails, @Param("now")LocalDateTime now);
}
