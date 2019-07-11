package com.lyw.webgomoku.game;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
class ChessAction {

    private int i;
    private int j;
    private ChessMap.MapPointEnum point;

}
