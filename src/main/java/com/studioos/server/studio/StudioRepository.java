package com.studioos.server.studio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudioRepository extends JpaRepository<Studio, String> {

    List<Studio> findByOwnerId(Integer ownerId);

    Page<Studio> findAll(Pageable pageable);
}