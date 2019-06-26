package com.lyw.webgomoku.connect;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.config.ThreadPool;
import com.lyw.webgomoku.connect.dto.MessageReceive;
import com.lyw.webgomoku.connect.dto.type.MessageTypeEnum;
import com.lyw.webgomoku.game.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@ServerEndpoint(value = "/gomoku")
public class SocketHandler {

    private static CopyOnWriteArraySet<SocketHandler> socketSet = new CopyOnWriteArraySet<>();

    private static Map<String, GameRoom> roomMap = new HashMap<>();
    private static Map<String, String> playerIn = new HashMap<>();

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        socketSet.add(this);
    }

    @OnClose
    public void onClose() {
        socketSet.remove(this);
    }

    @OnError
    public void onError(Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        MessageReceive recvMessage = JSON.parseObject(message, MessageReceive.class);
        MessageTypeEnum messageType = MessageTypeEnum.getTypeByCode(recvMessage.getType(), MessageTypeEnum.class);
        switch (messageType) {
            case JOIN:
                String roomCode = recvMessage.getRoomCode();
                GameRoom currentRoom = roomMap.get(roomCode);
                if (currentRoom == null) {
                    GameRoom newRoom = new GameRoom(session);
                    roomMap.put(roomCode, newRoom);
                    playerIn.put(session.getId(), roomCode);
                } else if (currentRoom.getGameStatus() == GameRoom.GameStatus.PREPARE) {
                    currentRoom.join(session);
                    playerIn.put(session.getId(), roomCode);
                    ThreadPool.gamePool.submit(currentRoom);
                    // TODO 怎么判断房间线程跑完（游戏结束）了，然后移除这个房间？（开个观察者线程轮询所有room的状态？）
                } else {
                    // TODO 拒绝这个加入请求，返回提示游戏已开始
                }
                break;
            case QUIT:
                break;
            case PUT_CHESS:
                GameRoom room = roomMap.get(playerIn.get(session.getId()));
                Assert.isTrue(room != null, "非法请求，该玩家未加入任何房间");
                room.putAction(session, recvMessage.getI(), recvMessage.getJ());
                break;
        }
    }

    private void sendAsync(String message) {
        try {
            this.session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            // TODO 发送失败，前端页面可能不改变，怎么办？（开个观察者线程轮询所有room，保持一个频率推送最新状态？）
        }
    }

}
