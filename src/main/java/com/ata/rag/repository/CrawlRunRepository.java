package com.ata.rag.repository;

import com.ata.rag.model.CrawlRunEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlRunRepository extends JpaRepository<CrawlRunEntity, UUID> {
    Optional<CrawlRunEntity> findFirstByRunTypeOrderByStartedAtDesc(String runType);
}
