package com.lyw.webgomoku.game;

import javax.websocket.Session;

public class GameRoom implements Runnable {

    public enum GameStatus {
        PREPARE,
        WHITE_WAITING, BLACK_WAITING,
        WHITE_WIN, BLACK_WIN, DRAW,
        TERMINAL
    }

    private Session whitePlayer;
    private Session blackPlayer;
    private GameStatus gameStatus;
    private ChessMap chessMap;

    /**
     * 下一步落子动作，游戏线程里监听这个值的变化
     */
    private ChessAction chessAction;

    public void putAction(Session session, int i, int j) {
        ChessAction action;
        if (session.getId().equals(whitePlayer.getId())) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_WHITE);
        } else if (session.getId().equals(blackPlayer.getId())) {
            action = new ChessAction(i, j, ChessMap.MapPointEnum.CHESS_BLACK);
        } else {
            throw new IllegalArgumentException("该玩家" + session.getId() + "不在房间里，不能落子");
        }
        chessAction = action;
    }

    /**
     * 第一个进入的人会创建这个房间，执黑子
     *
     * @param whitePlayer 执白子玩家的session
     */
    public GameRoom(Session whitePlayer) {
        this.whitePlayer = whitePlayer;
        gameStatus = GameStatus.PREPARE;
    }

    /**
     * 第二个进入的人执白子，游戏开始，黑子先手
     *
     * @param blackPlayer 执黑子玩家的session
     */
    public void join(Session blackPlayer) {
        this.blackPlayer = blackPlayer;
        gameStatus = GameStatus.BLACK_WAITING;
        chessMap = new ChessMap();
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    @Override
    public void run() {
        while (gameStatus != GameStatus.TERMINAL) {
            synchronized (this) {
                switch (gameStatus) {
                    case PREPARE:
                        // PREPARE状态，线程还没开启，不会进到这来的，忽略
                        break;
                    case WHITE_WAITING:
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
                            chessAction = null;
                            // TODO 给双方推送当前棋盘状态
                        }
                        break;
                    case BLACK_WAITING:
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
                            chessAction = null;
                            // TODO 给双方推送当前棋盘状态
                        }
                        break;
                    case WHITE_WIN:
                        // TODO 给双方推送胜利和失败信息
                        gameStatus = GameStatus.TERMINAL;
                        break;
                    case BLACK_WIN:
                        // TODO 给双方推送胜利和失败信息
                        gameStatus = GameStatus.TERMINAL;
                        break;
                    case DRAW:
                        // TODO 给双方推送平局信息
                        gameStatus = GameStatus.TERMINAL;
                        break;
                    case TERMINAL:
                        // 终局了，线程结束，可以销毁这个房间
                        break;
                }
            }
        }
    }

}
