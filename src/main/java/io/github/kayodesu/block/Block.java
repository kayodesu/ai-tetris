package io.github.kayodesu.block;

import javafx.scene.paint.Color;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Yo Ka
 */
public abstract class Block {
    protected int statsCount;

    // 当前block的形态
    protected int stat;

    public static final int SIDE_LEN = 4;

    private static final Color[] colors = {
            Color.BLUE, Color.DEEPPINK, Color.RED,
            Color.GREEN, Color.PURPLE
    };
    public Color color;
    
    // 第一维表示没个方块最多有4种状态，二三维表示每个方块最大宽高为4
    protected boolean[][][] data = new boolean[4][SIDE_LEN][SIDE_LEN];
    
    public Block(int statsCount) {
        this.statsCount = statsCount;
        stat = random.nextInt(statsCount);
        color = colors[random.nextInt(colors.length)];
    }
    
    public boolean[][] getData() {
        return data[stat];
    }

    public int getStatsCount() {
        return statsCount;
    }

    public int getStat() {
        return stat;
    }

    public abstract int getHeight();
    
    public abstract void switchToPrevStat(); // 转换到前一个状态
    public abstract void switchToNextStat(); // 转换到下一个状态

    public void switchToStat(int newStat) {
        assert newStat >= 0 && newStat < statsCount;
        while (stat != newStat) {
            switchToNextStat();
        }
    }

    private static Random random;
    private static List<Constructor<?>> constructors = new ArrayList<>();
    
    static {
        random = new Random();
        try {
            constructors.add(BlockI.class.getConstructor());
            constructors.add(BlockJ.class.getConstructor());
            constructors.add(BlockL.class.getConstructor());
            constructors.add(BlockO.class.getConstructor());
            constructors.add(BlockS.class.getConstructor());
            constructors.add(BlockT.class.getConstructor());
            constructors.add(BlockZ.class.getConstructor());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    
    public static Block getRandomBlock() {
        var constructor = constructors.get(random.nextInt(constructors.size()));
        Block block = null;
        try {
            block = (Block) constructor.newInstance();
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return block;
    }    
}
