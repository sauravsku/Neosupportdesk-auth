package com.centneo.fintech.authApp.repository.read.repository;

import com.centneo.fintech.authApp.model.auth.User;
import com.centneo.fintech.authApp.repository.write.repository.IUserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRepositoryReadOnly extends IUserRepository {

    //User findUserByUsernameAndPassword(String username, String password);

    User findBySsoId(String ssoId);

    Optional<User> findBySsoIdAndSupportLevel(String searchUser, String supportLevel);

    List<User> findAllBySupportLevel(String supportLevel);
}
