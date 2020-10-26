package io.github.kayodesu.block;

/**
 * S形的小方块
 * @author Yo Ka
 *
 */
public class BlockS extends Block {

    public BlockS() {
        // S型的block有两种形态
        super(2);

        // ....
        // .o..
        // .oo.
        // ..o.
        data[0][1][1] = data[0][1][2] = data[0][2][2] = data[0][2][3] = true;

         // ....
         // ....
         // .oo.
         // oo..
        data[1][0][3] = data[1][1][3] = data[1][1][2] = data[1][2][2] = true;
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
