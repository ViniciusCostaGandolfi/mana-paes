package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.DailyReport;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Optional<DailyReport> findByTenantIdAndReportDate(UUID tenantId, LocalDate reportDate);
}