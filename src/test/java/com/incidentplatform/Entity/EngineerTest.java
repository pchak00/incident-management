package com.incidentplatform.Entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineerTest {

    @Test
    void managesIndependentRolesWithoutDuplicates() {
        Engineer engineer = new Engineer("Sarah", "Backend Engineer", "sarah@example.com");

        engineer.addRole(EngineerRole.RESPONDER);
        engineer.addRole(EngineerRole.ADMIN);
        engineer.addRole(EngineerRole.ADMIN);

        assertEquals(Set.of(EngineerRole.RESPONDER, EngineerRole.ADMIN), engineer.getRoles());

        engineer.removeRole(EngineerRole.RESPONDER);

        assertEquals(Set.of(EngineerRole.ADMIN), engineer.getRoles());
    }

    @Test
    void doesNotExposeMutableRoleCollection() {
        Engineer engineer = new Engineer("Sarah", "Backend Engineer", "sarah@example.com");

        assertThrows(UnsupportedOperationException.class,
                () -> engineer.getRoles().add(EngineerRole.ADMIN));
        assertTrue(engineer.getRoles().isEmpty());
    }

    @Test
    void rejectsMissingIdentityFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new Engineer(" ", "Backend Engineer", "sarah@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new Engineer("Sarah", " ", "sarah@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new Engineer("Sarah", "Backend Engineer", " "));
        assertThrows(IllegalArgumentException.class,
                () -> new Engineer("Sarah", "Backend Engineer", "not-an-email"));
    }

    @Test
    void rejectsNullRoleWithoutChangingRoles() {
        Engineer engineer = new Engineer("Sarah", "Backend Engineer", "sarah@example.com");

        assertThrows(IllegalArgumentException.class, () -> engineer.addRole(null));

        assertTrue(engineer.getRoles().isEmpty());
    }
}
