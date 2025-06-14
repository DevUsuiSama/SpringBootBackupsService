package com.netsync.backup_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.netsync.backup_service.model.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, String> {
    boolean existsByName(String name);
}