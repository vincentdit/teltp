package tz.go.tirdo.teltp.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void acceptsAStrongPassword() {
        assertTrue(PasswordPolicy.meets("Train-Hub-2026!"));
    }

    @Test
    void rejectsTheLegacyDemoPasswordAndPartialRules() {
        assertFalse(PasswordPolicy.meets("Password123"));
        assertFalse(PasswordPolicy.meets("train-hub-2026!"));
        assertFalse(PasswordPolicy.meets("TRAIN-HUB-2026!"));
        assertFalse(PasswordPolicy.meets("TrainHubSecret!"));
        assertFalse(PasswordPolicy.meets("TrainHub2026"));
    }
}
