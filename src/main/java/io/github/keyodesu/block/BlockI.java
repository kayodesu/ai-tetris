package io.github.keyodesu.block;

/**
 * I形的小方块
 * @author Yo Ka
 *
 */
public class BlockI extends Block {
    public BlockI() {
        super(2);
        
        // I型的block有以下两种形态
        data[0][1][0] = data[0][1][1] = data[0][1][2] = data[0][1][3] = true;
        data[1][0][3] = data[1][1][3] = data[1][2][3] = data[1][3][3] = true;
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
