package ma.entraide.suivi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        int jwtTtlHours,
        boolean cookieSecure,
        String defaultPassword,
        String adminEmail,
        String adminPassword,
        String emailDomain) {
}
