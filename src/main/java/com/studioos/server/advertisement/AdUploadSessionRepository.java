package com.studioos.server.advertisement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdUploadSessionRepository extends JpaRepository<AdUploadSession, String> {
    List<AdUploadSession> findByAdvertisementId(String advertisementId);
}