package vgandolfi.dev.mana_paes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita a auditoria JPA (preenchimento automático de
 * {@code createdAt}/{@code updatedAt} via {@code AuditingEntityListener}).
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}