package tz.go.tirdo.teltp.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Convenience accessor for the authenticated principal's username. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Optional<String> username() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        return Optional.ofNullable(auth.getName());
    }

    public static String requireUsername() {
        return username().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
