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

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MOIService {
    private Logger logger = LogManager.getLogger(this.getClass());
    private final LostDeviceDetailRepository lostDeviceDetailRepository;
    private final LostDeviceDetailHisRepository lostDeviceDetailHisRepository;
    private final SearchImeiByPoliceMgmtRepository searchImeiByPoliceMgmtRepository;
    private final LostDeviceMgmtRepository lostDeviceMgmtRepository;
    private final MDRRepository mdrRepository;
    private final SearchImeiDetailByPoliceRepository searchImeiDetailByPoliceRepository;
    private final WebActionDbRepository webActionDbRepository;
    private final AppConfig appConfig;
    private final BlackListRepository blackListRepository;
    private final BlackListHisRepository blackListHisRepository;
    private final GreyListRepository greyListRepository;
    private final GreyListHisRepository greyListHisRepository;
    private final ImeiPairDetailRepository imeiPairDetailRepository;
    private final ImeiPairDetailHisRepository imeiPairDetailHisRepository;
    private final Validation validation;
    private final SysParamRepository sysParamRepository;
    static String runningImei = null;

    public Optional<SearchImeiByPoliceMgmt> findByTxnId(String txnId) {
        Optional<SearchImeiByPoliceMgmt> response = searchImeiByPoliceMgmtRepository.findByTransactionId(txnId);
        logger.info("SearchImeiByPoliceMgmt response : {} based on txn ID :{}", txnId, response);
        return response;
    }

    public boolean isBrandAndModelValid(List<String> list) {
        Map<String, String> map = new HashMap<>();
        for (String imei : list) {
            mdrRepository.findByDeviceId(imei).ifPresent(x -> {
                map.put(imei, x.stream().map(y -> y.getBrandName() + "<>" + y.getModelName()).collect(Collectors.joining()));
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
            Boolean isExist = lostDeviceDetailRepository.existsByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(x, "DONE", List.of("LOST", "STOLEN"));
            logger.info("is {} exist in lost_device_detail {}", x, isExist);
            return isExist;
        }).findFirst();
    }

    public Optional<LostDeviceDetail> findByImeiAndStatusAndRequestType(String imei) {
        Optional<LostDeviceDetail> result = lostDeviceDetailRepository.findByImeiAndStatusIgnoreCaseAndRequestTypeIgnoreCaseIn(imei, "DONE", List.of("LOST", "STOLEN"));
        return result;
    }


    public void updateStatusAndCountFoundInLost(String status, int count, String transactionId, String failReason) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {} and count {}", status, transactionId, count);
        searchImeiByPoliceMgmtRepository.updateCountFoundInLost(status, count, transactionId, failReason);
    }

    public Optional<String> findByImei(String imei) {
        Optional<String> byImei = lostDeviceDetailRepository.findLostDeviceDetailByImei(imei);
        if (byImei.isEmpty()) {
            logger.info("No record found for IMEI {} in lost_device_detail", imei);
        }
        return byImei;
    }

    public boolean copyLostDeviceMgmtToSearchIMEIDetailByPolice(String requestId) {
        Optional<LostDeviceMgmt> byRequestId = lostDeviceMgmtRepository.findByRequestId(requestId);
        if (byRequestId.isPresent()) {
            LostDeviceMgmt lostDeviceMgmt = byRequestId.get();
            String deviceLostDateTime = lostDeviceMgmt.getDeviceLostDateTime();
            String imei1 = lostDeviceMgmt.getImei1();
            String imei2 = lostDeviceMgmt.getImei2();
            String imei3 = lostDeviceMgmt.getImei3();
            String imei4 = lostDeviceMgmt.getImei4();
            String imei = Objects.nonNull(imei1) ? imei1 : (Objects.nonNull(imei2) ? imei2 : (Objects.nonNull(imei3)) ? imei3 : imei4);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime lostDateTime = LocalDateTime.parse(deviceLostDateTime, formatter);
            SearchImeiDetailByPolice searchImeiDetailByPolice = SearchImeiDetailByPolice.builder().imei(imei).lostDateTime(lostDateTime).createdBy(lostDeviceMgmt.getCreatedBy()).transactionId(lostDeviceMgmt.getLostId()).requestId(lostDeviceMgmt.getRequestId()).deviceOwnerName(lostDeviceMgmt.getDeviceOwnerName()).deviceOwnerAddress(lostDeviceMgmt.getDeviceOwnerAddress()).contactNumber(lostDeviceMgmt.getContactNumber()).deviceOwnerNationalId(lostDeviceMgmt.getDeviceOwnerNationalID()).deviceLostPoliceStation(lostDeviceMgmt.getPoliceStation()).requestMode(lostDeviceMgmt.getRequestMode()).build();
            SearchImeiDetailByPolice save = searchImeiDetailByPoliceRepository.save(searchImeiDetailByPolice);
            return save != null;
        }
        return false;
    }

    public boolean copyRecordLostDeviceMgmtToSearchIMEIDetailByPolice(String requestID) {
        return this.copyLostDeviceMgmtToSearchIMEIDetailByPolice(requestID);
    }


    public boolean isMultipleIMEIExist(IMEISeriesModel imeiSeriesModel) {
        long count = Stream.of(imeiSeriesModel).flatMap(x -> Stream.of(x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::isNull).count();
        logger.info("No. of IMEI's found empty {}", count);
        return count == 3 ? false : true;
    }

    public List<String> imeiList(IMEISeriesModel imeiSeriesModel) {
        List<String> collect = Stream.of(imeiSeriesModel).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::nonNull).collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", collect);
        return collect;
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
        if (validation.isNumeric(record)) {
            if (!isIMEILengthAllowed(record)) {
                logger.info("Invalid IMEI {} length", record);
                return false;
            }
        }
        return true;
    }

    public <T> T save(T v1, Function<T, T> saveFunction) {
        logger.info("going to save {}", v1);
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

/*        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedExpiryDate = expiryDate.format(dateTimeFormatter);*/
        return expiryDate;
    }

    public DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    public void imeiPairDetail(String createdOn) {
        imeiPairDetailRepository.findByCreatedOnGreaterThanEqual(createdOn).ifPresentOrElse(imeiPairDetailList -> {
                    logger.info("ImeiPairDetail list {}", imeiPairDetailList);
                    imeiPairDetailList.forEach(list -> {
                        ImeiPairDetailHis imeiPairDetailHis = new ImeiPairDetailHis();
                        BeanUtils.copyProperties(list, imeiPairDetailHis);
                        imeiPairDetailHis.setAction("DELETE");
                        imeiPairDetailHis.setActionRemark("MOI");
                        logger.info("ImeiPairDetailHis {}", imeiPairDetailHis);
                        ImeiPairDetailHis savedEntity = save(imeiPairDetailHis, imeiPairDetailHisRepository::save);
                        if (savedEntity != null) {
                            imeiPairDetailRepository.deleteById(Math.toIntExact(list.getId()));
                        }
                        LostDeviceDetail lostDeviceDetail = LostDeviceDetail.builder().imei(list.getImei()).contactNumber(list.getMsisdn()).requestId(list.getTxnId()).status("DONE").requestType("LOST/STOLEN").build();
                        logger.info("lostDeviceDetail {}", lostDeviceDetail);
                        save(lostDeviceDetail, lostDeviceDetailRepository::save);

                    });
                },
                () -> logger.info("No result found for created_on {}", createdOn));
    }

    public String getTacFromIMEI(String imei) {
        return Stream.of(imei).filter(Objects::nonNull).map(x -> imei.substring(0, 8)).collect(Collectors.joining());
    }

    public String getActualIMEI(String imei) {
        return Stream.of(imei).filter(Objects::nonNull).map(x -> imei.substring(0, 14)).collect(Collectors.joining());
    }


    public void greyListDurationIsZero(String imei, String mode, LostDeviceMgmt lostDeviceMgmt) {
        findBlackListByImei(imei).ifPresentOrElse(blackList -> {
            String source = remove(blackList.getSource());
            if (Objects.isNull(source)) updateSource("MOI", imei, "BLACK_LIST");
            else {
                if (!source.equalsIgnoreCase("MOI"))
                    updateSource(source + "MOI", imei, "BLACK_LIST");
            }
        }, () -> {
            BlackList blackList = new BlackList();
            blackList.setImei(imei);
            blackList.setModeType(mode);
            blackList.setSource("MOI");
            blackList.setTxnId(lostDeviceMgmt.getRequestId());
            blackList.setTac(getTacFromIMEI(imei));
            blackList.setActualImei(getActualIMEI(imei));
            logger.info("black_list {}", blackList);
            save(blackList, blackListRepository::save);
            BlackListHis blackListHis = new BlackListHis();
            BeanUtils.copyProperties(blackList, blackListHis);
            blackListHis.setAction("ADD");
            save(blackListHis, blackListHisRepository::save);
        });
    }

    public void greyListDurationGreaterThanZero(int greyListDuration, String imei, String mode, LostDeviceMgmt lostDeviceMgmt) {

        GreyList greyList = GreyList.builder().imei(imei).msisdn(lostDeviceMgmt.getContactNumber()).modeType(mode).source("MOI").expiryDate(expiryDate(greyListDuration)).txnId(lostDeviceMgmt.getRequestId()).tac(getTacFromIMEI(imei)).actualImei(getActualIMEI(imei)).build();
        save(greyList, greyListRepository::save);
        logger.info("greylist {}", greyList);
        GreyListHis greyListHis = new GreyListHis();
        BeanUtils.copyProperties(greyList, greyListHis);
        greyListHis.setAction("ADD");
        logger.info("greyListHis {}", greyListHis);
        save(greyListHis, greyListHisRepository::save);
    }

    public Optional<LostDeviceMgmt> findByRequestId(String id) {
        Optional<LostDeviceMgmt> response = lostDeviceMgmtRepository.findByRequestId(id);
        logger.info("LostDeviceMgmt response : {} based on txn ID :{}", response, id);
        return response;
    }

    public Optional<LostDeviceMgmt> findByLostId(String id) {
        Optional<LostDeviceMgmt> response = lostDeviceMgmtRepository.findByLostId(id);
        logger.info("LostDeviceMgmt response for recovery: {} based on txn ID :{}", response, id);
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
        logger.info("updated lost_device_mgmt with status {} and requestId {}", status, requestId);
        lostDeviceMgmtRepository.updateStatus(status, requestId);
    }

/*
    public boolean isMultipleIMEIExist(LostDeviceMgmt lostDeviceMgmt) {
        long count = Stream.of(lostDeviceMgmt).flatMap(x -> Stream.of(x.getImei2(), x.getImei3(), x.getImei4())).filter(String::isBlank).count();
        logger.info("No. of IMEI's found empty {}", count);
        return count == 3 ? false : true;
    }

   public Function<LostDeviceMgmt, List<String>> imeiList = (lostDeviceMgmt) -> {
        List<String> collect = Stream.of(lostDeviceMgmt).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::nonNull).collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", collect);
        return collect;
    };*/

    public Function<IMEISeriesModel, List<String>> imeiSeries = (imeiSeries) -> {
        List<String> collect = Stream.of(imeiSeries).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::nonNull).collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", collect);
        return collect;
    };

    public Predicate<String> isIMEILengthAllowed = record -> {
        int length = record.length();
        return length == 14 || length == 15 || length == 16;
    };

    public Predicate<String> isNumericAndValid = record -> {
        if (new Validation().isNumeric(record)) {
            return isIMEILengthAllowed.test(record) ? true : false;
        }
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

    /*     updateStatusInLostDeviceMgmt("Done", transactionId);
         webActionDbRepository.updateWebActionStatus(5, id);*/


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



/*    public void updateStatus(String status, String txnId, String failReason) {
        logger.info("updated SearchImeiByPoliceMgmt with status {} and txnId {}", status, txnId);
        searchImeiByPoliceMgmtRepository.updateStatus(status, txnId, failReason);
    }*/


/*
    public boolean requestIdDetails(String requestID, String transactionId, WebActionDb webActionDb) {

 */
/*       return findByImei(imei).map(requestId -> {
            logger.info("found requestId {} for IMEI {}", requestId, imei);
            int i = copyLostDeviceMgmtToSearchIMEIDetailByPolice(requestId);
            if (i > 0) {
                updateStatusAndCountFoundInLost("DONE", 1, transactionId, null);
                logger.info("updated record with status as DONE and count_found_in _lost as 1 for Txn ID {}", transactionId);
                webActionDbRepository.updateWebActionStatus(5, webActionDb.getId());
                return true;
            }
            return false;
        }).orElse(false);*//*

        return false;
    }
*/


        /*    public List<String> imeiList(IMEISeriesModel imeiSeriesModel) {
            List<String> collect = Stream.of(imeiSeriesModel).flatMap(x -> Stream.of(x.getImei1(), x.getImei2(), x.getImei3(), x.getImei4())).filter(Objects::nonNull).collect(Collectors.toList());
            logger.info("Non-null IMEI list {}", collect);
            return collect;
        }
    public <T> List<String> imeiList(T t, Function<T, List<String>> imeiExtractor) {
        List<String> imeiList = imeiExtractor.apply(t).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        logger.info("Non-null IMEI list {}", imeiList);
        return imeiList;
    }
*/
}
