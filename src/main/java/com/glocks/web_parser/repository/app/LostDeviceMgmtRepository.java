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
    Optional<LostDeviceMgmt> findByRequestId(String requestId);

    Optional<LostDeviceMgmt> findByLostId(String lostId);

    @Modifying
    @Query("UPDATE LostDeviceMgmt x SET x.status =:status WHERE x.requestId =:requestId")
    public int updateStatus(String status, String requestId);

    @Modifying
    @Query("UPDATE LostDeviceMgmt x SET x.status =:status, x.userStatus =:userStatus  WHERE x.requestId =:requestId")
    public int updateUserStatus(String status, String userStatus,String requestId);

    @Query("SELECT COUNT(e) FROM LostDeviceMgmt e WHERE " +
            "(e.imei1 = :imei OR e.imei2 = :imei OR e.imei3 = :imei OR e.imei4 = :imei) " +
            "AND e.status IN (:statuses)")
    long existsByImeiAndStatusIn(String imei, List<String> statuses);
}
