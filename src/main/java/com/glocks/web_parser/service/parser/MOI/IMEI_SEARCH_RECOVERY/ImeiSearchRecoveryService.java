package com.glocks.web_parser.service.parser.MOI.IMEI_SEARCH_RECOVERY;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.LostDeviceDetail;
import com.glocks.web_parser.model.app.MobileDeviceRepository;
import com.glocks.web_parser.model.app.SearchImeiByPoliceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.validator.Validation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ImeiSearchRecoveryService {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final LostDeviceDetailRepository lostDeviceDetailRepository;
    private final SearchImeiByPoliceMgmtRepository searchImeiByPoliceMgmtRepository;
    private final LostDeviceMgmtRepository lostDeviceMgmtRepository;
    private final MDRRepository mdrRepository;
    private final SearchImeiDetailByPoliceRepository searchImeiDetailByPoliceRepository;
    private final WebActionDbRepository webActionDbRepository;
    private final Validation validation;
    private final AppConfig appConfig;

    public Optional<SearchImeiByPoliceMgmt> findByTxnId(String txnId) {
        Optional<SearchImeiByPoliceMgmt> response = searchImeiByPoliceMgmtRepository.findByTransactionId(txnId);
        logger.info("SearchImeiByPoliceMgmt response : {} based on txn ID :{}", txnId, response);
        return response;
    }

    public boolean isBrandAndModelValid(List<String> list) {
        Optional<List<MobileDeviceRepository>> byDeviceIdIn = mdrRepository.findByDeviceIdIn(list);
        logger.info(" MDR record {}", byDeviceIdIn);
        if (byDeviceIdIn.isPresent()) {
            List<String> collect = byDeviceIdIn.get().stream().map(x -> x.getBrandName() + "#" + x.getModelName()).collect(Collectors.toList());
            logger.info("validate brand_name and model_name {}", collect);
            return collect.stream().distinct().count() == 1;
        }
        return false;
    }

    public Optional<String> imeiListOfLostDevice(List<String> imeiList) {
        return imeiList.stream().filter(x -> {
            Boolean isExist = lostDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(x, "DONE", List.of("LOST", "STOLEN"));
            logger.info("is {} exist in lost_device_detail {}", x, isExist);
            return isExist;
        }).findFirst();
    }

    public Optional<LostDeviceDetail> findByImeiAndStatusAndRequestType(String imei, String transactionId) {
        Optional<LostDeviceDetail> result = lostDeviceDetailRepository.findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(imei, "DONE", List.of("LOST", "STOLEN"));
        if (result.isEmpty()) {
            logger.info("No record found for IMEI {} and txn ID {} in lost_device_detail", imei, transactionId);
            this.updateStatusAndCountFoundInLost("Success", 0, transactionId);
            logger.info("Updated the status as Success, count_found_in_lost as 0 in app.search_imei_by_police_mgmt");
        }
        return result;
    }


    public void updateStatusAndCountFoundInLost(String status, int count, String transactionId) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {} and count {}", status, transactionId, count);
        searchImeiByPoliceMgmtRepository.updateCountFoundInLost(status, count, transactionId);
    }

    public Optional<String> findByImei(String imei) {
        Optional<String> byImei = lostDeviceDetailRepository.findByImei(imei);
        if (byImei.isEmpty()) {
            logger.info("No record found for IMEI {} in lost_device_detail", imei);
        }
        return byImei;
    }

    public int copyLostDeviceMgmtToSearchIMEIDetailByPolice(String requestId) {
        return searchImeiDetailByPoliceRepository.copyLostDeviceMgmtToSearchIMEIDetailByPolice(requestId);
    }

    public void updateStatus(String status, String txnId) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {}", status, txnId);
        searchImeiByPoliceMgmtRepository.updateStatus(status, txnId);
    }

    public void requestIdDetails(LostDeviceDetail lostDeviceDetail, String transactionId, WebActionDb webActionDb) {
        String imei = lostDeviceDetail.getImei();
        Optional<String> optional = findByImei(imei);
        if (optional.isPresent()) {
            String requestId = optional.get();
            logger.info("found requestId {} for IMEI {}", requestId, imei);
            int i = copyLostDeviceMgmtToSearchIMEIDetailByPolice(requestId);
            if (i > 0) {
                updateStatusAndCountFoundInLost("Success", 1, transactionId);
                logger.info("updated record with status as success and count_found_in _lost as 1 for Txn ID {}", transactionId);
                updateStatus("Done", transactionId);
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
            }
        }
    }

    public void copyRecordLostDeviceMgmtToSearchIMEIDetailByPolice(LostDeviceDetail lostDeviceDetail, String transactionId, WebActionDb webActionDb) {
        this.requestIdDetails(lostDeviceDetail, transactionId, webActionDb);
    }


    public boolean isMultipleIMEIExist(SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        long count = Stream.of(searchImeiByPoliceMgmt).flatMap(x -> Stream.of(x.getImei2(), x.getImei3(), x.getImei4())).filter(String::isEmpty).count();
        logger.info("No. of IMEI's found empty {}", count);
        return count == 3 ? false : true;
    }

    public List<String> imeiList(SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        List<String> collect = Stream.of(searchImeiByPoliceMgmt).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::nonNull).collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", collect);
        return collect;
    }

    public List<String> tacList(SearchImeiByPoliceMgmt searchImeiByPoliceMgmt) {
        String[] arr = {searchImeiByPoliceMgmt.getImei1(), searchImeiByPoliceMgmt.getImei2(), searchImeiByPoliceMgmt.getImei3(), searchImeiByPoliceMgmt.getImei4()};
        List<String> tacList = Stream.of(arr).filter(Objects::nonNull).filter(x -> x.length() > 8).map(imei -> imei.substring(0, 8)).collect(Collectors.toList());
        logger.info("TAC list : {}", tacList);
        if (tacList.isEmpty()) {
            logger.info("Invalid TAC list");
            return Collections.EMPTY_LIST;
        }
        return tacList;
    }


    public boolean isNumericAndValid(String record) {
        if (validation.isNumeric(record)) {
            if (!isIMEILengthAllowed(record)) {
                logger.info("The record {} is not in correct format {}", record);
                return false;
            }
            return true;
        }
        return false;
    }

    public boolean isIMEILengthAllowed(String record) {
        int length = record.length();
        return length == 14 || length == 15 || length == 16;
    }

    public PrintWriter file(String currFilePath) {
        try {
            File outFile = new File(currFilePath);
            PrintWriter writer = new PrintWriter(outFile);
            /*writer.println(" Invalid Format");
            writer.close();*/
            return writer;
        } catch (Exception e) {
            logger.info("PrintWriterException : {}", e.getMessage());
        }
        return null;
    }

    public boolean fileValidation(String transactionId, String filePath, PrintWriter printWriter) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String record;
            String[] split;
            List<IMEISeriesModel> list = new ArrayList<>();
            boolean headerSkipped = false;
            boolean numericAndValid = true;
            while ((record = reader.readLine()) != null) {
                if (!record.trim().isEmpty()) {
                    if (!headerSkipped) {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);
                        String[] headers = {"IMEI1", "IMEI2", "IMEI3", "IMEI4"};
                        boolean headersMatch = Arrays.equals(Arrays.stream(split).map(String::trim).toArray(String[]::new), Arrays.stream(headers).toArray(String[]::new));
                        if (!headersMatch) {
                            logger.info("Incorrect file header for transactionId {}", transactionId);
                            printWriter.println(record + ",Invalid Format");
                            printWriter.close();
                            break;
                        }
                        headerSkipped = true;
                    } else {
                        split = record.split(appConfig.getListMgmtFileSeparator(), -1);

                        if (split.length > 0) {
                            IMEISeriesModel imeiSeriesModel = null;
                            for (int i = 0; i < split.length; i++) {
                                numericAndValid = isNumericAndValid(split[i]);
                                if (!numericAndValid) {
                                    printWriter.println(",Invalid Format");
                                    printWriter.close();
                                    return false;
                                } else {
                                    imeiSeriesModel = new IMEISeriesModel(split);
                                }
                            }
                            list.add(imeiSeriesModel);
                            logger.info("list {}", list);
                            boolean isValidFile = list.stream().anyMatch(IMEISeriesModel::areAllFieldsEmpty);
                            if (isValidFile) {
                                logger.info("Invalid file found for transactionId {}", transactionId);
                                printWriter.println(",Invalid Format");
                                printWriter.close();
                                return false;
                            }
                        }
                    }
                }
            }
            if (list.isEmpty()) {
                logger.info("file is empty for transactionId {}", transactionId);
                return false;
            }

        } catch (Exception ex) {
            logger.error("Exception in processing the file {}", ex.getMessage());
        }
        return false;
    }
}
