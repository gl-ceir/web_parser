package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface LostDeviceMgmtRepository extends JpaRepository<LostDeviceMgmt, Long> {
    Optional<LostDeviceMgmt> findByRequestId(String lostID);

    Optional<LostDeviceMgmt> findByLostId(String lostID);

    @Modifying
    @Query("UPDATE LostDeviceMgmt x SET x.status =:status WHERE x.requestId =:requestId")
    public int updateStatus(String status, String requestId);

    Boolean existsByImei1AndStatusIn(String imeiValue, List<String> status);

    Boolean existsByImei2AndStatusIn(String imeiValue, List<String> status);

    Boolean existsByImei3AndStatusIn(String imeiValue, List<String> status);

    Boolean existsByImei4AndStatusIn(String imeiValue, List<String> status);
}
