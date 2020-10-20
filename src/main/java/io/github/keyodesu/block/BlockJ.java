package io.github.keyodesu.block;

/**
 * J形的小方块
 * @author Yo Ka
 *
 */
public class BlockJ extends Block {

    public BlockJ() {
        super(4);
        
        // J型的block有以下四种形态
        data[0][1][3] = data[0][2][1] = data[0][2][2] = data[0][2][3] = true; // _|
        data[1][0][2] = data[1][0][3] = data[1][1][3] = data[1][2][3] = true; // |___
        data[2][1][1] = data[2][1][2] = data[2][1][3] = data[2][2][1] = true; //
        data[3][0][2] = data[3][1][2] = data[3][2][2] = data[3][2][3] = true; //
    }

    @Override
    public int getHeight() {
        return (stat == 0 || stat == 2) ? 3 : 2;
    }

    @Override
    public void switchToPrevStat() {
        if(--stat < 0)
            stat = 3;
        
    }

    @Override
    public void switchToNextStat() {
        if(++stat > 3)
            stat = 0;
    }

}
