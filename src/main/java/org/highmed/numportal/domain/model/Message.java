package org.highmed.numportal.domain.model;

import org.highmed.numportal.domain.model.admin.UserDetails;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Message implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  private String text;

  private LocalDateTime startDate;

  private LocalDateTime endDate;

  private MessageType type;

  private boolean sessionBased;

  private boolean markAsDeleted;

  @OneToMany
  @JoinTable(
      name = "read_message_by_users",
      joinColumns = @JoinColumn(name = "message_id"),
      inverseJoinColumns = @JoinColumn(name = "user_details_id"))
  private List<UserDetails> readByUsers = new ArrayList<>();

}
