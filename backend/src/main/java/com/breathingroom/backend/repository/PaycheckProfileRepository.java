package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.PaycheckProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaycheckProfileRepository extends JpaRepository<PaycheckProfile, Long> {
}
