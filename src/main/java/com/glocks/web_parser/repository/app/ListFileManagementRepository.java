package com.glocks.web_parser.repository.app;


import com.glocks.web_parser.model.app.ListFileManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ListFileManagementRepository extends JpaRepository<ListFileManagement, Long> {

    List<ListFileManagement> findByCopyStatus(Integer copyStatus);

}
