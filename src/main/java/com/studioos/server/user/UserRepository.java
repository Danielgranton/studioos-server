package com.studioos.server.user;

import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.enums.VerificationStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmailOrPhone(String email, String phone);
    List<User> findByRole(Role role);
    long countByRoleAndStatus(Role role, AccountStatus status);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.status = :status AND u.accountVerified = true AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
    List<User> findFeaturedCreators(Collection<Role> roles, AccountStatus status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role IN :roles AND u.status = :status AND u.accountVerified = true AND u.deletedAt IS NULL")
    long countFeaturedCreators(Collection<Role> roles, AccountStatus status);

    @Query("SELECT u FROM User u WHERE u.role = com.studioos.server.shared.enums.Role.ARTIST AND u.status = :status AND u.accountVerified = true AND u.deletedAt IS NULL ORDER BY u.updatedAt DESC")
    Page<User> findPublicArtists(AccountStatus status, Pageable pageable);

    List<User> findByVerificationStatus(VerificationStatus verificationStatus);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    List<User> findByStatusAndCreatedAtBefore(AccountStatus status, LocalDateTime cutoff);
}
