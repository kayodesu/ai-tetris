package io.github.keyodesu;

import io.github.keyodesu.block.Block;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * @author Yo Ka
 */
public class Container extends Canvas {

    /*
     * 两个小方块之间的间隙占小方块边长的比例
     * 也就是：小方块之间的间隙 == 小方块边长 * GAP_BETWEEN_BLOCKS_PROPORTION
     */
    private static final double GAP_BETWEEN_BLOCKS_PROPORTION = 0.14;

    /*
     * 小方块内部的间隙占小方块边长的比例
     * 也就是：小方块内部的间隙 == 小方块边长 * GAP_INNER_BLOCK_PROPORTION
     */
    private static final double GAP_INNER_BLOCK_PROPORTION = 0.2;

    private GraphicsContext gc;

    // 画布中每个小方块的状态
    public enum CellStat {
        EMPTY,     // 空
        MOVING,    // 正在移动的痕迹
        SOLIDIFY   // 已经固定存在的小方块
    }

    public double blockSideLen;
    public double gapBetweenBlocks;
    private double gapInnerBlock;

    // 画布状态表示, 零点在左上角
    private CellStat[][] statMatrix;
    private int columnsCount, rowsCount;

    public int getColumnsCount() {
        return columnsCount;
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public Container(double width, double height, int columnsCount, int rowsCount) {
        super(width, height);

        this.columnsCount = columnsCount;
        this.rowsCount = rowsCount;
        statMatrix = new CellStat[columnsCount][rowsCount];

        // blockSideLen*COL + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(COL-1) = width
        var len0 = width / (columnsCount + GAP_BETWEEN_BLOCKS_PROPORTION*(columnsCount-1));
        var len1 = height / (rowsCount + GAP_BETWEEN_BLOCKS_PROPORTION*(rowsCount-1));

        blockSideLen = Math.min(len0, len1);
        gapBetweenBlocks = blockSideLen * GAP_BETWEEN_BLOCKS_PROPORTION;
        gapInnerBlock = blockSideLen * GAP_INNER_BLOCK_PROPORTION;

        System.out.println(width + ", " + height);
        System.out.println(blockSideLen*columnsCount + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(columnsCount-1) + ", "
                + blockSideLen*rowsCount + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(rowsCount-1));

        gc = getGraphicsContext2D();

        for(int x = 0; x < columnsCount; x++)
            for(int y = 0; y < rowsCount; y++)
                statMatrix[x][y] = CellStat.EMPTY;
    }

    public CellStat[][] getStatMatrix() {
        return statMatrix;
    }

    private boolean full = false;

    private Block danglingBlock;
    public int blockLeft, blockTop; // danglingBlock 的坐标

    public ConflictType setDanglingBlock(int left, int top, Block block) {
        ConflictType type = testBoundAndConflict(left, top, block);
        if (type == ConflictType.NONE_CONFLICT) {
            this.blockLeft = left;
            this.blockTop = top;
            danglingBlock = block;
        }
        return type;
    }

    public Block getDanglingBlock() {
        return danglingBlock;
    }

    public boolean isFull() {
        return full;
    }

    public boolean moveLeft() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(blockLeft - 1, blockTop, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            blockLeft--;
            draw();
            return true;
        }
        return false;
    }

