package com.lyw.webgomoku.connect.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MessageSend {

    private String gameStatus;

    private int[][] map;

}
