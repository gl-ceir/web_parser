package com.glocks.web_parser.repository.app;
import com.glocks.web_parser.model.app.WebActionDb;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebActionDbRepository extends JpaRepository<WebActionDb, Long>{


    @Transactional
    @Query("select u from WebActionDb u where u.state not in (4,5) and u.feature in :feature order by u.state, u.id asc")
    List<WebActionDb> getListOfPendingTasks(@Param("feature")List<String> feature);

    @Transactional
    @Modifying
    @Query("update WebActionDb u set u.state=:state where u.id=:id")
    void updateWebActionStatus(@Param("state") int state, @Param("id") long id);



}