    public boolean moveRight() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(blockLeft + 1, blockTop, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            blockLeft++;
            draw();
            return true;
        }
        return false;
    }

    public boolean transform() {
        assert danglingBlock != null;

        danglingBlock.switchToNextStat();
        ConflictType type = testBoundAndConflict(blockLeft, blockTop, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            draw();
            return true;
        }
        danglingBlock.switchToPrevStat();
        return false;
    }

    public boolean moveDown() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(blockLeft, blockTop + 1, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            blockTop++;
            draw();
            return true;
        }
        return false;
    }

    public boolean tryMoveDown() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(blockLeft, blockTop + 1, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            blockTop++;
            return true;
        }
        return false;
    }

    /**
     * 消除满行。
     * 先将panel的所有未满行复制到一个临时的数组中，
     * 在将此临时数组复制的panel中
     * @return 移除的行数
     */
    private int removeFullLines() {
        int notFullLineCount = 0;

        CellStat[][] tmp = new CellStat[columnsCount][rowsCount];
        for (int x = 0; x < columnsCount; x++)
            for (int y = 0; y < rowsCount; y++)
                tmp[x][y] = CellStat.EMPTY;

        int j = rowsCount - 1;

        for (int y = rowsCount - 1; y >= 0; y--) {
            for (int x = 0; x < columnsCount; x++) {
                if (statMatrix[x][y] == CellStat.EMPTY) {
                    // 发现一未满行，将此行复制的tmp数组的对应位置
                    for (int i = 0, t = 0; i < columnsCount; i++, t++) {
                        tmp[i][j] = statMatrix[t][y];
                    }
                    j--;
                    notFullLineCount++;
                    break;
                }
            }
        }

        for (int x = 0; x < columnsCount; x++) {
            for (int y = 0; y < rowsCount; y++) {
                statMatrix[x][y] = tmp[x][y];
            }
        }

        //draw();
        return rowsCount - notFullLineCount;
    }

    public int merger() {
        assert danglingBlock != null;
        assert testBoundAndConflict(blockLeft, blockTop, danglingBlock) == ConflictType.NONE_CONFLICT;

        for(int x = 0; x < Block.SIDE_LEN; x++) {
            for(int y = 0; y < Block.SIDE_LEN; y++) {
                if(danglingBlock.getData()[x][y]) {
                    // 上方屏幕外图形的不合并
                    if (blockTop + y < 0) {
                        full = true;
                    } else {
                        statMatrix[blockLeft + x][blockTop + y] = CellStat.SOLIDIFY;
                    }
                }
            }
        }

        int removedLinesCount = removeFullLines();
        if (removedLinesCount > 0 && blockTop < 0) {
            blockTop += removedLinesCount; // 下移 removedLinesCount 行
            if (blockTop > 0) {
                full = false;

                for(int x = 0; x < Block.SIDE_LEN; x++)
                    for(int y = 0; y < removedLinesCount; y++)
                        if(danglingBlock.getData()[x][y])
                            statMatrix[blockLeft + x][blockTop + y] = CellStat.SOLIDIFY;
            }
        }

        danglingBlock = null;
        draw();
        return removedLinesCount;
    }

    public void pasteDanglingBlock() {
        assert danglingBlock != null;
        assert testBoundAndConflict(blockLeft, blockTop, danglingBlock) == ConflictType.NONE_CONFLICT;

        for(int x = 0; x < Block.SIDE_LEN; x++) {
            for(int y = 0; y < Block.SIDE_LEN; y++) {
                // 上方屏幕外图形的不合并
                if(danglingBlock.getData()[x][y] && blockTop + y >= 0)
                    statMatrix[blockLeft + x][blockTop + y] = CellStat.MOVING;
            }
        }
    }

    public void unPasteDanglingBlock() {
        for(int x = 0; x < columnsCount; x++) {
            for(int y = 0; y < rowsCount; y++) {
                if(statMatrix[x][y] == CellStat.MOVING)
                    statMatrix[x][y] = CellStat.EMPTY;
            }
        }
    }

    public enum ConflictType {
        NONE_CONFLICT,  // 无冲突
        CONFLICT,       // 冲突
        OUT_OF_LEFT_BOUND,  // 左越界
        OUT_OF_RIGHT_BOUND,
        OUT_OF_BOTTOM_BOUND,
    }

    /**
     * 检测是否冲突
     * @param left 面板中的x坐标
     * @param top  面板中的y坐标
     * @param block 需要检测的小方块
     * @return 是否冲突
     */
    public ConflictType testBoundAndConflict(int left, int top, Block block) {
        boolean[][] data = block.getData();

        for (int x = 0; x < Block.SIDE_LEN; x++) {
            for (int y = 0; y < Block.SIDE_LEN; y++) {
                if (data[x][y]) {
                    int i = left + x;
                    int j = top + y;

                    if(i < 0)
                        return ConflictType.OUT_OF_LEFT_BOUND;
                    if(i >= columnsCount)
                        return ConflictType.OUT_OF_RIGHT_BOUND;
                    if(j >= rowsCount)
                        return ConflictType.OUT_OF_BOTTOM_BOUND;

                    // 小方块从顶部刚出来时是可以显示不全的，所以（j<0）不算越界
                    if(j >= 0 && statMatrix[i][j] != CellStat.EMPTY) {
                        return ConflictType.CONFLICT;
                    }
                }
            }
        }

        return ConflictType.NONE_CONFLICT;
    }

    public void draw() {
        // 将更新界面的工作交给 FX application thread 执行
        Platform.runLater(() -> {
            double lineWidth = 1;
            gc.setLineWidth(lineWidth);

            for (int x = 0; x < columnsCount; x++) {
                for (int y = 0; y < rowsCount; y++) {
                    if ((danglingBlock != null)
                            && (blockLeft <= x) && (x < blockLeft + Block.SIDE_LEN)
                            && (blockTop <= y) && (y < blockTop + Block.SIDE_LEN)
                            && (danglingBlock.getData()[x - blockLeft][y - blockTop])) {
                        gc.setFill(Color.BLACK);
                        gc.setStroke(Color.BLACK);
                    } else {
                        if (statMatrix[x][y] == CellStat.EMPTY) {
                            gc.setFill(Color.GRAY);
                            gc.setStroke(Color.GRAY);
                        } else if (statMatrix[x][y] == CellStat.SOLIDIFY) {
                            gc.setFill(Color.BLACK);
                            gc.setStroke(Color.BLACK);
                        }
                    }

                    double x0 = x * blockSideLen + x * gapBetweenBlocks;
                    double y0 = y * blockSideLen + y * gapBetweenBlocks;
                    gc.strokeRect(x0, y0, blockSideLen, blockSideLen);
                    x0 += gapInnerBlock;
                    y0 += gapInnerBlock;
                    gc.fillRect(x0, y0, blockSideLen - 2 * gapInnerBlock, blockSideLen - 2 * gapInnerBlock);
                }
            }
        });
    }

