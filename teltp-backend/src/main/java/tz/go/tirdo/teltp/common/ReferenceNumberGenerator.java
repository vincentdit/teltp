package tz.go.tirdo.teltp.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Generates human-facing reference numbers of the form
 * {@code TELTP-{MODULE}-{YEAR}-{00001}} using a per-module/year DB sequence
 * table so numbers are gapless and concurrency-safe.
 */
@Component
public class ReferenceNumberGenerator {

    private final JdbcTemplate jdbc;

    public ReferenceNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String module) {
        int year = Year.now().getValue();
        jdbc.update(
            "INSERT INTO reference_sequence(module, ref_year, current_value) VALUES (?,?,1) " +
            "ON DUPLICATE KEY UPDATE current_value = current_value + 1",
            module, year);
        Long value = jdbc.queryForObject(
            "SELECT current_value FROM reference_sequence WHERE module = ? AND ref_year = ?",
            Long.class, module, year);
        return "TELTP-%s-%d-%05d".formatted(module, year, value);
    }
}
