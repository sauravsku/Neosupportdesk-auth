package com.centneo.fintech.authApp.repository.write.repository;

import com.centneo.fintech.authApp.model.auth.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreference, Long> {
}
