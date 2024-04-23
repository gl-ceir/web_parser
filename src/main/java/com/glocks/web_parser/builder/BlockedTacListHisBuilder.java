package com.glocks.web_parser.builder;

import com.glocks.web_parser.model.app.BlackList;
import com.glocks.web_parser.model.app.BlackListHis;
import com.glocks.web_parser.model.app.BlockedTacList;
import com.glocks.web_parser.model.app.BlockedTacListHis;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class BlockedTacListHisBuilder {

    public static BlockedTacListHis forInsert(BlockedTacList blockedTacList, int operation) {
        BlockedTacListHis blockedTacListHis = new BlockedTacListHis();
        blockedTacListHis.setOperation(operation);
        blockedTacListHis.setRemarks(blockedTacList.getRemarks());//
        blockedTacListHis.setModeType(blockedTacList.getModeType());
        blockedTacListHis.setRequestType(blockedTacList.getRequestType());
        blockedTacListHis.setTxnId(blockedTacList.getTxnId());
        blockedTacListHis.setUserId(blockedTacList.getUserId());
        blockedTacListHis.setTac(blockedTacList.getTac());
        blockedTacListHis.setSource(blockedTacList.getSource());
        return blockedTacListHis;
    }
}
