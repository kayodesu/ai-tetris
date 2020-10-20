package io.github.keyodesu;

import io.github.keyodesu.block.Block;
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

//    private static final int ROW = 20;   // 行数
//    private static final int COL = 10;   // 列数

    private GraphicsContext gc;

    // 画布中每个小方块的状态
    public enum CellStat {
        EMPTY,     // 空
        MOVING,    // 正在移动的痕迹
        SOLIDIFY   // 已经固定存在的小方块
    }

//    private double width;
//    private double height;

    private double blockSideLen;
    private double gapBetweenBlocks;
    private double gapInnerBlock;

    // 画布状态表示
    private CellStat[][] statMatrix;
    private int col, row;

    public Container(double width, double height, int col, int row) {
        super(width, height);

        this.col = col;
        this.row = row;
        statMatrix = new CellStat[col][row];

//        this.width = width;
//        this.height = height;

        // blockSideLen*COL + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(COL-1) = width
        var len0 = width / (col + GAP_BETWEEN_BLOCKS_PROPORTION*(col-1));
        var len1 = height / (row + GAP_BETWEEN_BLOCKS_PROPORTION*(row-1));
//        System.out.println("xx, yy " + xx + ", " + yy);
        blockSideLen = Math.min(len0, len1);
        gapBetweenBlocks = blockSideLen * GAP_BETWEEN_BLOCKS_PROPORTION;
        gapInnerBlock = blockSideLen * GAP_INNER_BLOCK_PROPORTION;

        System.out.println(width + ", " + height);
        System.out.println(blockSideLen*col + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(col-1) + ", "
                + blockSideLen*row + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(row-1));

        gc = getGraphicsContext2D();

        for(int x = 0; x < col; x++)
            for(int y = 0; y < row; y++)
                statMatrix[x][y] = CellStat.EMPTY;
    }

    public CellStat[][] getStatMatrix() {
        return statMatrix;
    }

    private boolean full = false;

    private Block danglingBlock;
    private int left, top;

    public ConflictType setDanglingBlock(int left, int top, Block block) {
        ConflictType type = testBoundAndConflict(left, top, block);
        if (type == ConflictType.NONE_CONFLICT) {
            this.left = left;
            this.top = top;
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

        ConflictType type = testBoundAndConflict(left - 1, top, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            left--;
            draw();
            return true;
        }
        return false;
    }

    public boolean moveRight() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(left + 1, top, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            left++;
            draw();
            return true;
        }
        return false;
    }

    public boolean transform() {
        assert danglingBlock != null;

        danglingBlock.switchToNextStat();
        ConflictType type = testBoundAndConflict(left, top, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            draw();
            return true;
        }
        danglingBlock.switchToPrevStat();
        return false;
    }

    public boolean moveDown() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(left, top + 1, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            top++;
            draw();
            return true;
        }

        if (top < 0) {
            full = true;
        }
        return false;
    }

    public boolean tryMoveDown() {
        assert danglingBlock != null;

        ConflictType type = testBoundAndConflict(left, top + 1, danglingBlock);
        if (type == ConflictType.NONE_CONFLICT) {
            top++;
            return true;
        }
        return false;
    }

    public void merger() {
        assert danglingBlock != null;
        assert testBoundAndConflict(left, top, danglingBlock) == ConflictType.NONE_CONFLICT;

        for(int x = 0; x < Block.SIDE_LEN; x++) {
            for(int y = 0; y < Block.SIDE_LEN; y++) {
                // 上方屏幕外图形的不合并
                if(top + y > 0 && danglingBlock.getData()[x][y])
                    statMatrix[left + x][top + y] = CellStat.SOLIDIFY;
            }
        }

        danglingBlock = null;
    }

    public void danglingMerger() {
        assert danglingBlock != null;
        assert testBoundAndConflict(left, top, danglingBlock) == ConflictType.NONE_CONFLICT;

        for(int x = 0; x < Block.SIDE_LEN; x++) {
            for(int y = 0; y < Block.SIDE_LEN; y++) {
                // 上方屏幕外图形的不合并
                if(top + y > 0 && danglingBlock.getData()[x][y])
                    statMatrix[left + x][top + y] = CellStat.MOVING;
            }
        }
    }

    public void undoDanglingMerger() {
        for(int x = 0; x < col; x++) {
            for(int y = 0; y < row; y++) {
                if(statMatrix[x][y] == CellStat.MOVING)
                    statMatrix[x][y] = CellStat.EMPTY;
            }
        }
    }


