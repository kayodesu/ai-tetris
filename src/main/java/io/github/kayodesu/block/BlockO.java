package io.github.kayodesu.block;

/**
 * O形的小方块
 * @author Yo Ka
 *
 */
public class BlockO extends Block {

    public BlockO() {
        // O型的block有一种形态
        super(1);

        // ....
        // ....
        // .oo.
        // .oo.
        data[0][1][2] = data[0][1][3] = data[0][2][2] = data[0][2][3] = true;
    }

    @Override
    public int getHeight() {
        return 2;
    }

    @Override
    public void switchToPrevStat() {
    }

    @Override
    public void switchToNextStat() {
    }

}
