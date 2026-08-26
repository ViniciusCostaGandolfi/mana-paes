package vgandolfi.dev.mana_paes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vgandolfi.dev.mana_paes.domain.model.DailyReportItem;

import java.util.List;
import java.util.UUID;

public interface DailyReportItemRepository extends JpaRepository<DailyReportItem, UUID> {

    List<DailyReportItem> findByReportId(UUID reportId);
}