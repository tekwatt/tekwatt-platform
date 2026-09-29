package com.tekwatt.auth.repository;

import com.tekwatt.auth.entity.UsedOtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedOtpTokenRepository extends JpaRepository<UsedOtpToken, String> { }
