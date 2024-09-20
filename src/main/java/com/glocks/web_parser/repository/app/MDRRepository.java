package com.glocks.web_parser.repository.app;

import com.glocks.web_parser.model.app.MobileDeviceRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface MDRRepository extends JpaRepository<MobileDeviceRepository, Integer> {
    Optional<List<MobileDeviceRepository>> findByDeviceIdIn(List<String> deviceIds);
}
