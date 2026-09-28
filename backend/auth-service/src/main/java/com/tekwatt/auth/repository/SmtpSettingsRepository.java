package com.tekwatt.auth.repository;

import com.tekwatt.auth.entity.SmtpSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpSettingsRepository extends JpaRepository<SmtpSettings, Integer> { }
