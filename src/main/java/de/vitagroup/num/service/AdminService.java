package de.vitagroup.num.service;

import de.vitagroup.num.config.KeycloakConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AdminService {
    private final Keycloak keycloak;
    private final KeycloakConfig config;

    public Set<UserRepresentation> getUsersByRole(String role) {
        return keycloak.realm(config.getRealm()).roles().get(role).getRoleUserMembers();
    }

    public UserRepresentation getUser(String userId) {
        UserRepresentation userRepresentation = keycloak.realm(config.getRealm()).users().get(userId).toRepresentation();
        // Query for roles as they're not returned by user query
        MappingsRepresentation mappingsRepresentation = getUsersRoles(userId);
        List<String> roles = mappingsRepresentation.getRealmMappings().stream().map(roleRepresentation -> roleRepresentation.getName()).collect(Collectors.toList());
        userRepresentation.setRealmRoles(roles);
        return userRepresentation;
    }

    public MappingsRepresentation getUsersRoles(String userId) {
        return keycloak.realm(config.getRealm()).users().get(userId).roles().getAll();
    }

}
