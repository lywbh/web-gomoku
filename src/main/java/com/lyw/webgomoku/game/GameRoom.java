package com.lyw.webgomoku.game;

import com.alibaba.fastjson.JSON;
import com.lyw.webgomoku.config.ThreadPool;
import com.lyw.webgomoku.connect.dto.MessageSend;
import lombok.AllArgsConstructor;

import javax.websocket.Session;

import static com.lyw.webgomoku.connect.PlayerManager.*;

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
        ThreadPool.gamePool.submit(this);
        roomMap.put(roomId, this);
    }

    /**
     * 游戏加入，如果满两人则初始化棋盘，游戏开始，白子先手
     *
     * @param player 玩家的session
     */
    public synchronized void join(Session player) {
        if (gameStatus == GameStatus.TERMINAL) {
            throw new IllegalStateException("房间正在清理，请稍后");
        }
        if (whitePlayer == null && blackPlayer == null) {
            whitePlayer = player;
        } else if (whitePlayer == null) {
            whitePlayer = player;
        } else if (blackPlayer == null) {
            blackPlayer = player;
        } else {
            throw new IllegalStateException("房间已满，不允许加入");
        }
        playerIn.put(player, roomId);
        if (whitePlayer != null && blackPlayer != null) {
            chessMap = new ChessMap();
            gameStatus = GameStatus.WHITE_WAITING;
        }
    }

    /**
     * 玩家退出时清空棋盘，一人退出则回到PREPARE，两人都退出则进入TERMINAL
     *
     * @param player 玩家的session
     */
    public synchronized void quit(Session player) {
        if (gameStatus == GameStatus.TERMINAL) {
            throw new IllegalStateException("游戏已结束");
        }
        chessMap = null;
        if (whitePlayer != null && whitePlayer == player) {
            whitePlayer = null;
            playerIn.remove(player);
        } else if (blackPlayer != null && blackPlayer == player) {
            blackPlayer = null;
            playerIn.remove(player);
        } else {
            throw new IllegalArgumentException("该玩家" + player.getId() + "不在房间里，不能退出");
        }
        if (whitePlayer == null && blackPlayer == null) {
            gameStatus = GameStatus.TERMINAL;
        } else {
            gameStatus = GameStatus.PREPARE;
        }
    }

    public synchronized void putAction(Session player, int i, int j) {
        ChessAction action;
        if (player == whitePlayer && gameStatus == GameStatus.WHITE_WAITING) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_WHITE);
        } else if (player == blackPlayer && gameStatus == GameStatus.BLACK_WAITING) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_BLACK);
        } else {
            throw new IllegalArgumentException("玩家" + player.getId() + "非法操作，不能落子");
        }
        chessAction = action;
    }

    @Override
    public void run() {
        enableWatcher();
        while (gameStatus != GameStatus.TERMINAL) {
            synchronized (this) {
                switch (gameStatus) {
                    case PREPARE:
                        // 准备状态，等待另一位玩家，啥都不做
                        break;
                    case WHITE_WAITING:
                        try {
                            // 检查有无落白子的动作
                            if (chessAction != null && chessAction.getPoint() == ChessMap.MapPointEnum.CHESS_WHITE) {
                                chessMap.put(chessAction);
                                // 落子完成后检查结果
                                if (chessMap.checkFive(chessAction)) {
                                    gameStatus = GameStatus.WHITE_WIN;
                                } else if (chessMap.checkFull()) {
                                    gameStatus = GameStatus.DRAW;
                                } else {
                                    gameStatus = GameStatus.BLACK_WAITING;
                                }
                                pushCurrent();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        chessAction = null;
                        break;
                    case BLACK_WAITING:
                        try {
                            // 检查有无落黑子的动作
                            if (chessAction != null && chessAction.getPoint() == ChessMap.MapPointEnum.CHESS_BLACK) {
                                chessMap.put(chessAction);
                                // 落子完成后检查结果
                                if (chessMap.checkFive(chessAction)) {
                                    gameStatus = GameStatus.BLACK_WIN;
                                } else if (chessMap.checkFull()) {
                                    gameStatus = GameStatus.DRAW;
                                } else {
                                    gameStatus = GameStatus.WHITE_WAITING;
                                }
                                pushCurrent();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        chessAction = null;
                        break;
                    case WHITE_WIN:
                    case BLACK_WIN:
                    case DRAW:
                        // 游戏结束了，稍等一下子线程推送结果，再把状态终止
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignore) {
                        }
                        gameStatus = GameStatus.TERMINAL;
                        break;
                    case TERMINAL:
                        // TERMINAL会直接跳出，不会有这个情况
                        break;
                }
            }
            // 停一会让出CPU
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {
            }
        }
        // TERMINAL后线程结束，关闭房间
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
        ThreadPool.watcherPool.submit(() -> {
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
            if (whitePlayer != null) {
                whitePlayer.getAsyncRemote().sendText(JSON.toJSONString(currentStatus()));
            }
            if (blackPlayer != null) {
                blackPlayer.getAsyncRemote().sendText(JSON.toJSONString(currentStatus()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessageSend currentStatus() {
        int[][] map = chessMap == null ? null : chessMap.getMap();
        return new MessageSend(gameStatus.code, map);
    }

}