//    public static class BlockOutOfLeftBoundException extends Exception { }
//    public static class BlockOutOfRightBoundException extends Exception { }
//    public static class BlockOutOfBottomBoundException extends Exception { }
//    public static class BlockConflictException extends Exception { }

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
                    if(i >= col)
                        return ConflictType.OUT_OF_RIGHT_BOUND;
                    if(j >= row)
                        return ConflictType.OUT_OF_BOTTOM_BOUND;

                    // 小方块从顶部刚出来时是可以显示不全的，所以（j<0）不算越界
                    if(j >= 0 && statMatrix[i][j] == CellStat.SOLIDIFY) {
                        return ConflictType.CONFLICT;
                    }
                }
            }
        }

        return ConflictType.NONE_CONFLICT;
    }

//    /**
//     * 将一个block的数据贴到container中，只可以贴到 empty cell上
//     * 上面可以越界，左，右，下不可以越界
//     * @param left
//     * @param top
//     * @param block
//     */
//    public ConflictType pasteBlock(int left, int top, Block block) {
//        ConflictType type = testBoundAndConflict(left, top, block);
//        if (type == ConflictType.NONE_CONFLICT) {
//            boolean[][] data = block.getData();
//
//            for (int x = 0; x < Block.SIDE_LEN; x++) {
//                for (int y = 0; y < Block.SIDE_LEN; y++) {
//                    if (data[x][y] && top + y >= 0) {
//                        statMatrix[left + x][top + y] = CellStat.MOVING;
//                    }
//                }
//            }
//        }
//        return type;
//    }

//    public ConflictType pasteBlockBottom(int column, Block block) {
//        for (int r = -Block.SIDE_LEN; r < row; r++) {
//            ConflictType type = pasteBlock(column, r, block);
//            if (type == ConflictType.OUT_OF_LEFT_BOUND || type == ConflictType.OUT_OF_RIGHT_BOUND)
//                return type;
//            else if (type == ConflictType.NONE_CONFLICT) {
//                cleanAllBlocksMovingStat();
//            } else { // 底部冲突了
//                cleanAllBlocksMovingStat();
//                r--; // 底部冲突了，回退
//                pasteBlock(column, r, block); // 贴到底部
//                return ConflictType.NONE_CONFLICT;
//            }
//        }
//
//        throw new NeverReachHereError();
//    }

//    public void cleanAllBlocksMovingStat() {
//        for(int x = 0; x < col; x++) {
//            for(int y = 0; y < row; y++) {
//                if(statMatrix[x][y] == CellStat.MOVING)
//                    statMatrix[x][y] = CellStat.EMPTY;
//            }
//        }
//    }

//    public void solidifyAllBlocksMovingStat() {
//        for(int x = 0; x < col; x++) {
//            for(int y = 0; y < row; y++) {
//                if(statMatrix[x][y] == CellStat.MOVING)
//                    statMatrix[x][y] = CellStat.SOLIDIFY;
//            }
//        }
//    }

    /**
     * 消除满行。
     * 先将panel的所有未满行复制到一个临时的数组中，
     * 在将此临时数组复制的panel中
     * @return 移除的行数
     */
    public int removeFullLines() {
        int notFullLineCount = 0;

        CellStat[][] tmp = new CellStat[col][row];
        for (int x = 0; x < col; x++)
            for (int y = 0; y < row; y++)
                tmp[x][y] = CellStat.EMPTY;

        int j = row - 1;

        for (int y = row - 1; y >= 0; y--) {
            for (int x = 0; x < col; x++) {
                if (statMatrix[x][y] == CellStat.EMPTY) {
                    // 发现一未满行，将此行复制的tmp数组的对应位置
                    for (int i = 0, t = 0; i < col; i++, t++) {
                        tmp[i][j] = statMatrix[t][y];
                    }
                    j--;
                    notFullLineCount++;
                    break;
                }
            }
        }

        for (int x = 0; x < col; x++) {
            for (int y = 0; y < row; y++) {
                statMatrix[x][y] = tmp[x][y];
            }
        }

        draw();
        return row - notFullLineCount;
    }

