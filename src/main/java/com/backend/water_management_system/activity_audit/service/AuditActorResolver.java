package com.backend.water_management_system.activity_audit.service;

import com.backend.water_management_system.activity_audit.model.AuditActorSnapshot;
import com.backend.water_management_system.security.UserPrincipal;
import com.backend.water_management_system.user.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditActorResolver {

    private static final AuditActorSnapshot UNAUTHENTICATED =
            new AuditActorSnapshot(null, "Unauthenticated user", null);
    private static final AuditActorSnapshot SYSTEM =
            new AuditActorSnapshot(null, "System", null);

    public AuditActorSnapshot resolveAuthenticatedWebActor() {
        UserPrincipal principal = currentUserPrincipal();
        if (principal == null) {
            throw new AccessDeniedException("Authenticated audit actor is required");
        }
        return snapshot(principal.getUser());
    }

    public AuditActorSnapshot resolvePublicWebActor() {
        UserPrincipal principal = currentUserPrincipal();
        return principal == null ? UNAUTHENTICATED : snapshot(principal.getUser());
    }

    public AuditActorSnapshot resolveSystemActor() {
        return SYSTEM;
    }

    private UserPrincipal currentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getPrincipal() instanceof UserPrincipal principal ? principal : null;
    }

    private AuditActorSnapshot snapshot(User user) {
        String displayName = user.getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = "User " + user.getId().toString().substring(0, 8);
        }
        return new AuditActorSnapshot(user.getId(), displayName, user.getRole());
    }
}
