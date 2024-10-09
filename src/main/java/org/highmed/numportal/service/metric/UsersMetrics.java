package org.highmed.numportal.service.metric;

import org.highmed.numportal.domain.model.admin.UserDetails;
import org.highmed.numportal.domain.repository.UserDetailsRepository;
import org.highmed.numportal.web.feign.KeycloakFeign;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Custom prometheus metric, to detect number of user in different states, active, inactive or unapproved.
 */
@Getter
@Component
public class UsersMetrics {

  private double unapprovedUsers;
  private double activeUsers;
  private double inactiveUsers;

  public UsersMetrics(MeterRegistry registry, UserDetailsRepository userDetailsRepository, KeycloakFeign keycloakFeign) {
    Gauge.builder("custom.metric.user.unapproved.counter", this::getUnapprovedUsers)
         .description("Unapproved users")
         .register(registry);
    Gauge.builder("custom.metric.user.active.counter", this::getActiveUsers)
         .description("Active users")
         .register(registry);
    Gauge.builder("custom.metric.user.inactive.counter", this::getInactiveUsers)
         .description("Inactive users")
         .register(registry);

    Optional<List<UserDetails>> unapproved = userDetailsRepository.findAllByApproved(false);
    unapproved.ifPresent(userDetails -> this.unapprovedUsers = userDetails.size());
    this.inactiveUsers = keycloakFeign.countUsers(false);
    // decrease because of service account
    this.activeUsers = keycloakFeign.countUsers(true) - 1;

  }

  public void addNewUserAsUnapproved() {
    this.unapprovedUsers++;
  }

  public void approveUser() {
    this.unapprovedUsers--;
  }

  public void updateCountStatus(@NotNull boolean active) {
    if (active) {
      this.incrementActiveUsers();
    } else {
      this.incrementInactiveUsers();
    }
  }

  private void incrementActiveUsers() {
    this.activeUsers++;
    this.inactiveUsers--;
  }

  private void incrementInactiveUsers() {
    this.inactiveUsers++;
    this.activeUsers--;
  }
}