//    /**
//     * 模拟将一个block放置在底部，从左到右依次放置
//     * @param block
//     * @return 是否可以继续模拟放置
//     */
//    public boolean simulatePut(Block block) {
//        for (int x = -Block.SIDE_LEN + 1; true; x++) {
//            for (int y = -Block.SIDE_LEN; true; y++) {
//                boolean b = testConflict(x, y, block);
//            }
//        }
//    }

    public void draw() {
        double lineWidth = 1;
        gc.setLineWidth(lineWidth);

        for(int x = 0; x < col; x++) {
            for (int y = 0; y < row; y++) {
                if (danglingBlock != null
                        && x >= left && x < left + Block.SIDE_LEN
                        && y >= top && y < top + Block.SIDE_LEN
                        && danglingBlock.getData()[x-left][y-top]) {
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

                double x0 = x*blockSideLen + x*gapBetweenBlocks;
                double y0 = y*blockSideLen + y*gapBetweenBlocks;
                gc.strokeRect(x0, y0, blockSideLen, blockSideLen);
                x0 += gapInnerBlock;
                y0 += gapInnerBlock;
                gc.fillRect(x0, y0, blockSideLen - 2*gapInnerBlock, blockSideLen - 2*gapInnerBlock);
            }
        }
    }

    /**
     * 设置面板显示"over"
     */
    public void overPattern() {
        for (int y = row - 1; y >= 0; y--) {
            for (int x = 0; x < col; x++) {
                statMatrix[x][y] = CellStat.EMPTY;
            }
        }

        // "O"
        statMatrix[1][3] = CellStat.SOLIDIFY;
        statMatrix[1][4] = CellStat.SOLIDIFY;
        statMatrix[1][5] = CellStat.SOLIDIFY;
        statMatrix[1][6] = CellStat.SOLIDIFY;
        statMatrix[1][7] = CellStat.SOLIDIFY;

        statMatrix[2][3] = CellStat.SOLIDIFY;
        statMatrix[2][7] = CellStat.SOLIDIFY;

        statMatrix[3][3] = CellStat.SOLIDIFY;
        statMatrix[3][4] = CellStat.SOLIDIFY;
        statMatrix[3][5] = CellStat.SOLIDIFY;
        statMatrix[3][6] = CellStat.SOLIDIFY;
        statMatrix[3][7] = CellStat.SOLIDIFY;

        // "V"
        statMatrix[5][3] = CellStat.SOLIDIFY;
        statMatrix[5][4] = CellStat.SOLIDIFY;
        statMatrix[5][5] = CellStat.SOLIDIFY;
        statMatrix[5][6] = CellStat.SOLIDIFY;

        statMatrix[6][7] = CellStat.SOLIDIFY;

        statMatrix[7][3] = CellStat.SOLIDIFY;
        statMatrix[7][4] = CellStat.SOLIDIFY;
        statMatrix[7][5] = CellStat.SOLIDIFY;
        statMatrix[7][6] = CellStat.SOLIDIFY;

        // "E"
        statMatrix[1][9] = CellStat.SOLIDIFY;
        statMatrix[1][10] = CellStat.SOLIDIFY;
        statMatrix[1][11] = CellStat.SOLIDIFY;
        statMatrix[1][12] = CellStat.SOLIDIFY;
        statMatrix[1][13] = CellStat.SOLIDIFY;

        statMatrix[2][9] = CellStat.SOLIDIFY;
        statMatrix[3][9] = CellStat.SOLIDIFY;

        statMatrix[2][11] = CellStat.SOLIDIFY;
        statMatrix[3][11] = CellStat.SOLIDIFY;

        statMatrix[2][13] = CellStat.SOLIDIFY;
        statMatrix[3][13] = CellStat.SOLIDIFY;

        // "R"
        statMatrix[5][9] = CellStat.SOLIDIFY;
        statMatrix[5][10] = CellStat.SOLIDIFY;
        statMatrix[5][11] = CellStat.SOLIDIFY;
        statMatrix[5][12] = CellStat.SOLIDIFY;
        statMatrix[5][13] = CellStat.SOLIDIFY;

        statMatrix[6][9] = CellStat.SOLIDIFY;
        statMatrix[7][9] = CellStat.SOLIDIFY;

        statMatrix[7][10] = CellStat.SOLIDIFY;
        statMatrix[7][11] = CellStat.SOLIDIFY;

        statMatrix[6][11] = CellStat.SOLIDIFY;

        statMatrix[6][12] = CellStat.SOLIDIFY;
        statMatrix[7][13] = CellStat.SOLIDIFY;
    }
    
}
