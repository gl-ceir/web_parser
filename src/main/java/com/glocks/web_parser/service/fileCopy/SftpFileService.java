package com.glocks.web_parser.service.fileCopy;


import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.constants.CopyStatus;
import com.glocks.web_parser.dto.SftpDestinationDto;
import com.glocks.web_parser.dto.SftpFileDto;
import com.glocks.web_parser.model.app.ListFileManagement;

import com.glocks.web_parser.repository.app.ListFileManagementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static com.glocks.web_parser.constants.Constants.applicationName;

@Service
public class SftpFileService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());




//    @Autowired
//    ListFileManagementService listFileManagementService;

    @Autowired
    ListFileManagementRepository listFileManagementRepository;

    @Autowired
    AppConfig appConfig;



    private String serverName = null;


    private CopyStatus callUrl(SftpFileDto sftpFileDto, String operator) {
        RestTemplate restTemplate = new RestTemplate();
        CopyStatus copyStatus = CopyStatus.NEW;
        try {
            String URL = appConfig.getFileCopyUrl();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SftpFileDto> request = new HttpEntity<SftpFileDto>(sftpFileDto, headers);
            log.info("Calling URL for Sftp File Request:{}, Url:{}", sftpFileDto, URL);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(URL, request, String.class);
            log.info("Response URL for Sftp File Request:{}, Response:{}", sftpFileDto, responseEntity);
            if(responseEntity.getStatusCode() == HttpStatusCode.valueOf(200)) {
                copyStatus = CopyStatus.COPIED;
            }
        } catch (Exception e) {
            log.error("Error while URL for Sftp File Error:{} Request:{}", e.getMessage(), sftpFileDto, e);
        }
        return copyStatus;
    }

    public void sendCopyFileInfo(ListFileManagement listFileManagement) {
        log.info("Going to call SFTP URL for record:{}", listFileManagement);
        SftpDestinationDto destinationDto = new SftpDestinationDto();
        destinationDto.setDestServerName(listFileManagement.getDestinationServer());
        destinationDto.setDestFilePath(listFileManagement.getDestinationPath());
        SftpFileDto sftpFileDto = SftpFileDto.builder()
                .txnId(String.valueOf(listFileManagement.getId()))
                .sourceFileName(listFileManagement.getFileName())
                .applicationName(applicationName)
                .destination(Collections.singletonList(destinationDto))
                .sourceFilePath(listFileManagement.getFilePath())
                .serverName(serverName)
                .fileType("web parser processed file")
                .remarks("")
                .sourceServerName(listFileManagement.getSourceServer()).build();
        CopyStatus copyStatus = callUrl(sftpFileDto, "");
        if(copyStatus == CopyStatus.NEW) {
        save(listFileManagement);
        }
    }

    public ListFileManagement save(ListFileManagement listFileManagement) {
        log.info("Going to Save listFileManagement {}", listFileManagement);
        ListFileManagement savedListFileManagement = listFileManagementRepository.save(listFileManagement);
        log.info("Saved listFileManagement {}", savedListFileManagement);
        return savedListFileManagement;
    }
}