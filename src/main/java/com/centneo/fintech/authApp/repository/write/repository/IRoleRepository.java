package com.centneo.fintech.authApp.repository.write.repository;

import com.centneo.fintech.authApp.model.auth.Role;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
@Primary
public interface IRoleRepository extends JpaRepository<Role, Long>{
}
