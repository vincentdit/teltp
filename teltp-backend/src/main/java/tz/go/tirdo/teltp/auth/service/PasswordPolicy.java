package tz.go.tirdo.teltp.auth.service;

import tz.go.tirdo.teltp.common.exception.BusinessRuleException;

/** Rules for new passwords. Existing stored passwords are not re-checked at login. */
public final class PasswordPolicy {
    public static final String MESSAGE =
            "Password must be at least 12 characters and include an uppercase letter, a lowercase letter, a number, and a special character";

    private PasswordPolicy() {}

    public static void require(String password) {
        if (!meets(password)) throw new BusinessRuleException(MESSAGE);
    }

    public static boolean meets(String password) {
        if (password == null || password.length() < 12 || password.length() > 200) return false;
        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }
        return upper && lower && digit && special;
    }
}
