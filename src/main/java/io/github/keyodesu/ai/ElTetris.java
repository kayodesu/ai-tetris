package io.github.keyodesu.ai;

import io.github.keyodesu.Config;
import io.github.keyodesu.Container;
import io.github.keyodesu.block.Block;

import java.util.stream.IntStream;

import static io.github.keyodesu.Container.CellStat.*;
import static io.github.keyodesu.Container.ConflictType.NONE_CONFLICT;

/**
 * @author Yo Ka
 */
public class ElTetris implements AI {
    private Container container;

    public ElTetris(Container container) {
        this.container = container;
    }

    // weights
    private static final double LANDING_HEIGHT_WEIGHT     = -4.500158825082766;
    private static final double ROWS_ELIMINATED_WEIGHT    = +3.4181268101392694;
    private static final double ROW_TRANSITIONS_WEIGHT    = -3.2178882868487753;
    private static final double COLUMN_TRANSITIONS_WEIGHT = -9.348695305445199;
    private static final double NUMBER_OF_HOLES_WEIGHT    = -7.899265427351652;
    private static final double WELL_SUMS_WEIGHT          = -3.3855972247263626;

    /**
     * 1. Landing Height: The height where the piece is put
     * (= the height of the column + (the height of the piece / 2))
     */
    private static int landingHeight(Container container) {
//        int row = statMatrix[0].length;
//        for (int y = 0; y < row; y++) {
//            if (statMatrix[col][y] != EMPTY)
//                return row - y + blockHeight/2;
//        }
//
//        return blockHeight/2;
        return container.getRowsCount() - container.top + container.getDanglingBlock().getHeight()/2;
    }

    /**
     * 2. Rows eliminated: The number of rows eliminated.
     */
    private static int rowsEliminated(Container.CellStat[][] statMatrix) {
        int num = 0;

        int col = statMatrix.length;
        int row = statMatrix[0].length;
        for (int y = 0; y < row; y++) {
            int x;
            for (x = 0; x < col; x++) {
                if (statMatrix[x][y] == EMPTY)
                    break;
            }
            if (x == col) { // find a full row
                num++;
            }
        }

        return num;
    }

    /**
     * 3. Row Transitions: The total number of row transitions.
     * A row transition occurs when an empty cell is adjacent to a filled cell
     * on the same row and vice versa.
     * 左右边界视为filled cell
     */
    private static int rowTransitions(Container.CellStat[][] statMatrix) {
        int transitions = 0;

        int col = statMatrix.length;
        int row = statMatrix[0].length;

        for (int y = 0; y < row; y++) {
            var lastStat = SOLIDIFY;

            for (var column : statMatrix) {
                if (column[y] != lastStat) {
                    lastStat = column[y];
                    transitions++;
                }
            }
            if (statMatrix[col-1][y] == EMPTY)
                transitions++;
        }

        return transitions;
    }

    /**
     * 4. Column Transitions: The total number of column transitions.
     * A column transition occurs when an empty cell is adjacent to a filled cell
     * on the same column and vice versa.
     * 上下边界视为filled cell
     */
    private static int columnTransitions(Container.CellStat[][] statMatrix) {
        int transitions = 0;

        for (var column : statMatrix) {
            var lastStat = SOLIDIFY;

            for (var stat : column) {
                if (stat != lastStat) {
                    transitions++;
                    lastStat = stat;
                }
            }
            if (column[column.length-1] == EMPTY)
                transitions++;
        }

        return transitions;
    }

    /**
     * 5. Number of Holes: A hole is an empty cell
     * that has at least one filled cell above it in the same column.
     */
    private static int numberOfHoles(Container.CellStat[][] statMatrix) {
        int num = 0;

        for (var column : statMatrix) {
            boolean filledCellAbove = false;
            for (var stat : column) {
                if (stat != EMPTY) {
                    filledCellAbove = true;
                } else if (filledCellAbove) {
                    num++;
                }
            }
        }

        return num;
    }


    /**
     * 6. Well Sums: A well is a succession of empty cells
     * such that their left cells and right cells are both filled.
     */
    private static int wellSums(Container.CellStat[][] statMatrix) {
        int sums = 0;
        int num = 0;

        for (int x = 0; x < statMatrix.length; x++) {
            for (int y = 0; y < statMatrix[x].length; y++) {
                if ((statMatrix[x][y] == EMPTY)
                        && (x-1 < 0 || statMatrix[x-1][y] != EMPTY)
                        && (x+1 >= statMatrix.length || statMatrix[x+1][y] != EMPTY)) {
                    num++;
                } else if (num > 0) {
                    sums += IntStream.rangeClosed(0, num).sum();
                    num = 0;
                }
            }
        }

        sums += IntStream.rangeClosed(0, num).sum();
        return sums;
    }

    private static double evaluateScore(Container container) {
        Container.CellStat[][] statMatrix = container.getStatMatrix();
        return landingHeight(container) * LANDING_HEIGHT_WEIGHT
                + rowsEliminated(statMatrix)*ROWS_ELIMINATED_WEIGHT
                + rowTransitions(statMatrix)*ROW_TRANSITIONS_WEIGHT
                + columnTransitions(statMatrix)*COLUMN_TRANSITIONS_WEIGHT
                + numberOfHoles(statMatrix)*NUMBER_OF_HOLES_WEIGHT
                + wellSums(statMatrix)*WELL_SUMS_WEIGHT;
    }

    public void calBestColAndStat() {
        Block block = container.getDanglingBlock();
        assert block != null;

        double maxScore = Double.NEGATIVE_INFINITY;
        int col = Integer.MIN_VALUE;
        int blockStat = -1;

        for (int i = block.getStatsCount(); i > 0; i--) {
            for (int x = -Block.SIDE_LEN + 1; x < Config.COL; x++) {
                Container.ConflictType type = container.setDanglingBlock(x, -Block.SIDE_LEN, block);
                if (type != NONE_CONFLICT) {
                    continue;
                }

                while (container.tryMoveDown()) {
//                    try {
//                        Thread.sleep(1); /////////////////////////////////////////////////////
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }

//                if (container.isFull()) {
//                    continue;
//                }

                // block 已经悬停在底部了
                container.pasteDanglingBlock();
                double score = evaluateScore(container);
                container.unPasteDanglingBlock();

                if (score > maxScore) {
                    maxScore = score;
                    col = x;
                    blockStat = block.getStat();
                }
            }

            block.switchToNextStat();
        }

        assert blockStat > 0;
        block.switchToStat(blockStat);
//         System.out.println("col: " + col); ////////////////////////////////////////////////////////////////////////////
        container.setDanglingBlock(col, -Block.SIDE_LEN, block);
    }

    public void stop() {

    }
}
