package ai;

import io.github.keyodesu.Container;
import io.github.keyodesu.ai.ElTetris;
import io.github.keyodesu.block.Block;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.IntStream;

import static io.github.keyodesu.Config.*;
import static io.github.keyodesu.Container.CellStat.*;

/**
 * @author Yo Ka
 */
public class ElTetrisTest {

    private static Method numberOfHoles;
    private static Method wellSums;
    private static Method rowTransitions;
    private static Method columnTransitions;

    private static Container.CellStat[][] stats0 = new Container.CellStat[COL][ROW];
    private static Container.CellStat[][] stats1 = new Container.CellStat[COL][ROW];
    private static Container.CellStat[][] stats2 = new Container.CellStat[COL][ROW];
    private static Container.CellStat[][] stats3 = new Container.CellStat[COL][ROW];
    private static Container.CellStat[][] stats4 = new Container.CellStat[COL][ROW];

    private void printStats(Container.CellStat[][] stats) {
        for (int y = 0; y < ROW; y++) {
            for (int x = 0; x < COL; x++) {
                if (stats[x][y] == SOLIDIFY)
                    System.out.print('*');
                else
                    System.out.print('.');
            }
            System.out.println();
        }
    }

    @Test
    void testPrint() {
        printStats(stats0);
    }

    @BeforeAll
    static void init() throws NoSuchMethodException {
        numberOfHoles = ElTetris.class.getDeclaredMethod("numberOfHoles", Container.CellStat[][].class);
        numberOfHoles.setAccessible(true);

        wellSums = ElTetris.class.getDeclaredMethod("wellSums", Container.CellStat[][].class);
        wellSums.setAccessible(true);

        rowTransitions = ElTetris.class.getDeclaredMethod("rowTransitions", Container.CellStat[][].class);
        rowTransitions.setAccessible(true);

        columnTransitions = ElTetris.class.getDeclaredMethod("columnTransitions", Container.CellStat[][].class);
        columnTransitions.setAccessible(true);

        for (int x = 0; x < COL; x++) {
            for (int y = 0; y < ROW; y++) {
                stats0[x][y] = stats1[x][y] = stats2[x][y] = stats3[x][y] = stats4[x][y] = EMPTY;
            }
        }

        stats0[0][19] = SOLIDIFY;
        stats0[1][1] = SOLIDIFY;
        stats0[1][7] = SOLIDIFY;
        stats0[7][9] = SOLIDIFY;

        stats1[0][18] = SOLIDIFY;
        stats1[2][18] = SOLIDIFY;
        stats1[4][17] = SOLIDIFY;
        stats1[4][18] = SOLIDIFY;
        stats1[6][17] = SOLIDIFY;
        stats1[6][18] = SOLIDIFY;
    }

    @Test
    void testNumberOfHoles() throws InvocationTargetException, IllegalAccessException {
        int num = (Integer) numberOfHoles.invoke(null, (Object) stats0);
        printStats(stats0);
        System.out.println(num);
    }

    @Test
    void testWellSums() throws InvocationTargetException, IllegalAccessException {
        int num = (Integer) wellSums.invoke(null, (Object) stats1);
        printStats(stats1);
        System.out.println(num);
    }

    @Test
    void testRowTransitions() throws InvocationTargetException, IllegalAccessException {
        int count = (Integer) rowTransitions.invoke(null, (Object) stats1);
        printStats(stats1);
        System.out.println(count);
    }

    @Test
    void testColumnTransitions() throws InvocationTargetException, IllegalAccessException {
        int count = (Integer) columnTransitions.invoke(null, (Object) stats1);
        printStats(stats1);
        System.out.println(count);
    }
}
