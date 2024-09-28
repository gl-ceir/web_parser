package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.LostDeviceDetailHis;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface LostDeviceDetailHisRepository extends JpaRepository<LostDeviceDetailHis, Long> {

}
