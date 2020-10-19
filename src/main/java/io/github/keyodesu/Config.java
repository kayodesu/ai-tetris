package io.github.keyodesu;

/**
 * @author Yo Ka
 */
public final class Config {
    private Config() { }

    public static final String TITLE = "AI Tetris";

    public static final int ROW = 20;   // 行数
    public static final int COL = 10;   // 列数
    public static final double GAP_AROUND_PROPORTION = 0.01;   // 1%

    public static final int FAST_DOWN_CELL_COUNT = 3;

    public static final String BACKGROUND_COLOR = "0xa7b7b1";

    public static final String AI_PLAYS = "AI plays";
    public static final String I_PLAY = "I play";
}
