package sn.jaakha.backend.domain.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jaakha.backend.domain.enums.ReportStatus;
import sn.jaakha.backend.domain.model.Report;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
}
