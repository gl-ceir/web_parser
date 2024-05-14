package com.glocks.web_parser.service.fileCopy;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.constants.CopyStatus;
import com.glocks.web_parser.constants.FileType;
import com.glocks.web_parser.constants.ListType;
import com.glocks.web_parser.model.app.ListFileManagement;
import com.glocks.web_parser.repository.app.ListFileManagementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListFileManagementService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ListFileManagementRepository listFileManagementRepository;

    @Autowired
    AppConfig appConfig;

    @Autowired
    SftpFileService sftpFileService;

    public ListFileManagement save(ListFileManagement listFileManagement) {
        log.info("Going to Save listFileManagement {}", listFileManagement);
        ListFileManagement savedListFileManagement = listFileManagementRepository.save(listFileManagement);
        log.info("Saved listFileManagement {}", savedListFileManagement);
        return savedListFileManagement;
    }

    public void saveListManagementEntity(String transactionId, ListType listType, FileType fileType,
                                                              String sourceFilePath, String sourceFileName,
                                                              Long totalCount) {
        String sourceServerName = appConfig.getSourceServerName();
        List<String> copyDestinationServerName = appConfig.getCopyDestinationServerName();
        List<String> copyDestinationServerPath = appConfig.getCopyDestinationServerPath();
        Integer noOfDestination = copyDestinationServerName.size();
        for (int i = 0; i < noOfDestination; i++) {
            String destServerName = copyDestinationServerName.get(i);
            String destFilePath = copyDestinationServerPath.get(i);
            ListFileManagement listFileManagement = new ListFileManagement();
            listFileManagement.setFileType(fileType.getIndex());
            listFileManagement.setFileState(1);
            listFileManagement.setFileName(sourceFileName);
            listFileManagement.setFilePath(sourceFilePath);
            listFileManagement.setListType(listType);
            listFileManagement.setCopyStatus(CopyStatus.NEW.getIndex());
            listFileManagement.setCreatedOn(LocalDateTime.now());
            listFileManagement.setDestinationPath(destFilePath + "/" + transactionId);
            listFileManagement.setModifiedOn(LocalDateTime.now());
            listFileManagement.setDestinationServer(destServerName);
            listFileManagement.setOperatorName("ALL");
            listFileManagement.setRecordCount(totalCount);
            listFileManagement.setSourceServer(sourceServerName);
            sftpFileService.sendCopyFileInfo(listFileManagement);
        }
        return ;
    }
//
//    public List<ListFileManagement> saveListManagement(ListType listType, FileType fileType,
//                                                       String sourceFilePath, String sourceFileName, Long totalCount) {
//        List<ListFileManagement> list = new ArrayList<>();
//        list.addAll(saveListManagementEntity(String transactionId, String transactionId, listType, fileType, sourceFilePath, sourceFileName, totalCount));
//        return list;
//    }


//    public List<ListFileManagement> getAllNotCopiedFiles() {
//        log.info("Getting all records which are not copied");
//        return listFileManagementRepository.findByCopyStatus(0);
//    }

}
