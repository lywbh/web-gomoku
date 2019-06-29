package com.lyw.webgomoku.connect;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.connect.dto.MessageReceive;
import com.lyw.webgomoku.connect.dto.type.MessageTypeEnum;
import com.lyw.webgomoku.game.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import static com.lyw.webgomoku.connect.PlayerManager.*;

@Component
@ServerEndpoint(value = "/gomoku")
public class SocketHandler {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("玩家" + session.getId() + "建立连接");
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("玩家" + session.getId() + "断开连接");
        // 玩家断开连接时，检查玩家是否正在一个房间里，如果是要主动退出
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
        e.printStackTrace();
    }

    @OnMessage
    public synchronized void onMessage(String message, Session session) {
        try {
            MessageReceive recvMessage = JSON.parseObject(message, MessageReceive.class);
            switch (MessageTypeEnum.getTypeByCode(recvMessage.getType(), MessageTypeEnum.class)) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
