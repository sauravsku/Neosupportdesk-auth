package com.centneo.fintech.authApp.repository.read.repository;

import com.centneo.fintech.authApp.model.auth.UserRole;
import com.centneo.fintech.authApp.repository.write.repository.IUserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserRoleRepositoryReadOnly extends IUserRoleRepository {

    List<UserRole> findAllByUserId(Long id);
}
