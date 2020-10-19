package io.github.keyodesu.ai;

import io.github.keyodesu.Config;
import io.github.keyodesu.Container;
import io.github.keyodesu.block.Block;

import java.util.stream.IntStream;

import static io.github.keyodesu.Container.CellStat.*;

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
     * 1. Landing Height: The height where the piece is put (= the height of the column + (the height of the piece / 2))
     */
    private int landingHeight(Block block) {
        boolean[][]data = block.getData();
        return 1;
    }

    /**
     * 2. Rows eliminated: The number of rows eliminated.
     */
    private int rowsEliminated(Block block) {
        return 1;
    }

    /**
     * 3. Row Transitions: The total number of row transitions.
     * A row transition occurs when an empty cell is adjacent to a filled cell on the same row and vice versa.
     */
    private int rowTransitions(Block block) {
        return 1;
    }

    private static int rowTransitions1(Container.CellStat[][] statMatrix) {
        int count = 0;

        for (int x = 0; x < statMatrix.length; x++) {
            for (int y = 0; y < statMatrix[x].length; y++) {
                if ((statMatrix[x][y] == EMPTY)
                        && ((x-1 >= 0 && statMatrix[x-1][y] != EMPTY) || (x+1 < statMatrix.length && statMatrix[x+1][y] != EMPTY))) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 4. Column Transitions: The total number of column transitions.
     * A column transition occurs when an empty cell is adjacent to a filled cell on the same column and vice versa.
     */
    private int columnTransitions(Block block) {
        return 1;
    }

    private static int columnTransitions1(Container.CellStat[][] statMatrix) {
        int count = 0;

        for (var column : statMatrix) {
            for (int y = 0; y < column.length; y++) {
                if ((column[y] == EMPTY)
                        && ((y - 1 >= 0 && column[y - 1] != EMPTY) || (y + 1 < column.length && column[y + 1] != EMPTY))) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * 5. Number of Holes: A hole is an empty cell that has at least one filled cell above it in the same column.
     */
    private int numberOfHoles(Block block) {
        return 1;
    }

    private static int numberOfHoles1(Container.CellStat[][] statMatrix) {
        int num = 0;

        for (var column : statMatrix) {
            boolean filledCellAbove = false;
            for (var stat : column) {
                if (stat == SOLIDIFY) {
                    filledCellAbove = true;
                } else if (filledCellAbove) {
                    num++;
                }
            }
        }

        return num;
    }


    /**
     * 6. Well Sums: A well is a succession of empty cells such that their left cells and right cells are both filled.
     */
    private int wellSums(Block block) {
        return 1;
    }

    private static int wellSums1(Container.CellStat[][] statMatrix) {
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

    private double evaluateScore(Block block) {
        return landingHeight(block)*LANDING_HEIGHT_WEIGHT
                + rowsEliminated(block)*ROWS_ELIMINATED_WEIGHT
                + rowTransitions(block)*ROW_TRANSITIONS_WEIGHT
                + columnTransitions(block)*COLUMN_TRANSITIONS_WEIGHT
                + numberOfHoles(block)*NUMBER_OF_HOLES_WEIGHT
                + wellSums(block)*WELL_SUMS_WEIGHT;
    }

    public int calBestColAndStat(Block block) {
        double maxScore = Double.NEGATIVE_INFINITY;
        int col = Integer.MIN_VALUE;
        int blockStat = -1;

        for (int i = block.getStatsCount(); i > 0; i--) {
            for (int x = -Block.SIDE_LEN + 1; x < Config.COL; x++) {
                double score = evaluateScore(block);

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
        // System.out.println(col); ////////////////////////////////////////////////////////////////////////////
        col = 7;  ///////////////////////////////////////todo//////////////////////////////////////////////////
        return col;
    }

    public void stop() {

    }
}
