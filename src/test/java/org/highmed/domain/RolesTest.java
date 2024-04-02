package org.highmed.domain;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.highmed.domain.model.Roles;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class RolesTest {

    @Test
    public void isSuperAdminTest() {
        Assert.assertTrue(Roles.isSuperAdmin(List.of(Roles.SUPER_ADMIN, Roles.CONTENT_ADMIN)));
        Assert.assertFalse(Roles.isSuperAdmin(List.of(Roles.ORGANIZATION_ADMIN, Roles.STUDY_COORDINATOR)));
    }

    @Test
    public void isOrganizationAdminTest() {
        Assert.assertTrue(Roles.isOrganizationAdmin(List.of(Roles.SUPER_ADMIN, Roles.ORGANIZATION_ADMIN)));
        Assert.assertFalse(Roles.isOrganizationAdmin(List.of(Roles.RESEARCHER, Roles.STUDY_COORDINATOR)));
    }

    @Test
    public void isProjectLeadTest() {
        Assert.assertTrue(Roles.isProjectLead(List.of(Roles.STUDY_APPROVER, Roles.STUDY_COORDINATOR)));
        Assert.assertFalse(Roles.isProjectLead(List.of(Roles.RESEARCHER, Roles.STUDY_APPROVER)));
    }

    @Test
    public void extractRolesTest() {
        Jwt jwt = Jwt.withTokenValue("1111")
                .subject("user-uuid-1234")
                .issuedAt(Instant.now())
                .claim("name", "John")
                .claim("email", "john.doe@vitagroup.de")
                .claim("realm_access", Map.of("roles", List.of(Roles.STUDY_APPROVER, Roles.STUDY_COORDINATOR)))
                .claim("username", "john.doe")
                .header("dummy", "dummy")
                .build();
        List<String> result = Roles.extractRoles(jwt);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains(Roles.STUDY_COORDINATOR));
        Assert.assertTrue(result.contains(Roles.STUDY_APPROVER));
    }
    @Test
    public void shouldReturnEmptyListExtractRolesTest() {
        Jwt jwt = Jwt.withTokenValue("1112")
                .subject("user-uuid-1234")
                .issuedAt(Instant.now())
                .claim("name", "John")
                .claim("email", "john.doe@vitagroup.de")
                .claim("username", "john.doe")
                .header("dummy", "dummy")
                .build();
        List<String> result = Roles.extractRoles(jwt);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void isAllowedToSetTest() {
        Assert.assertTrue(Roles.isAllowedToSet(Roles.RESEARCHER, List.of(Roles.ORGANIZATION_ADMIN)));
        Assert.assertTrue(Roles.isAllowedToSet(Roles.ORGANIZATION_ADMIN, List.of(Roles.SUPER_ADMIN)));
        Assert.assertTrue(Roles.isAllowedToSet(Roles.CRITERIA_EDITOR, List.of(Roles.SUPER_ADMIN, Roles.STUDY_COORDINATOR)));
        Assert.assertTrue(Roles.isAllowedToSet(Roles.STUDY_COORDINATOR, List.of(Roles.ORGANIZATION_ADMIN, Roles.STUDY_COORDINATOR)));
        Assert.assertFalse(Roles.isAllowedToSet(Roles.STUDY_COORDINATOR, List.of(Roles.STUDY_COORDINATOR)));
    }

}
