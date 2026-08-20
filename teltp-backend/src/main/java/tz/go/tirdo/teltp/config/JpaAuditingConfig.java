package tz.go.tirdo.teltp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            var ctx = SecurityContextHolder.getContext().getAuthentication();
            if (ctx == null || !ctx.isAuthenticated() || "anonymousUser".equals(ctx.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(ctx.getName());
        };
    }
}
