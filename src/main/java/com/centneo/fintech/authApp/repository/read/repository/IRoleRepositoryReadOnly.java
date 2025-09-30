package com.centneo.fintech.authApp.repository.read.repository;

import com.centneo.fintech.authApp.model.auth.Role;
import com.centneo.fintech.authApp.repository.write.repository.IRoleRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IRoleRepositoryReadOnly extends IRoleRepository {

    Role findByName(String name);
}
