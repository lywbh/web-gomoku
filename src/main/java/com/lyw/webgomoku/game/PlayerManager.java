package com.lyw.webgomoku.game;

import javax.websocket.Session;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    public static Map<String, GameRoom> roomMap = new ConcurrentHashMap<>();
    public static Map<Session, String> playerIn = new ConcurrentHashMap<>();

}