//    /**
//     * 设置面板显示"over"
//     */
//    public void overPattern() {
//        for (int y = rowsCount - 1; y >= 0; y--) {
//            for (int x = 0; x < columnsCount; x++) {
//                statMatrix[x][y] = CellStat.EMPTY;
//            }
//        }
//
//        // "O"
//        statMatrix[1][3] = CellStat.SOLIDIFY;
//        statMatrix[1][4] = CellStat.SOLIDIFY;
//        statMatrix[1][5] = CellStat.SOLIDIFY;
//        statMatrix[1][6] = CellStat.SOLIDIFY;
//        statMatrix[1][7] = CellStat.SOLIDIFY;
//
//        statMatrix[2][3] = CellStat.SOLIDIFY;
//        statMatrix[2][7] = CellStat.SOLIDIFY;
//
//        statMatrix[3][3] = CellStat.SOLIDIFY;
//        statMatrix[3][4] = CellStat.SOLIDIFY;
//        statMatrix[3][5] = CellStat.SOLIDIFY;
//        statMatrix[3][6] = CellStat.SOLIDIFY;
//        statMatrix[3][7] = CellStat.SOLIDIFY;
//
//        // "V"
//        statMatrix[5][3] = CellStat.SOLIDIFY;
//        statMatrix[5][4] = CellStat.SOLIDIFY;
//        statMatrix[5][5] = CellStat.SOLIDIFY;
//        statMatrix[5][6] = CellStat.SOLIDIFY;
//
//        statMatrix[6][7] = CellStat.SOLIDIFY;
//
//        statMatrix[7][3] = CellStat.SOLIDIFY;
//        statMatrix[7][4] = CellStat.SOLIDIFY;
//        statMatrix[7][5] = CellStat.SOLIDIFY;
//        statMatrix[7][6] = CellStat.SOLIDIFY;
//
//        // "E"
//        statMatrix[1][9] = CellStat.SOLIDIFY;
//        statMatrix[1][10] = CellStat.SOLIDIFY;
//        statMatrix[1][11] = CellStat.SOLIDIFY;
//        statMatrix[1][12] = CellStat.SOLIDIFY;
//        statMatrix[1][13] = CellStat.SOLIDIFY;
//
//        statMatrix[2][9] = CellStat.SOLIDIFY;
//        statMatrix[3][9] = CellStat.SOLIDIFY;
//
//        statMatrix[2][11] = CellStat.SOLIDIFY;
//        statMatrix[3][11] = CellStat.SOLIDIFY;
//
//        statMatrix[2][13] = CellStat.SOLIDIFY;
//        statMatrix[3][13] = CellStat.SOLIDIFY;
//
//        // "R"
//        statMatrix[5][9] = CellStat.SOLIDIFY;
//        statMatrix[5][10] = CellStat.SOLIDIFY;
//        statMatrix[5][11] = CellStat.SOLIDIFY;
//        statMatrix[5][12] = CellStat.SOLIDIFY;
//        statMatrix[5][13] = CellStat.SOLIDIFY;
//
//        statMatrix[6][9] = CellStat.SOLIDIFY;
//        statMatrix[7][9] = CellStat.SOLIDIFY;
//
//        statMatrix[7][10] = CellStat.SOLIDIFY;
//        statMatrix[7][11] = CellStat.SOLIDIFY;
//
//        statMatrix[6][11] = CellStat.SOLIDIFY;
//
//        statMatrix[6][12] = CellStat.SOLIDIFY;
//        statMatrix[7][13] = CellStat.SOLIDIFY;
//    }
    
}
