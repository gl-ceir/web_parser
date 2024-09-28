package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Optional;

@Repository
@Transactional(rollbackOn = {SQLException.class})
public interface SearchImeiByPoliceMgmtRepository extends JpaRepository<SearchImeiByPoliceMgmt, Long>, JpaSpecificationExecutor<SearchImeiByPoliceMgmt> {
    @Modifying
    @Query("UPDATE SearchImeiByPoliceMgmt x SET x.status =:status, x.countFoundInLost =:count, x.failReason =:failReason WHERE x.transactionId =:transactionId")
    public void updateCountFoundInLost(String status, int count, String transactionId, String failReason);

    Optional<SearchImeiByPoliceMgmt> findByTransactionId(String txnId);

    @Modifying
    @Query("UPDATE SearchImeiByPoliceMgmt x SET x.status =:status, x.failReason =:failReason WHERE x.transactionId =:transactionId")
    public int updateStatus(String status, String transactionId, String failReason);

}
