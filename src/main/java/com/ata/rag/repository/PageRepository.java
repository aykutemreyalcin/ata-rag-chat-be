package com.ata.rag.repository;

import com.ata.rag.model.PageEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<PageEntity, UUID> {
    Optional<PageEntity> findByUrl(String url);

    List<PageEntity> findByStatusIn(Collection<String> statuses);

    List<PageEntity> findBySourceTypeAndStatus(String sourceType, String status);

    long countByStatus(String status);
}
