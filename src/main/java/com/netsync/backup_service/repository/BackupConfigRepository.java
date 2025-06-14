package com.netsync.backup_service.repository;

import com.netsync.backup_service.model.BackupConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupConfigRepository 
        extends JpaRepository<BackupConfig, Long> {
    // Spring Data JPA infiere la consulta: SELECT c FROM BackupConfig c ORDER BY c.id ASC LIMIT 1
    Optional<BackupConfig> findFirstByOrderByIdAsc();
}
