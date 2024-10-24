package com.glocks.web_parser.service.parser.moi.utility;

import com.glocks.web_parser.config.AppConfig;
import com.glocks.web_parser.model.app.*;
import com.glocks.web_parser.repository.app.*;
import com.glocks.web_parser.validator.Validation;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MOIService {
    private Logger logger = LogManager.getLogger(this.getClass());
    private final StolenDeviceDetailRepository stolenDeviceDetailRepository;
    private final SearchImeiByPoliceMgmtRepository searchImeiByPoliceMgmtRepository;
    private final StolenDeviceMgmtRepository stolenDeviceMgmtRepository;
    private final MDRRepository mdrRepository;
    private final BlackListRepository blackListRepository;
    private final BlackListHisRepository blackListHisRepository;
    private final GreyListRepository greyListRepository;
    private final GreyListHisRepository greyListHisRepository;
    private final ImeiPairDetailRepository imeiPairDetailRepository;
    private final ImeiPairDetailHisRepository imeiPairDetailHisRepository;
    private final Validation validation;
    private final SysParamRepository sysParamRepository;
    private final AppConfig appConfig;
    private final WebActionDbRepository webActionDbRepository;
    public static Map<String, String> requestIdMap = new HashMap<>();

    public Optional<SearchImeiByPoliceMgmt> findByTxnId(String txnId) {
        Optional<SearchImeiByPoliceMgmt> response = searchImeiByPoliceMgmtRepository.findByTransactionId(txnId);
        logger.info("SearchImeiByPoliceMgmt response : {} based on txn ID :{}", txnId, response);
        return response;
    }

    public boolean isBrandAndModelValid(List<String> list) {
        Map<String, String> map = new HashMap<>();
        for (String tac : list) {
            mdrRepository.findByDeviceId(tac).ifPresent(x -> {
                map.put(tac, x.stream().map(y -> y.getBrandName() + "<>" + y.getModelName()).collect(Collectors.joining()));
            });
        }
        logger.info(" MDR record {}", map);
        if (map.isEmpty()) return false;
        Set<String> uniqueKeys = map.keySet().stream().collect(Collectors.toSet());
        Set<String> uniqueValues = map.values().stream().collect(Collectors.toSet());
        return uniqueKeys.size() == 1 && uniqueValues.size() == 1;
    }

    public Optional<String> imeiListOfLostDevice(List<String> imeiList) {
        return imeiList.stream().filter(x -> {
            Boolean isExist = stolenDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(x, "Done", List.of("LOST", "STOLEN"));
            logger.info("is {} exist in stolen_device_detail {}", x, isExist);
            return isExist;
        }).findFirst();
    }

    public Optional<StolenDeviceDetail> findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(String imei) {
        Optional<StolenDeviceDetail> result = stolenDeviceDetailRepository.findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(imei, "Done", List.of("LOST", "STOLEN"));
        logger.info("findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn response {}", result);
        return result;
    }


    public void updateStatusAndCountFoundInLost(String status, int count, String transactionId, String failReason) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {} and count {} and failReason {}", status, transactionId, count, failReason);
        searchImeiByPoliceMgmtRepository.updateCountFoundInLost(status, count, transactionId, failReason);
    }

    public void updateReasonAndCountInSearchImeiByPoliceMgmt(String status, String failReason, String transactionId, int count) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {} and failReason {}", status, transactionId, failReason);
        searchImeiByPoliceMgmtRepository.updateStatus(status, failReason, transactionId, count);
    }

    public void updateCountFoundInLost(String status, int count, String transactionId, String failReason) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {} and count {}", status, transactionId, count);
        searchImeiByPoliceMgmtRepository.updateCountFoundInLost(status, count, transactionId, null);
    }

    public Optional<String> findByImei(String imei) {
        Optional<String> byImei = stolenDeviceDetailRepository.findStolenDeviceDetailByImei(imei);
        if (byImei.isEmpty()) {
            logger.info("No record found for IMEI {} in stolen_device_detail", imei);
        }
        return byImei;
    }

    public boolean isMultipleIMEIExist(IMEISeriesModel imeiSeriesModel) {
        long count = Stream.of(imeiSeriesModel).flatMap(x -> Stream.of(x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::isNull).count();
        logger.info("No. of IMEI's found empty {}", count);
        return count == 3 ? false : true;
    }

    public List<String> tacList(IMEISeriesModel imeiSeriesModel) {
        String[] arr = {imeiSeriesModel.getImei1(), imeiSeriesModel.getImei2(), imeiSeriesModel.getImei3(), imeiSeriesModel.getImei4()};
        List<String> tacList = Stream.of(arr).filter(Objects::nonNull).filter(x -> x.length() > 8).map(imei -> imei.substring(0, 8)).collect(Collectors.toList());
        logger.info("TAC list : {}", tacList);
        return tacList;
    }

    public String greyListDuration() {
        String greyListDuration = sysParamRepository.getValueFromTag("GREY_LIST_DURATION");
        return greyListDuration;
    }

    public boolean isIMEILengthAllowed(String record) {
        int length = record.length();
        return length == 14 || length == 15 || length == 16;
    }

    public boolean isNumericAndValid(String record) {
        logger.info("going to check IMEI {} length", record);
        if (validation.isNumeric(record)) {
            if (!isIMEILengthAllowed(record)) {
                logger.info("Invalid IMEI {} length", record);
                return false;
            }
        }
        return true;
    }

    public <T> T save(T v1, Function<T, T> saveFunction) {
        logger.info("Going to save {}", v1);
        return saveFunction.apply(v1);
    }

    public void updateSource(String source, String imei, String repo) {
        logger.info("updated {} with source {} for imei {}", repo, source, imei);
        int rowAffected = 0;
        switch (repo) {
            case "BLACK_LIST" -> rowAffected = blackListRepository.updateSource(source, imei);
            case "GREY_LIST" -> rowAffected = greyListRepository.updateSource(source, imei);
        }
        if (rowAffected == 1) {
            logger.info("removed MOI and updated source value for {}", repo);
        } else {
            logger.info("failed to update source value for {}", repo);
        }
    }


    public LocalDateTime expiryDate(int daysToAdd) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        LocalDateTime expiryDate = currentDateTime.plusDays(daysToAdd);
        return expiryDate;
    }

    public DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }

    public boolean isDateFormatValid(String createdOn) {
        try {
            LocalDateTime.parse(createdOn, dateFormatter());
        } catch (DateTimeParseException e) {
            logger.info("Invalid format received for deviceLostDateTime {} ,Expected format: yyyy-MM-dd HH:mm:ss", createdOn);
            return false;
        }
        return true;
    }

    public void imeiPairDetail(String createdOn, String mode) {

        imeiPairDetailRepository.findByCreatedOnGreaterThanEqual(createdOn).ifPresentOrElse(imeiPairDetailList -> {
            logger.info("ImeiPairDetail list {}", imeiPairDetailList);
            imeiPairDetailList.forEach(list -> {
                ImeiPairDetailHis imeiPairDetailHis = new ImeiPairDetailHis();
                BeanUtils.copyProperties(list, imeiPairDetailHis);
                imeiPairDetailHis.setAction("Delete");
                imeiPairDetailHis.setActionRemark("MOI");
                logger.info("ImeiPairDetailHis {}", imeiPairDetailHis);
                ImeiPairDetailHis savedEntity = save(imeiPairDetailHis, imeiPairDetailHisRepository::save);
                if (savedEntity != null) {
                    imeiPairDetailRepository.deleteById(Math.toIntExact(list.getId()));
                }
            });
        }, () -> logger.info("No result found for created_on {}", createdOn));
    }

    public String getTacFromIMEI(String imei) {
        return Stream.of(imei).filter(Objects::nonNull).map(x -> imei.substring(0, 8)).collect(Collectors.joining());
    }

    public String getActualIMEI(String imei) {
        return Stream.of(imei).filter(Objects::nonNull).map(x -> imei.substring(0, 14)).collect(Collectors.joining());
    }


    public void greyListDurationIsZero(String imei, String mode, StolenDeviceMgmt lostDeviceMgmt) {
        findBlackListByImei(imei).ifPresentOrElse(blackList -> {
            String source = remove(blackList.getSource());
            if (Objects.isNull(source)) updateSource("MOI", imei, "BLACK_LIST");
            else {
                if (!source.equalsIgnoreCase("MOI")) updateSource(source + "MOI", imei, "BLACK_LIST");
            }
        }, () -> {
            BlackList blackList = new BlackList();
            blackList.setImei(imei);
            blackList.setModeType(mode);
            blackList.setSource("MOI");
            blackList.setRequestType("Stolen");
            blackList.setRemarks(lostDeviceMgmt.getRemark());
            blackList.setTxnId(lostDeviceMgmt.getRequestId());
            blackList.setTac(getTacFromIMEI(imei));
            blackList.setActualImei(getActualIMEI(imei));
            save(blackList, blackListRepository::save);
            BlackListHis blackListHis = new BlackListHis();
            BeanUtils.copyProperties(blackList, blackListHis);
            blackListHis.setOperation(0);
            save(blackListHis, blackListHisRepository::save);
        });
    }

    public void greyListDurationGreaterThanZero(int greyListDuration, String imei, String mode, StolenDeviceMgmt stolenDeviceMgmt) {
        GreyList greyList = GreyList.builder().imei(imei).msisdn(stolenDeviceMgmt.getContactNumber()).modeType(mode).source("MOI").expiryDate(expiryDate(greyListDuration)).requestType("Stolen").remarks(stolenDeviceMgmt.getRemark()).txnId(stolenDeviceMgmt.getRequestId()).tac(getTacFromIMEI(imei)).actualImei(getActualIMEI(imei)).build();
        save(greyList, greyListRepository::save);
        GreyListHis greyListHis = new GreyListHis();
        BeanUtils.copyProperties(greyList, greyListHis);
        greyListHis.setOperation(0);
        save(greyListHis, greyListHisRepository::save);
    }

    public Optional<StolenDeviceMgmt> findByRequestId(String id) {
        Optional<StolenDeviceMgmt> response = stolenDeviceMgmtRepository.findByRequestId(id);
        logger.info("LostDeviceMgmt response : {} based on txn ID :{}", response, id);
        return response;
    }

    public Optional<BlackList> findBlackListByImei(String imei) {
        Optional<BlackList> result = Optional.ofNullable(blackListRepository.findBlackListByImei(imei));
        if (result.isEmpty()) {
            logger.info("No record found for IMEI {} in black_list", imei);
        }
        return result;
    }

    public Optional<GreyList> findGreyListByImei(String imei) {
        Optional<GreyList> result = Optional.ofNullable(greyListRepository.findGreyListByImei(imei));
        if (result.isEmpty()) {
            logger.info("No record found for IMEI {} in grey_list", imei);
        }
        return result;
    }

    public void updateStatusInLostDeviceMgmt(String status, String requestId) {
        logger.info("updated stolen_device_mgmt with status {} and requestId {}", status, requestId);
        stolenDeviceMgmtRepository.updateStatus(status, requestId);
    }

    public Function<IMEISeriesModel, List<String>> imeiSeries = (imeiSeries) -> {
        List<String> collect = Stream.of(imeiSeries).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(imei -> imei != null && !imei.isEmpty()).collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", collect);
        return collect;
    };

    public Predicate<String> isIMEILengthAllowed = record -> {
        int length = record.length();
        return length == 14 || length == 15 || length == 16;
    };

    public Predicate<String> isNumericAndValid = record -> {
        if (new Validation().isNumeric(record)) {
            return isIMEILengthAllowed.test(record);
        }
        logger.info("Invalid IMEI length {}", record);
        return false;
    };

    public PrintWriter file(String currFilePath) {
        try {
            File outFile = new File(currFilePath);
            PrintWriter writer = new PrintWriter(outFile);
            return writer;
        } catch (Exception e) {
            logger.info("PrintWriterException : {}", e.getMessage());
        }
        return null;
    }

    public String joiner(String[] split, String status) {
        return Arrays.stream(split).collect(Collectors.joining(",")) + status;
    }

    public void invalidFile(String header, PrintWriter printWriter) {
        printWriter.println(header + ",Invalid Format");
        printWriter.close();
    }

    public Function<String, Long> sourceCount = (source) -> {
        if (Objects.nonNull(source)) {
            String[] split = source.split(",");
            boolean isMoiExistInSource = Arrays.stream(split).anyMatch(x -> x.equals("MOI"));
            logger.info("isMoiExistInSource {}", isMoiExistInSource);
            if (isMoiExistInSource) {
                long count = Arrays.stream(split).count();
                if (count > 1) {
                    logger.info("Multiple values found including MOI");
                    return 2L;
                }
                if (count == 1) {
                    logger.info("source value matched with MOI", source);
                    return 1L;
                }
            }
        }
        logger.info("No source value {} matched with MOI", source);
        return 0L;
    };

    public String remove(String source) {
        if (Objects.nonNull(source)) {
            return Arrays.stream(source.split(",")).filter(element -> !element.equals("MOI")).collect(Collectors.joining(","));
        } else logger.info("source value is null");
        return null;
    }


    public boolean areHeadersValid(String filePath, String feature, int length) {
        boolean isHeadersNameValid = false;
        Map<String, String> map = new HashMap<>();
        File file = new File(filePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headers = reader.readLine();
            String[] header = headers.split(appConfig.getListMgmtFileSeparator(), -1);
            if (header.length != length) {
                logger.info("Invalid header size");
                return false;
            }

            switch (feature) {
                case "STOLEN" -> {
                    map.put("0", "Phone Number");
                    map.put("1", "IMEI1");
                    map.put("2", "IMEI2");
                    map.put("3", "IMEI3");
                    map.put("4", "IMEI4");
                    map.put("5", "Device Type");
                    map.put("6", "Device Brand");
                    map.put("7", "Device Model");
                    map.put("8", "Serial number");
                    isHeadersNameValid = IntStream.range(0, length).allMatch(i -> map.get(String.valueOf(i)).equalsIgnoreCase(header[i].trim()));
                    logger.info("isHeadersNameValid {}", isHeadersNameValid);
                    if (!isHeadersNameValid) {
                        logger.info("The header of the file is not correct");
                        reader.close();
                    }

                }
                case "RECOVER" -> {
                    map.put("0", "IMEI");
                    isHeadersNameValid = IntStream.range(0, length).allMatch(i -> map.get(String.valueOf(i)).equalsIgnoreCase(header[i].trim()));
                    logger.info("isHeadersNameValid {}", isHeadersNameValid);
                    if (!isHeadersNameValid) {
                        logger.info("The header of the file is not correct");
                        reader.close();
                    }
                }

                case "DEFAULT" -> {
                    map.put("0", "IMEI1");
                    map.put("1", "IMEI2");
                    map.put("2", "IMEI3");
                    map.put("3", "IMEI4");
                    isHeadersNameValid = IntStream.range(0, length).allMatch(i -> map.get(String.valueOf(i)).equalsIgnoreCase(header[i].trim()));
                    logger.info("isHeadersNameValid {}", isHeadersNameValid);
                    if (!isHeadersNameValid) {
                        logger.info("The header of the file is not correct");
                        reader.close();
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Exception while reading the file {} {}", filePath, ex.getMessage());
            return false;
        }
        return isHeadersNameValid;
    }


    public void updateStatusAsFailInLostDeviceMgmt(WebActionDb webActionDb, String transactionId) {
        this.updateStatusInLostDeviceMgmt("Fail", transactionId);
        logger.info("updated record with status as Fail for Txn ID {}", transactionId);
        webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
    }
}
