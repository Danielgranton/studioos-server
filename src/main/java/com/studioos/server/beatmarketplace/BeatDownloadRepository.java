package com.studioos.server.beatmarketplace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatDownloadRepository extends JpaRepository<BeatDownload, String> {
    List<BeatDownload> findByPurchaseId(String purchaseId);
    List<BeatDownload> findByPurchaseIdIn(List<String> purchaseIds);
}