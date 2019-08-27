package com.lyw.webgomoku.controller;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.dto.type.MessageTypeEnum;
import com.lyw.webgomoku.game.GameRoom;
import com.lyw.webgomoku.dto.MessageReceive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import static com.lyw.webgomoku.game.PlayerManager.playerIn;
import static com.lyw.webgomoku.game.PlayerManager.roomMap;

@Slf4j
@Component
@ServerEndpoint(value = "/gomoku")
public class SocketEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        log.info("玩家" + session.getId() + "建立连接");
    }

    @OnClose
    public void onClose(Session session) {
        log.info("玩家" + session.getId() + "断开连接");
        String roomId = playerIn.get(session);
        if (roomId != null) {
            GameRoom room = roomMap.get(roomId);
            if (room != null) {
                room.quit(session);
            }
        }
    }

    @OnError
    public void onError(Throwable e) {
        log.error("连接异常", e);
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) {
        log.info("玩家" + session.getId() + "请求消息：" + message);
        try {
            MessageReceive recvMessage = JSON.parseObject(message, MessageReceive.class);
            switch (MessageTypeEnum.getTypeByCode(recvMessage.getType())) {
                case JOIN:
                    String roomId = playerIn.get(session);
                    Assert.isTrue(roomId == null, "玩家" + session.getId() + "正在房间" + roomId + "内，请先退出");
                    roomId = recvMessage.getRoomId();
                    Assert.isTrue(!StringUtils.isEmpty(roomId), "玩家" + session.getId() + "请求加入的房间号为空");
                    GameRoom currentRoom = roomMap.get(roomId);
                    // 房间号不存在，先创建一个空房间
                    if (currentRoom == null) {
                        currentRoom = new GameRoom(roomId);
                    }
                    // 加入房间
                    currentRoom.join(session);
                    break;
                case QUIT:
                    // 退出房间
                    roomId = playerIn.get(session);
                    Assert.isTrue(roomId != null, "玩家" + session.getId() + "未加入任何房间，不能退出");
                    GameRoom room2Quit = roomMap.get(roomId);
                    Assert.isTrue(room2Quit != null, "玩家" + session.getId() + "的房间找不到了");
                    room2Quit.quit(session);
                    break;
                case PUT_CHESS:
                    // 下棋
                    roomId = playerIn.get(session);
                    Assert.isTrue(roomId != null, "玩家" + session.getId() + "未加入任何房间，不能落子");
                    GameRoom room2Put = roomMap.get(roomId);
                    Assert.isTrue(room2Put != null, "玩家" + session.getId() + "的房间找不到了");
                    room2Put.putAction(session, recvMessage.getI(), recvMessage.getJ());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("玩家" + session.getId() + "消息处理异常", e);
        }
    }

}
