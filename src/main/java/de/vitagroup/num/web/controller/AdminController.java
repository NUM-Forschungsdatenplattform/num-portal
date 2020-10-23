package de.vitagroup.num.web.controller;

import de.vitagroup.num.domain.admin.Role;
import de.vitagroup.num.domain.admin.User;
import de.vitagroup.num.service.AdminService;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Set;

@RestController
@AllArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/admin/roles/{role}/users")
    @ApiOperation(value = "Retrieves a list of users that have given role")
    public ResponseEntity<Set<User>> getUsersByRole(@NotNull @PathVariable String role) {
        return ResponseEntity.ok(adminService.getUsersByRole(role));
    }

    @GetMapping("/admin/users/{userId}")
    @ApiOperation(value = "Retrieves the information about the given user")
    public ResponseEntity<User> getUser(@NotNull @PathVariable String userId) {
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @GetMapping("/admin/users/{userId}/roles")
    @ApiOperation(value = "Retrieves the information about the given user")
    public ResponseEntity<Set<Role>> getRolesOfUser(@NotNull @PathVariable String userId) {
        return ResponseEntity.ok(adminService.getRolesOfUser(userId));
    }

}
