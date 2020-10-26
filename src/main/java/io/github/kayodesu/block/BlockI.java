package io.github.kayodesu.block;

/**
 * I形的小方块
 * @author Yo Ka
 *
 */
public class BlockI extends Block {
    public BlockI() {
        // I型的block有两种形态
        super(2);

        // .o..
        // .o..
        // .o..
        // .o..
        data[0][1][0] = data[0][1][1] = data[0][1][2] = data[0][1][3] = true;

        // ....
        // ....
        // ....
        // oooo
        data[1][0][3] = data[1][1][3] = data[1][2][3] = data[1][3][3] = true;
    }

    @Override
    public int getHeight() {
        return stat == 0 ? 4 : 1;
    }

    @Override
    public void switchToPrevStat() {
        stat = stat == 0 ? 1 : 0;
    }

    @Override
    public void switchToNextStat() {
        stat = stat == 0 ? 1 : 0;
    }

}
