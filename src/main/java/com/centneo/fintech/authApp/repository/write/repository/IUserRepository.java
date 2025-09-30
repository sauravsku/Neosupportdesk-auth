package com.centneo.fintech.authApp.repository.write.repository;

import com.centneo.fintech.authApp.model.auth.User;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public interface IUserRepository extends JpaRepository<User, Long> {
}
