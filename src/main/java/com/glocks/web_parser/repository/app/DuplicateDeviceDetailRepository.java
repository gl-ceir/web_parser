package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.DuplicateDeviceDetail;
import com.glocks.web_parser.model.app.EirsInvalidImei;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface DuplicateDeviceDetailRepository extends JpaRepository<DuplicateDeviceDetail, Long> {
    Boolean existsByImeiAndMsisdnNull(String imei);

   Boolean existsByImeiAndStatusIgnoreCaseEquals(String imei, String status);
 //   Optional<List<DuplicateDeviceDetail>> findByImeiAndStatusIgnoreCaseEquals(String imei, String status);
}
