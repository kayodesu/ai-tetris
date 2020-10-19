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

    private static final int ROW = 20;   // 行数
    private static final int COL = 10;   // 列数

    private GraphicsContext gc;

    // 画布中每个小方块的状态
    public enum CellStat {
        EMPTY,     // 空
        MOVING,    // 正在移动的痕迹
        SOLIDIFY   // 已经固定存在的小方块
    }

    private double width;
    private double height;

    private double blockSideLen;
    private double gapBetweenBlocks;
    private double gapInnerBlock;

    // 画布状态表示
    private static final CellStat[][] statMatrix = new CellStat[COL][ROW];

    public Container(double width, double height) {
        super(width, height);

        this.width = width;
        this.height = height;
        // blockSideLen*COL + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(COL-1) = width
        var len0 = width / (COL + GAP_BETWEEN_BLOCKS_PROPORTION*(COL-1));
        var len1 = height / (ROW + GAP_BETWEEN_BLOCKS_PROPORTION*(ROW-1));
//        System.out.println("xx, yy " + xx + ", " + yy);
        blockSideLen = Math.min(len0, len1);
        gapBetweenBlocks = blockSideLen * GAP_BETWEEN_BLOCKS_PROPORTION;
        gapInnerBlock = blockSideLen * GAP_INNER_BLOCK_PROPORTION;

        System.out.println(width + ", " + height);
        System.out.println(blockSideLen*COL + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(COL-1) + ", "
                + blockSideLen*ROW + GAP_BETWEEN_BLOCKS_PROPORTION*blockSideLen*(ROW-1));

        gc = getGraphicsContext2D();

        for(int x = 0; x < COL; x++)
            for(int y = 0; y < ROW; y++)
                statMatrix[x][y] = CellStat.EMPTY;
    }

    void draw() {
        double lineWidth = 1;
        gc.setLineWidth(lineWidth);
//        gc.strokeLine(40, 10, 10, 40);

        for(int x = 0; x < COL; x++) {
            for (int y = 0; y < ROW; y++) {
                if (statMatrix[x][y] == CellStat.EMPTY) {
                    gc.setFill(Color.GRAY);
                    gc.setStroke(Color.GRAY);
                } else if (statMatrix[x][y] == CellStat.MOVING || statMatrix[x][y] == CellStat.SOLIDIFY) {
                    gc.setFill(Color.BLACK);
                    gc.setStroke(Color.BLACK);
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
     * 检测是否冲突
     * @param left 面板中的x坐标
     * @param top  面板中的y坐标
     * @param block 需要检测的小方块
     * @return 是否冲突
     */
    public boolean testConflict(int left, int top, Block block) {
        boolean[][] data = block.getData();

        for (int x = 0; x < Block.SIDE_LEN; x++) {
            for (int y = 0; y < Block.SIDE_LEN; y++) {
                if (data[x][y]) {
                    int i = left + x;
                    int j = top + y;

                    if(i < 0 || i >= COL) // 超出左或右边界
                        return true;
                    if(j >= ROW) // 超出下边界
                        return true;

                    // 小方块从顶部刚出来时是可以显示不全的，所以（j<0）不算越界
                    if(j >= 0 && statMatrix[i][j] == CellStat.SOLIDIFY) {
                        return true;
                    }
                }
            }
        }

        return false; // 没冲突
    }

    /**
     * 将一个block的数据贴到面板上
     * @param left
     * @param top
     * @param block
     */
    public void pasteBlock(int left, int top, Block block) {
        boolean[][] data = block.getData();

        for (int x = 0; x < Block.SIDE_LEN; x++) {
            for (int y = 0; y < Block.SIDE_LEN; y++) {
                if (data[x][y] && top + y >= 0) {
                    statMatrix[left + x][top + y] = CellStat.MOVING;
                }
            }
        }
    }

    public void cleanAllBlocksMovingStat() {
        for(int x = 0; x < COL; x++) {
            for(int y = 0; y < ROW; y++) {
                if(statMatrix[x][y] == CellStat.MOVING)
                    statMatrix[x][y] = CellStat.EMPTY;
            }
        }
    }

    public void solidifyAllBlocksMovingStat() {
        for(int x = 0; x < COL; x++) {
            for(int y = 0; y < ROW; y++) {
                if(statMatrix[x][y] == CellStat.MOVING)
                    statMatrix[x][y] = CellStat.SOLIDIFY;
            }
        }
    }

    /**
     * 消除满行。
     * 先将panel的所有未满行复制到一个临时的数组中，
     * 在将此临时数组复制的panel中
     * @return 移除的行数
     */
    public int removeFullLines() {

        int notFullLineCount = 0;

        CellStat[][] tmp = new CellStat[COL][ROW];
        for (int x = 0; x < COL; x++)
            for (int y = 0; y < ROW; y++)
                tmp[x][y] = CellStat.EMPTY;

        int j = ROW - 1;

        for (int y = ROW - 1; y >= 0; y--) {
            for (int x = 0; x < COL; x++) {
                if (statMatrix[x][y] == CellStat.EMPTY) {
                    // 发现一未满行，将此行复制的tmp数组的对应位置
                    for (int i = 0, t = 0; i < COL; i++, t++) {
                        tmp[i][j] = statMatrix[t][y];
                    }
                    j--;
                    notFullLineCount++;
                    break;
                }
            }
        }

        for (int x = 0; x < COL; x++) {
            for (int y = 0; y < ROW; y++) {
                statMatrix[x][y] = tmp[x][y];
            }
        }

        return ROW - notFullLineCount;
    }

    /**
     * 模拟将一个block放置在底部，从左到右依次放置
     * @param block
     * @return 是否可以继续模拟放置
     */
    public boolean simulatePut(Block block) {
        for (int x = -Block.SIDE_LEN + 1; true; x++) {
            for (int y = -Block.SIDE_LEN; true; y++) {
                boolean b = testConflict(x, y, block);
            }
        }
    }

    /**
     * 设置面板显示"over"
     */
    public void overPattern() {
        for (int y = ROW - 1; y >= 0; y--) {
            for (int x = 0; x < COL; x++) {
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
