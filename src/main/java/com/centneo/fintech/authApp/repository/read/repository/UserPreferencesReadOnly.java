package com.centneo.fintech.authApp.repository.read.repository;

import com.centneo.fintech.authApp.dto.ResponseDto;
import com.centneo.fintech.authApp.model.auth.UserPreference;
import com.centneo.fintech.authApp.repository.write.repository.UserPreferencesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesReadOnly extends UserPreferencesRepository {


    UserPreference findByUserId(Long id);
}
