package com.incidentplatform.Entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@Entity
@Table(name = "engineers")
public class Engineer {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String engineerName;

    @Column(nullable = false)
    private String engineerPosition;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "engineer_roles", joinColumns = @JoinColumn(name = "engineer_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<EngineerRole> roles = new HashSet<>();

    protected Engineer() {
    }

    public Engineer(String engineerName, String engineerPosition, String email) {
        String validEngineerName = requireNonBlank(engineerName, "Engineer name");
        String validEngineerPosition = requireNonBlank(engineerPosition, "Engineer position");
        String validEmail = requireValidEmail(email);

        this.engineerName = validEngineerName;
        this.engineerPosition = validEngineerPosition;
        this.email = validEmail;
        checkInvariant();
    }

    public void addRole(EngineerRole role) {
        checkInvariant();
        checkArgument(role != null, "Role cannot be null");

        roles.add(role);
        checkInvariant();
    }

    public void removeRole(EngineerRole role) {
        checkInvariant();
        checkArgument(role != null, "Role cannot be null");

        roles.remove(role);
        checkInvariant();
    }

    public Long getId() {
        return id;
    }

    public String getEngineerName() {
        return engineerName;
    }

    public String getEngineerPosition() {
        return engineerPosition;
    }

    public String getEmail() {
        return email;
    }

    public Set<EngineerRole> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    private void checkInvariant() {
        checkState(isNonBlank(engineerName), "Engineer name cannot be blank");
        checkState(isNonBlank(engineerPosition), "Engineer position cannot be blank");
        checkState(isValidEmail(email), "Engineer email must be valid");
        checkState(roles != null, "Engineer roles cannot be null");
        checkState(!roles.contains(null), "Engineer roles cannot contain null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        checkArgument(isNonBlank(value), "%s cannot be blank", fieldName);
        return value.trim();
    }

    private static String requireValidEmail(String value) {
        String email = requireNonBlank(value, "Email");
        checkArgument(isValidEmail(email), "Email must be valid");
        return email;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isValidEmail(String value) {
        return isNonBlank(value) && EMAIL_PATTERN.matcher(value).matches();
    }
}
