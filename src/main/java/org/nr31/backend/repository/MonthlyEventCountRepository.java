package org.nr31.backend.repository;

import org.nr31.backend.model.MonthlyEventCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyEventCountRepository extends JpaRepository<MonthlyEventCount, Long> {
    Optional<MonthlyEventCount> findByYearAndMonth(int year, int month);
}
