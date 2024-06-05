package com.glocks.web_parser.service.fileCopy;


import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.constants.CopyStatus;
import com.glocks.web_parser.constants.FileType;
import com.glocks.web_parser.constants.ListType;
import com.glocks.web_parser.dto.SftpDestinationDto;
import com.glocks.web_parser.dto.SftpFileDto;
import com.glocks.web_parser.model.app.ListFileManagement;

import com.glocks.web_parser.repository.app.ListFileManagementRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.glocks.web_parser.constants.Constants.applicationName;

@Service
public class SftpFileService {

    private final Logger log = LogManager.getLogger(this.getClass());




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
            log.info("Calling URL for file copy File Request:{}, Url:{}", sftpFileDto, URL);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(URL, request, String.class);
            log.info("Response URL for file copy File Request:{}, Response:{}", sftpFileDto, responseEntity);
            if(responseEntity.getStatusCode() == HttpStatusCode.valueOf(200)) {
                copyStatus = CopyStatus.COPIED;
            }
        } catch (Exception e) {
            log.error("Error while URL for file copy File Error:{} Request:{}", e.getMessage(), sftpFileDto, e);
        }
        return copyStatus;
    }

    public void sendCopyFileInfo(ListFileManagement listFileManagement) {
        log.info("Going to call file copy URL for record:{}", listFileManagement);
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
    public boolean sendCopyFileInfo(String transactionId, ListType listType, FileType fileType,
                                 String sourceFilePath, String sourceFileName,
                                 Long totalCount, List<String> destinationServers, List<String> destinationFilePath) {
        log.info("Going to call file copy URL for file:{}", sourceFileName);
        List<SftpDestinationDto> destinationDtos = new ArrayList<>();
        for(int i=0;i<destinationServers.size();i++) {
            SftpDestinationDto destinationDto = new SftpDestinationDto();
            destinationDto.setDestServerName(destinationServers.get(i));
            destinationDto.setDestFilePath(destinationFilePath.get(i) + "/" + transactionId);
            destinationDtos.add(destinationDto);
        }
        SftpFileDto sftpFileDto = SftpFileDto.builder()
                .txnId(transactionId)
                .sourceFileName(sourceFileName)
                .applicationName(applicationName)
                .destination(destinationDtos)
                .sourceFilePath(sourceFilePath)
                .serverName(serverName)
                .fileType("web parser processed file")
                .remarks("")
                .sourceServerName(appConfig.getSourceServerName()).build();
        CopyStatus copyStatus = callUrl(sftpFileDto, "");
        if(copyStatus == CopyStatus.NEW) {
            return false;
        }
        return true;
    }


    public ListFileManagement save(ListFileManagement listFileManagement) {
        log.info("Going to Save listFileManagement {}", listFileManagement);
        ListFileManagement savedListFileManagement = listFileManagementRepository.save(listFileManagement);
        log.info("Saved listFileManagement {}", savedListFileManagement);
        return savedListFileManagement;
    }
}
