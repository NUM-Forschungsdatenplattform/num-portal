package de.vitagroup.num.domain.admin;

import lombok.Data;

import java.util.Set;

@Data
public class User {
    private String id;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private Set<String> roles;
}
