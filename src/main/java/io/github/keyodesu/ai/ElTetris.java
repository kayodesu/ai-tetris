package io.github.keyodesu.ai;

import io.github.keyodesu.Container;
import io.github.keyodesu.block.Block;

import java.util.function.Function;
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
    private static double landingHeight(Container container) {
        int blockHeight = container.getDanglingBlock().getHeight();
        int blockTopSpace = Block.SIDE_LEN - blockHeight;

        int columnHeight = container.getRowsCount() - (container.blockTop + blockTopSpace);
        if (columnHeight > container.getRowsCount())
            columnHeight = 10000; // todo

        return columnHeight + blockHeight/2.0;
    }

    /**
     * 2. Rows eliminated: The number of rows eliminated.
     */
    private static int rowsEliminated(Container.Cell[][] cellMatrix) {
        int num = 0;

        int col = cellMatrix.length;
        int row = cellMatrix[0].length;
        for (int y = 0; y < row; y++) {
            int x;
            for (x = 0; x < col; x++) {
                if (cellMatrix[x][y].stat == EMPTY)
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
    private static int rowTransitions(Container.Cell[][] cellMatrix) {
        int transitions = 0;

        int col = cellMatrix.length;
        int row = cellMatrix[0].length;

        for (int y = 0; y < row; y++) {
            var lastStat = SOLIDIFY;

            for (var column : cellMatrix) {
                if (column[y].stat != lastStat) {
                    lastStat = column[y].stat;
                    transitions++;
                }
            }
            if (cellMatrix[col-1][y].stat == EMPTY)
                transitions++;
        }

        return transitions;
    }

    /**
     * 4. Column Transitions: The total number of column transitions.
     * A column transition occurs when an empty cell is adjacent to a filled cell
     * on the same column and vice versa.
     * 上边界视为empty cell，下边界视为filled cell
     */
    private static int columnTransitions(Container.Cell[][] cellMatrix) {
        int transitions = 0;

        for (var column : cellMatrix) {
            var lastStat = EMPTY;

            for (var cell : column) {
                if (cell.stat != lastStat) {
                    transitions++;
                    lastStat = cell.stat;
                }
            }
            if (column[column.length-1].stat == EMPTY)
                transitions++;
        }

        return transitions;
    }

    /**
     * 5. Number of Holes: A hole is an empty cell
     * that has at least one filled cell above it in the same column.
     */
    private static int numberOfHoles(Container.Cell[][] cellMatrix) {
        int num = 0; // number of holes

        for (var column : cellMatrix) {
            boolean filledCellAbove = false;
            for (var cell : column) {
                if (cell.stat != EMPTY) {
                    filledCellAbove = true;
                } else if (filledCellAbove) {
                    num++;
                }
            }
        }

        return num;
    }

    /**
     * 6. Well Sums: A well is a sequence of empty cells above the top piece in a column
     * such that the top cell in the sequence is surrounded (left and right)
     * by occupied cells or a boundary of the board.
     *
     * @return :
     *    The well sums. For a well of length n, we define the well sums as
     *    1 + 2 + 3 + ... + n. This gives more significance to deeper holes.
     */
    private static int wellSums(Container.Cell[][] cellMatrix) {
        int sums = 0;

        for (int x = 0; x < cellMatrix.length; x++) {
            int deep = 0;
            boolean inWell = false;

            for (int y = 0; y < cellMatrix[x].length; y++) {
                if (cellMatrix[x][y].stat != EMPTY)
                    break; // 触底了
                if (!inWell) {
                    if (((x-1 < 0) || (cellMatrix[x-1][y].stat != EMPTY))
                            && ((x+1 >= cellMatrix.length) || (cellMatrix[x+1][y].stat != EMPTY))) {
                        inWell = true;
                        deep = 1;
                    }
                } else { // in well
                    deep++;
                }
            }

            sums += IntStream.rangeClosed(0, deep).sum();
        }

        return sums;
    }

    private static double evaluateScore(Container container) {
        Container.Cell[][] cellMatrix = container.getCellMatrix();
        return landingHeight(container) * LANDING_HEIGHT_WEIGHT
                + rowsEliminated(cellMatrix)*ROWS_ELIMINATED_WEIGHT
                + rowTransitions(cellMatrix)*ROW_TRANSITIONS_WEIGHT
                + columnTransitions(cellMatrix)*COLUMN_TRANSITIONS_WEIGHT
                + numberOfHoles(cellMatrix)*NUMBER_OF_HOLES_WEIGHT
                + wellSums(cellMatrix)*WELL_SUMS_WEIGHT;
    }

    public void calBestColAndStat() {
        Block block = container.getDanglingBlock();
        assert block != null;

        double maxScore = Double.NEGATIVE_INFINITY;
        int col = 0;
        int blockStat = 1;

        for (int i = block.getStatsCount(); i > 0; i--) {
            for (int x = -Block.SIDE_LEN + 1; x < container.getColumnsCount(); x++) {
                Container.ConflictType type = container.setDanglingBlock(x, -Block.SIDE_LEN, block);
                if (type != NONE_CONFLICT) {
                    continue;
                }

                while (container.tryMoveDown());

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
        container.setDanglingBlock(col, -Block.SIDE_LEN, block);
    }

//    public void fallOne() {
//        calBestColAndStat();
//        while (container.moveDown()) {
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void stop() {

    }
}
