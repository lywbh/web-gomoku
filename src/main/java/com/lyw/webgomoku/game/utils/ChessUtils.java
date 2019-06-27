package com.lyw.webgomoku.game.utils;

import com.lyw.webgomoku.game.ChessMap;

public class ChessUtils {

    /**
     * 判断是否五子相连
     */
    public static boolean dudgeWin(ChessMap.MapPointEnum[][] map, int width, int y, int x) {
        ChessMap.MapPointEnum qi = map[y][x];

        // 上下
        int c = 0;
        for (int i = 0; i < width; i++) {
            if (qi == map[i][x]) {
                c++;
                if (c >= 5) {
                    return true;
                }
            } else {
                c = 0;
            }
        }

        // 左右
        c = 0;
        for (int i = 0; i < width; i++) {
            if (qi == map[y][i]) {
                c++;
                if (c >= 5) {
                    return true;
                }
            } else {
                c = 0;
            }
        }

        // 平行于主对角线
        c = 0;
        if (x > y) {
            // 主对角线上
            for (int i = 0, j = x - y; i < width && j < width; i++, j++) {
                if (map[i][j] == qi) {
                    c++;
                    if (c >= 5) {
                        return true;
                    }
                } else {
                    c = 0;
                }
            }
        } else if (x < y) {
            // 主对角线下
            for (int i = y - x, j = 0; i < width && j < width; i++, j++) {
                if (map[i][j] == qi) {
                    c++;
                    if (c >= 5) {
                        return true;
                    }
                } else {
                    c = 0;
                }
            }
        } else {
            // 在主对角线上
            for (int i = 0, j = 0; i < width && j < width; i++, j++) {
                if (map[i][j] == qi) {
                    c++;
                    if (c >= 5) {
                        return true;
                    }
                } else {
                    c = 0;
                }
            }
        }

        // 平行于副对角线的搜索
        c = 0;
        for (int i = y, j = x; i >= 0 && j < width; i--, j++) {
            if (map[i - 1][j + 1] == qi) {
                c++;
                if (c >= 5) {
                    return true;
                }
            } else {
                break;
            }
        }
        for (int i = y, j = x; i < width && j >= 0; i++, j--) {
            if (map[i][j] == qi) {
                c++;
                if (c >= 5) {
                    return true;
                }
            } else {
                break;
            }
        }

        return false;
    }

}
