package com.studioos.server.user;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailOrPhone(String email, String phone);
    List<User> findByRole(Role role);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
