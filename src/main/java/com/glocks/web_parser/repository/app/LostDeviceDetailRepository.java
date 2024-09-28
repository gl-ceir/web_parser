package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.LostDeviceDetail;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface LostDeviceDetailRepository extends JpaRepository<LostDeviceDetail, Long> {
    Boolean existsByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(String imei, String status, List<String> requestTypes);

    @Query("SELECT x.requestId FROM LostDeviceDetail x WHERE x.imei =:imei")
    Optional<String> findLostDeviceDetailByImei(String imei);

    Optional<LostDeviceDetail> findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(String imei, String status, List<String> requestType);

    int deleteByImeiAndStatusIn(String imei, List<String> status);

    Boolean existsByImei(String imei);
}
