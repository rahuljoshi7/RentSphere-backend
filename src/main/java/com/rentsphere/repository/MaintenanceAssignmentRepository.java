package com.rentsphere.repository;

import com.rentsphere.entity.MaintenanceAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceAssignmentRepository extends JpaRepository<MaintenanceAssignment, Long> {

    List<MaintenanceAssignment> findByRequestId(Long requestId);

    Optional<MaintenanceAssignment> findByRequestIdAndStaffId(Long requestId, Long staffId);

    boolean existsByRequestIdAndStaffId(Long requestId, Long staffId);
}
