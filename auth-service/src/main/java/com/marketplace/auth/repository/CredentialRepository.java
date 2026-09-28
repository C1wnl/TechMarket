package com.marketplace.auth.repository;

import com.marketplace.auth.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByUserId(Long userId);
}