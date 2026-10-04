package com.thedeucecara.digitalexchange.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ExchangeConfig {

    public static class Common {
        public final ModConfigSpec.DoubleValue inputRatio;
        public final ModConfigSpec.DoubleValue extractRatio;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("DigitalExchange Economy & Difficulty Configuration")
                   .push("general");

            inputRatio = builder
                    .comment("Multiplier applied to the base value when inserting/dissolving items into Bits.",
                             "Example: 1.0 = 100% value. 0.5 = 50% value (Harder). 2.0 = 200% value (Easier).")
                    .defineInRange("inputRatio", 1.0, 0.01, 100.0);

            extractRatio = builder
                    .comment("Multiplier applied to the base value when extracting/duplicating items.",
                             "Example: 1.0 = standard cost. 2.0 = costs double to withdraw (Harder). 0.5 = costs half (Easier).")
                    .defineInRange("extractRatio", 1.0, 0.01, 100.0);

            builder.pop();
        }
    }

    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
}
