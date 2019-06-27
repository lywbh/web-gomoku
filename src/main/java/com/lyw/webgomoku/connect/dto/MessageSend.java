package com.lyw.webgomoku.connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageSend {

    private String gameStatus;

    private int[][] map;

}
