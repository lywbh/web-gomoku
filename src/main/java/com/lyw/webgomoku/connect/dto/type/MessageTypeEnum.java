package com.lyw.webgomoku.connect.dto.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    JOIN("1"),
    QUIT("2"),
    PUT_CHESS("3");

    private String code;

    public static MessageTypeEnum getTypeByCode(String code) {
        MessageTypeEnum obj = null;
        for (MessageTypeEnum e : MessageTypeEnum.values()) {
            if (e.getCode().equals(code)) {
                obj = e;
                break;
            }
        }
        return obj;
    }

}
