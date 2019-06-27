package com.lyw.webgomoku.connect;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.connect.dto.MessageReceive;
import com.lyw.webgomoku.connect.dto.type.MessageTypeEnum;
import com.lyw.webgomoku.game.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import static com.lyw.webgomoku.connect.PlayerManager.*;

@Component
@ServerEndpoint(value = "/gomoku")
public class SocketHandler {

    @OnOpen
    public void onOpen(Session session) {
        playerSet.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        playerSet.remove(session);
        // 玩家断开连接时，检查玩家是否正在一个房间里，如果是要主动退出
        GameRoom room = roomMap.get(playerIn.get(session));
        if (room != null) {
            room.quit(session);
        }
    }

    @OnError
    public void onError(Throwable error) {
        error.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        MessageReceive recvMessage = JSON.parseObject(message, MessageReceive.class);
        switch (MessageTypeEnum.getTypeByCode(recvMessage.getType(), MessageTypeEnum.class)) {
            case JOIN:
                String roomId = recvMessage.getRoomCode();
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
                currentRoom = roomMap.get(playerIn.get(session));
                Assert.isTrue(currentRoom != null, "非法请求，该玩家" + session.getId() + "未加入任何房间");
                currentRoom.quit(session);
                break;
            case PUT_CHESS:
                // 下棋
                currentRoom = roomMap.get(playerIn.get(session));
                Assert.isTrue(currentRoom != null, "非法请求，该玩家" + session.getId() + "未加入任何房间");
                currentRoom.putAction(session, recvMessage.getI(), recvMessage.getJ());
                break;
        }
    }

}
