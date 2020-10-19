package io.github.keyodesu.block;

/**
 * S形的小方块
 * @author Yo Ka
 *
 */
public class BlockS extends Block {

    public BlockS() {
        super(2);
        
        // S型的block有以下两种形态
        data[0][1][1] = data[0][1][2] = data[0][2][2] = data[0][2][3] = true;        
        data[1][0][3] = data[1][1][3] = data[1][1][2] = data[1][2][2] = true;
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
