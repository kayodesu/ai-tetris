package io.github.keyodesu.ai;

import io.github.keyodesu.block.Block;

public interface AI {
    int calBestColAndStat(Block block);
    void stop();
}
