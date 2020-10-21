package io.github.keyodesu.block;

/**
 * L形的小方块
 * @author Yo Ka
 *
 */
public class BlockL extends Block {

    public BlockL() {
        // L型的block有四种形态
        super(4);

        // ....
        // .o..
        // .o..
        // .oo.
        data[0][1][1] = data[0][1][2] = data[0][1][3] = data[0][2][3] = true;

        // ....
        // ....
        // ooo.
        // o...
        data[1][0][2] = data[1][0][3] = data[1][1][2] = data[1][2][2] = true;

        // ....
        // .oo.
        // ..o.
        // ..o.
        data[2][1][1] = data[2][2][1] = data[2][2][2] = data[2][2][3] = true;

        // ....
        // ....
        // ..o.
        // ooo.
        data[3][0][3] = data[3][1][3] = data[3][2][2] = data[3][2][3] = true;
    }

    @Override
    public int getHeight() {
        return (stat == 0 || stat == 2) ? 3 : 2;
    }

    @Override
    public void switchToPrevStat() {
        if (--stat < 0)
            stat = 3;
    }

    @Override
    public void switchToNextStat() {
        if (++stat > 3)
            stat = 0;
    }

}
