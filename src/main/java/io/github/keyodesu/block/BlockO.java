package io.github.keyodesu.block;

/**
 * O形的小方块
 * @author Yo Ka
 *
 */
public class BlockO extends Block {

    public BlockO() {
        super(1);
        
        // O型的block只有一种形态
        data[0][1][2] = true;
        data[0][1][3] = true;
        data[0][2][2] = true;
        data[0][2][3] = true;
    }

    @Override
    public void switchToPrevStat() {
    }

    @Override
    public void switchToNextStat() {
    }

}
