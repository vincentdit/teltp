package tz.go.tirdo.teltp.auth.service;

import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void require(String password) {
        if (password == null
                || password.length() < 10
                || !password.chars().anyMatch(Character::isLetter)
                || !password.chars().anyMatch(Character::isDigit)) {
            throw new BusinessRuleException(
                    "Password must be at least 10 characters and include a letter and a digit");
        }
    }
}
