package com.ata.rag.repository;

import com.ata.rag.model.ChatQueryEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatQueryRepository extends JpaRepository<ChatQueryEntity, UUID> {}
