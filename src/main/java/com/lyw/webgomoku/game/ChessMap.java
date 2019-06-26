package com.lyw.webgomoku.game;

import org.springframework.util.Assert;

public class ChessMap {

    private static final int MAP_WIDTH = 15;
    private static final int MAP_HEIGHT = 15;

    public enum MapPointEnum {
        EMPTY, CHESS_WHITE, CHESS_BLACK;
    }

    private MapPointEnum[][] map;
    private int whiteCount;
    private int blackCount;

    public ChessMap() {
        map = new MapPointEnum[MAP_WIDTH][MAP_HEIGHT];
        for (int i = 0; i < MAP_WIDTH; ++i) {
            for (int j = 0; j < MAP_HEIGHT; ++j) {
                map[i][j] = MapPointEnum.EMPTY;
            }
        }
        whiteCount = 0;
        blackCount = 0;
    }

    /**
     * 落子
     */
    public void put(ChessAction chessAction) {
        pointCheck(chessAction);
        map[chessAction.getI()][chessAction.getJ()] = chessAction.getPoint();
        if (chessAction.getPoint() == MapPointEnum.CHESS_WHITE) {
            whiteCount++;
        } else if (chessAction.getPoint() == MapPointEnum.CHESS_BLACK) {
            blackCount++;
        }
    }

    /**
     * 检查是否有连成五子，传入上次落子，只扫描周围11x11的范围
     */
    public boolean checkFive(ChessAction chessAction) {
        pointCheck(chessAction);
        return false;
    }

    /**
     * 检查棋盘是否已满
     * @return 是否结束
     */
    public boolean checkFull() {
        return whiteCount + blackCount >= MAP_WIDTH * MAP_HEIGHT;
    }

    /**
     * 校验输入合法性
     */
    private void pointCheck(ChessAction chessAction) {
        int i = chessAction.getI();
        int j = chessAction.getJ();
        MapPointEnum point = chessAction.getPoint();
        Assert.isTrue(point != MapPointEnum.EMPTY, "落子类型有误");
        Assert.isTrue(i >= 0 && i <= MAP_WIDTH && j >= 0 && j <= MAP_HEIGHT, "落子范围有误");
        Assert.isTrue(map[i][j] == MapPointEnum.EMPTY, "落子位置重复");
    }

}
