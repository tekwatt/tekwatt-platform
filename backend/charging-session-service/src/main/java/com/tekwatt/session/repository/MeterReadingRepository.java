package com.tekwatt.session.repository;

import com.tekwatt.session.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MeterReadingRepository extends JpaRepository<MeterReading, UUID> {}
