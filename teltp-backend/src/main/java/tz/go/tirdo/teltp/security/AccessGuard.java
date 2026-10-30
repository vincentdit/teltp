package tz.go.tirdo.teltp.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tz.go.tirdo.teltp.auth.service.UserService;

/** Object-level checks on top of role security. */
@Component
public class AccessGuard {

    private final UserService users;

    public AccessGuard(UserService users) {
        this.users = users;
    }

    public String currentUserUuid() {
        return users.uuidForUsername(CurrentUser.requireUsername());
    }

    public boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (String role : roles) {
            String granted = "ROLE_" + role;
            if (auth.getAuthorities().stream().anyMatch(a -> granted.equals(a.getAuthority()))) return true;
        }
        return false;
    }

    /** The user themselves, or one of the listed staff roles. */
    public void assertSelfOrRoles(String userUuid, String... roles) {
        if (userUuid != null && userUuid.equals(currentUserUuid())) return;
        if (hasAnyRole(roles)) return;
        throw new AccessDeniedException("Access denied");
    }

    /** Invoices: the payer, or finance and admin. Organization invoices stay with staff. */
    public void assertCanReadInvoice(String payerUuid, String payerType) {
        if (hasAnyRole("ADMIN", "FINANCE_OFFICER")) return;
        if ("USER".equals(payerType) && payerUuid != null && payerUuid.equals(currentUserUuid())) return;
        throw new AccessDeniedException("Access denied");
    }
}
