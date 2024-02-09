package com.sjoopies.endertap.mixin;

import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CrashReport.class})
public class CrashReport_noopMixin {
    @Inject(method = {"initCrashReport"}, at = {@At("HEAD")})
    private static void gameStarted(CallbackInfo ci) {
        
    }
}
