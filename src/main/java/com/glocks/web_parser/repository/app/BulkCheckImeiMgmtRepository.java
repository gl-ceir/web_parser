package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.BulkCheckImeiMgmt;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BulkCheckImeiMgmtRepository extends JpaRepository<BulkCheckImeiMgmt, Integer> {

    BulkCheckImeiMgmt findByTransactionId (String transactionId);

//    @Transactional
//    @Modifying
//    @Query("update BulkCheckImeiMgmt u set u.status=:status, u.modifiedOn=:modifiedOn, u.remarks= :remarks where u.id=:id")
//    void updateBulkCheckImeiMgmtStatus(@Param("status") String status, @Param("modifiedOn") LocalDateTime modifiedOn,
//                                  @Param("remarks") String remarks, @Param("id") long id);

    @Transactional
    @Modifying
    @Query("update BulkCheckImeiMgmt u set u.status=:status, u.modifiedOn=:modifiedOn where u.id=:id")
    void updateBulkCheckImeiMgmtStatus(@Param("status") String status, @Param("modifiedOn") LocalDateTime modifiedOn,
                                  @Param("id") long id);

}
