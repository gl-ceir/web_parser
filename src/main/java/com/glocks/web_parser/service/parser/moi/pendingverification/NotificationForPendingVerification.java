package com.glocks.web_parser.service.parser.moi.pendingverification;

import com.glocks.web_parser.dto.EmailDto;
import com.glocks.web_parser.dto.SmsNotificationDto;
import com.glocks.web_parser.model.app.LostDeviceMgmt;
import com.glocks.web_parser.model.app.WebActionDb;
import com.glocks.web_parser.repository.app.EirsResponseParamRepository;
import com.glocks.web_parser.repository.app.SysParamRepository;
import com.glocks.web_parser.service.email.EmailService;
import com.glocks.web_parser.service.parser.BulkIMEI.UtilFunctions;
import com.glocks.web_parser.service.parser.moi.utility.ConfigurableParameter;
import com.glocks.web_parser.service.sms.SmsService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import static com.glocks.web_parser.constants.BulkCheckImeiConstants.*;

@Component
@RequiredArgsConstructor
public class NotificationForPendingVerification {
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final SmsService smsService;
    private final EmailService emailService;
    private final UtilFunctions utilFunctions;
    private final EirsResponseParamRepository eirsResponseParamRepository;
    private final SysParamRepository sysParamRepository;

    public void sendNotification(WebActionDb webActionDb, LostDeviceMgmt lostDeviceMgmt, String channel, String currentFileName, String uploadedFilePath) {
        String requestId = lostDeviceMgmt.getRequestId();
        String subFeature = webActionDb.getSubFeature();
        String feature = webActionDb.getFeature();
        String email = lostDeviceMgmt.getEmailForOtp();
        String language = lostDeviceMgmt.getLanguage() == null ?
                sysParamRepository.getValueFromTag("systemDefaultLanguage") : lostDeviceMgmt.getLanguage();
        switch (channel) {
            case "SMS" -> {
                SmsNotificationDto smsNotificationDto = new SmsNotificationDto();
                smsNotificationDto.setMsgLang(language);
                smsNotificationDto.setSubFeature(subFeature);
                smsNotificationDto.setFeatureName(feature);
                smsNotificationDto.setFeatureTxnId(requestId);
                smsNotificationDto.setEmail(email);
                smsNotificationDto.setChannelType("SMS");
                smsNotificationDto.setMsisdn(lostDeviceMgmt.getContactNumber());
                smsNotificationDto.setMessage(utilFunctions.replaceString(eirsResponseParamRepository.findValue(ConfigurableParameter.PENDING_VERIFICATION_FEATURE.getValue(),
                        language, ConfigurableParameter.PENDING_VERIFICATION_TAG.getValue()), 0L, requestId, currentFileName));
                logger.info("SMS notification sent {}", smsNotificationDto);
                smsService.callSmsNotificationApi(smsNotificationDto);
            }
            case "EMAIL" -> {
                EmailDto emailDto = new EmailDto();
                emailDto.setEmail(email);
                emailDto.setTxn_id(requestId);
                emailDto.setLanguage(language);
                emailDto.setFile(uploadedFilePath);
                emailDto.setSubject(utilFunctions.replaceString(eirsResponseParamRepository.findValue(featureName,
                        language, numberOfRecordsSubject), 0L, lostDeviceMgmt.getRequestId(), currentFileName));
                emailDto.setMessage(utilFunctions.replaceString(eirsResponseParamRepository.findValue(featureName,
                        language, numberOfRecordsMessage), 0L, requestId, currentFileName));
                emailService.callEmailApi(emailDto);
            }
        }

    }
}
