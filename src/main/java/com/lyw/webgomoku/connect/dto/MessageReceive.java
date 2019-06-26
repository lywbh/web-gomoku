package com.lyw.webgomoku.connect.dto;

import lombok.Data;

@Data
public class MessageReceive {

    private String type;

    private String roomCode;

    private Integer i;

    private Integer j;

}
