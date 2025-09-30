package com.centneo.fintech.authApp.repository.write.repository;

import java.util.List;

import com.centneo.fintech.authApp.model.auth.UserRole;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public interface IUserRoleRepository extends JpaRepository<UserRole, Long>{
}