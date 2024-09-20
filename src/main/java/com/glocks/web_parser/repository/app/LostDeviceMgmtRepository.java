package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.LostDeviceMgmt;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface LostDeviceMgmtRepository extends JpaRepository<LostDeviceMgmt, Long> {
    // List<LostDeviceMgmt> findByRequestId(String RequestId);
}
