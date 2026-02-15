package cn.craftime.fqincatcher.integration.seedcrackerx;

import cn.craftime.fqincatcher.seed.FqincatcherSeedCrackerController;
import kaptainwutax.seedcrackerX.api.SeedCrackerAPI;

public final class FqincatcherSeedCrackerEP implements SeedCrackerAPI {
    @Override
    public void pushWorldSeed(long seed) {
        FqincatcherSeedCrackerController.onCracked(seed);
    }
}

