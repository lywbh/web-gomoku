package com.lyw.webgomoku.game;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.config.ThreadPoolConfig;
import com.lyw.webgomoku.dto.MessageSend;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;

import static com.lyw.webgomoku.game.PlayerManager.*;

@Slf4j
public class GameRoom implements Runnable {

    @AllArgsConstructor
    public enum GameStatus {
        PREPARE("0"),
        WHITE_WAITING("1"),
        BLACK_WAITING("2"),
        WHITE_WIN("3"),
        BLACK_WIN("4"),
        DRAW("5"),
        TERMINAL("6");

        private String code;
    }

    private String roomId;

    private Session whitePlayer;
    private Session blackPlayer;
    private GameStatus gameStatus;
    private ChessMap chessMap;

    /**
     * 下一步落子动作，游戏线程里监听这个值的变化
     */
    private ChessAction chessAction;

    public GameRoom(String roomId) {
        this.roomId = roomId;
        gameStatus = GameStatus.PREPARE;
        ThreadPoolConfig.gamePool.submit(this);
        roomMap.put(roomId, this);
    }

    /**
     * 游戏加入，如果满两人则初始化棋盘，游戏开始，白子先手
     *
     * @param player 玩家的session
     */
    public synchronized void join(Session player) {
        log.info("room-" + roomId + "：" + "玩家" + player.getId() + "进入房间");
        if (gameStatus == GameStatus.TERMINAL) {
            throw new IllegalStateException("roomId" + roomId + "：" + "游戏已结束，玩家" + player.getId() + "进入房间失败");
        }
        if (whitePlayer == null && blackPlayer == null) {
            whitePlayer = player;
        } else if (whitePlayer == null) {
            whitePlayer = player;
        } else if (blackPlayer == null) {
            blackPlayer = player;
        } else {
            throw new IllegalStateException("roomId" + roomId + "：" + "房间已满，玩家" + player.getId() + "进入房间失败");
        }
        playerIn.put(player, roomId);
        if (whitePlayer != null && blackPlayer != null) {
            chessMap = new ChessMap();
            gameStatus = GameStatus.WHITE_WAITING;
        }
        notify();
    }

    /**
     * 玩家退出时清空棋盘，一人退出则回到PREPARE，两人都退出则进入TERMINAL
     *
     * @param player 玩家的session
     */
    public synchronized void quit(Session player) {
        log.info("room-" + roomId + "：" + "玩家" + player.getId() + "退出房间");
        if (gameStatus == GameStatus.TERMINAL) {
            throw new IllegalStateException("roomId" + roomId + "：" + "游戏已结束，玩家" + player.getId() + "退出房间失败");
        }
        chessMap = null;
        if (whitePlayer != null && whitePlayer == player) {
            whitePlayer = null;
            playerIn.remove(player);
        } else if (blackPlayer != null && blackPlayer == player) {
            blackPlayer = null;
            playerIn.remove(player);
        } else {
            throw new IllegalStateException("roomId" + roomId + "：" + "玩家" + player.getId() + "不在房间里，不能退出");
        }
        if (whitePlayer == null && blackPlayer == null) {
            gameStatus = GameStatus.TERMINAL;
        } else {
            gameStatus = GameStatus.PREPARE;
        }
        notify();
    }

    public synchronized void putAction(Session player, int i, int j) {
        log.info("room-" + roomId + "：" + "玩家" + player.getId() + "在（" + i + "，" + j + "）处落子");
        ChessAction action;
        if (player == whitePlayer && gameStatus == GameStatus.WHITE_WAITING) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_WHITE);
        } else if (player == blackPlayer && gameStatus == GameStatus.BLACK_WAITING) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_BLACK);
        } else {
            log.warn("roomId" + roomId + "：" + "玩家" + player.getId() + "当前不能落子");
            return;
        }
        chessAction = action;
        notify();
    }

    @Override
    public void run() {
        enableWatcher();
        MainLoop:
        while (true) {
            synchronized (this) {
                try {
                    wait();
                    switch (gameStatus) {
                        case PREPARE:
                            // 准备状态，等待另一位玩家，啥都不做
                            break;
                        case WHITE_WAITING:
                            // 检查有无落白子的动作
                            if (chessAction != null && chessAction.getPoint() == ChessMap.MapPointEnum.CHESS_WHITE) {
                                chessMap.put(chessAction);
                                if (chessMap.checkFive(chessAction)) {
                                    gameStatus = GameStatus.WHITE_WIN;
                                    break MainLoop;
                                } else if (chessMap.checkFull()) {
                                    gameStatus = GameStatus.DRAW;
                                    break MainLoop;
                                } else {
                                    gameStatus = GameStatus.BLACK_WAITING;
                                    pushCurrent();
                                }
                            }
                            break;
                        case BLACK_WAITING:
                            // 检查有无落黑子的动作
                            if (chessAction != null && chessAction.getPoint() == ChessMap.MapPointEnum.CHESS_BLACK) {
                                chessMap.put(chessAction);
                                if (chessMap.checkFive(chessAction)) {
                                    gameStatus = GameStatus.BLACK_WIN;
                                    break MainLoop;
                                } else if (chessMap.checkFull()) {
                                    gameStatus = GameStatus.DRAW;
                                    break MainLoop;
                                } else {
                                    gameStatus = GameStatus.WHITE_WAITING;
                                    pushCurrent();
                                }
                            }
                            break;
                        default:
                            // 终结状态会直接break出去
                            break MainLoop;
                    }
                } catch (Exception e) {
                    log.error("room-" + roomId  + "：主线程异常", e);
                }
            }
        }
        // 游戏结束
        pushCurrent();
        gameStatus = GameStatus.TERMINAL;
        closeRoom();
    }

    /**
     * 移除所有玩家，删除房间
     */
    private void closeRoom() {
        if (playerIn != null) {
            if (blackPlayer != null) {
                playerIn.remove(blackPlayer);
            }
            if (whitePlayer != null) {
                playerIn.remove(whitePlayer);
            }
        }
        if (roomMap != null) {
            roomMap.remove(roomId);
        }
    }

    /**
     * 定时推送房间状态、棋盘信息
     */
    private void enableWatcher() {
        ThreadPoolConfig.watcherPool.submit(() -> {
            while (gameStatus != GameStatus.TERMINAL) {
                pushCurrent();
                try {
                    Thread.sleep(2000);
                } catch (Exception ignore) {
                }
            }
        });
    }

    private void pushCurrent() {
        try {
            checkAndPush(whitePlayer);
            checkAndPush(blackPlayer);
        } catch (Exception e) {
            log.error("room-" + roomId  + "：推送棋盘信息异常", e);
        }
    }

    private void checkAndPush(Session player) {
        if (player != null) {
            if (player.isOpen()) {
                player.getAsyncRemote().sendText(JSON.toJSONString(currentStatus()));
            } else {
                log.info("room-" + roomId + "：" + "发现玩家" + player.getId() + "意外断开，把这个用户踢出");
                quit(player);
            }
        }
    }

    private MessageSend currentStatus() {
        int[][] map = chessMap == null ? null : chessMap.getMap();
        return new MessageSend(gameStatus.code, map);
    }

}
