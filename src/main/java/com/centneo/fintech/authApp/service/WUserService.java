package com.centneo.fintech.authApp.service;

import java.util.*;
import java.util.stream.Collectors;

import com.centneo.fintech.authApp.dto.ResponseDto;
import com.centneo.fintech.authApp.dto.UserPreferenceDto;
import com.centneo.fintech.authApp.dto.UserRegisterRequestDTO;
import com.centneo.fintech.authApp.dto.WUserDto;
import com.centneo.fintech.authApp.dto.requests.UserMetaDataDto;
import com.centneo.fintech.authApp.model.auth.Role;
import com.centneo.fintech.authApp.model.auth.User;
import com.centneo.fintech.authApp.model.auth.UserPreference;
import com.centneo.fintech.authApp.model.auth.UserRole;
import com.centneo.fintech.authApp.repository.read.repository.IUserRepositoryReadOnly;
import com.centneo.fintech.authApp.repository.read.repository.IUserRoleRepositoryReadOnly;
import com.centneo.fintech.authApp.repository.read.repository.UserPreferencesReadOnly;
import com.centneo.fintech.authApp.repository.write.repository.IUserRepository;
import com.centneo.fintech.authApp.repository.write.repository.IUserRoleRepository;
import com.centneo.fintech.authApp.repository.write.repository.UserPreferencesRepository;
import com.centneo.fintech.authApp.security.SecurityPrincipal;
import io.jsonwebtoken.lang.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class WUserService implements UserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(WUserService.class);

    @Autowired
    @Qualifier("IUserRepositoryReadOnly")
    private IUserRepositoryReadOnly userRepositoryReadOnly;

    @Autowired
    @Qualifier("IUserRepository")
    private IUserRepository userRepository;

    @Autowired
    private IUserRoleRepository userRoleRepository;

    @Autowired
    private IUserRoleRepositoryReadOnly userRoleRepositoryReadOnly;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    @Autowired
    private UserPreferencesReadOnly userPreferencesReadOnly;

    @Autowired
    WRoleService roleService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepositoryReadOnly.findByUsername(username);
        if (user != null) {
            List<UserRole> userRoles = userRoleRepositoryReadOnly.findAllByUserId(user.getId());

            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            userRoles.forEach(userRole -> {
                authorities.add(new SimpleGrantedAuthority(userRole.getRole().getName()));
            });

            // Dummy password since LDAP will authenticate
            String dummyPassword = "LDAP_AUTH";

            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    dummyPassword, // Never null or empty
                    authorities
            );
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }


    public User findByUsername(String username) {
        return userRepositoryReadOnly.findByUsername(username);
    }

    public User createUser(UserRegisterRequestDTO request) {

        User user = new User();
        try {
            user = (User) dtoMapperRequestDtoToUser(request);
            user.setMfaRegistered(false);
            user.setMfaSecret(null);

            user = userRepository.save(user);
            if (!request.getRoleList().isEmpty()) {
                for (String role : request.getRoleList()) {
                    Role existingRole = roleService.findRoleByName("ROLE_" + role.toUpperCase());
                    if(existingRole != null) {
                        addUserRole(user, existingRole);
                    }
                }
            } else {
                addUserRole(user, null);
            }
            return user;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    public List<User> retrieveAllUserList() {
        return userRepositoryReadOnly.findAll();
    }

    public User updateUser(UserRegisterRequestDTO userRequestDTO) {

        User user = (User) dtoMapperRequestDtoToUser(userRequestDTO);

        user = userRepository.save(user);
        addUserRole(user, null);

        return user;
    }

    public User findCurrentUser() {
        return userRepositoryReadOnly.findById(SecurityPrincipal.getInstance().getLoggedInPrincipal().getId()).get();

    }

    public List<UserRole> findAllCurrentUserRole() {
        return userRoleRepositoryReadOnly.findAllByUserId(SecurityPrincipal.getInstance().getLoggedInPrincipal().getId());

    }

    public Optional<User> findUserById(long id) {
        return userRepositoryReadOnly.findById(id);
    }

    public void addUserRole(User user, Role role) {

        UserRole userRole = new UserRole();
        userRole.setUser(user);

        if (role == null) {
            role = roleService.findDefaultRole();
        }

        userRole.setRole(role);
        userRoleRepository.save(userRole);
    }

    private Object dtoMapperRequestDtoToUser(UserRegisterRequestDTO source) {
        User target = new User();
        target.setEntityNo(source.getEntityNo());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setBranchCode(source.getBranchCode());
        return target;
    }

    public User setPreferences(String username, UserPreferenceDto userPreferenceDto) {

        var user = new User();
        UserPreference userPreference = new UserPreference();
        try {
            user = userRepositoryReadOnly.findByUsername(username);
            user.setEmail(userPreferenceDto.getEmailId());
            user.setSupportLevel(userPreferenceDto.getSupportLevel());
            user.setBranchCode(userPreferenceDto.getBranchCode());
            userRepository.save(user);

            userPreference.setUserId(user.getId());
            userPreference.setPreferences("NA");
            userPreference.setUserJourney(userPreferenceDto.getJourney());
            userPreference.setBranchCode(userPreferenceDto.getBranchCode());
            userPreferencesRepository.save(userPreference);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    public ResponseEntity<ResponseDto<List<Long>>> getUserJourneyId(String username) {

        try {
            // Fetch user by username
            User user = userRepositoryReadOnly.findByUsername(username);
            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ResponseDto.<List<Long>>builder()
                                .success(false)
                                .message("User not found")
                                .data(null)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .build());
            }

            // Fetch user preferences
            UserPreference prefs = userPreferencesReadOnly.findByUserId(user.getId());
            if (prefs == null || prefs.getUserJourney() == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ResponseDto.<List<Long>>builder()
                                .success(false)
                                .message("User journey not found")
                                .data(null)
                                .statusCode(HttpStatus.NOT_FOUND.value())
                                .build());
            }

            // Convert journey string to List<Long>
            List<Long> journeyIds = Arrays.stream(prefs.getUserJourney().split(","))
                    .map(String::trim)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // Return the list of journey IDs
            return ResponseEntity.ok(
                    ResponseDto.<List<Long>>builder()
                            .success(true)
                            .message("Success")
                            .data(journeyIds)
                            .statusCode(HttpStatus.OK.value())
                            .build()
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.<List<Long>>builder()
                            .success(false)
                            .message("Error fetching user journey: " + e.getMessage())
                            .data(null)
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .build());
        }
    }

    public ResponseEntity<ResponseDto> getAllActiveSupportUsers() {

        List<User> userList = userRepositoryReadOnly.findAll();
        List<UserMetaDataDto> userMetaDataDtos = userList.stream().map(user -> {
            return new UserMetaDataDto(
                    user.getId(),user.getUsername(), user.getSupportLevel()
            );
        }).collect(Collectors.toUnmodifiableList());
        ResponseDto responseDto = ResponseDto.builder().success(true).statusCode(200).data(userMetaDataDtos)
                .message(userList.size() + " active users currently registered in auth svc").build();
        return ResponseEntity.ok(responseDto);
    }
}
