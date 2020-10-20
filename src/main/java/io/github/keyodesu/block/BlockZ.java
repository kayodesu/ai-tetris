package io.github.keyodesu.block;

/**
 * Z形的小方块
 * @author Yo Ka
 *
 */
public class BlockZ extends Block {

    public BlockZ() {
        super(2);
        
        // Z型的block有以下两种形态
        data[0][1][2] = data[0][1][3] = data[0][2][1] = data[0][2][2] = true;        
        data[1][1][2] = data[1][2][2] = data[1][2][3] = data[1][3][3] = true;
    }

    @Override
    public int getHeight() {
        return stat == 0 ? 3 : 2;
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
