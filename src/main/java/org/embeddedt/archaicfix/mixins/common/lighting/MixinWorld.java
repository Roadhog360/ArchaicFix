package org.embeddedt.archaicfix.mixins.common.lighting;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.embeddedt.archaicfix.lighting.api.IChunkLighting;
import org.embeddedt.archaicfix.lighting.api.ILightingEngineProvider;
import org.embeddedt.archaicfix.lighting.world.lighting.LightingEngine;
import org.embeddedt.archaicfix.lighting.world.lighting.LightingEngineHelpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(World.class)
public abstract class MixinWorld implements ILightingEngineProvider {
    @Shadow protected Set activeChunkSet;

    @Shadow public abstract IChunkProvider getChunkProvider();

    private LightingEngine lightingEngine;

    /**
     * @author Angeline
     * Initialize the lighting engine on world construction.
     */
    @Redirect(method = "<init>(Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldProvider;Lnet/minecraft/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/ISaveHandler;loadWorldInfo()Lnet/minecraft/world/storage/WorldInfo;"))
    private WorldInfo onConstructed(ISaveHandler handler) {
        this.lightingEngine = new LightingEngine((World) (Object) this);
        return handler.loadWorldInfo();
    }


    /**
     * Directs the light update to the lighting engine and always returns a success value.
     * @author Angeline
     */
    @Inject(method = "updateLightByType", at = @At("HEAD"), cancellable = true)
    private void checkLightFor(EnumSkyBlock type, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        this.lightingEngine.scheduleLightUpdate(type, x, y, z);

        cir.setReturnValue(true);
    }

    @Inject(method = "setActivePlayerChunksAndCheckLight", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", args = {"ldc=playerCheckLight"}))
    private void speedUpLightChecks(CallbackInfo ci) {
        for(ChunkCoordIntPair pair : (Set<ChunkCoordIntPair>)this.activeChunkSet) {
            Chunk chunk = LightingEngineHelpers.getLoadedChunk(getChunkProvider(), pair.chunkXPos, pair.chunkZPos);
            if(chunk != null) {
                ((IChunkLighting)chunk).speedupRelight();
            }
        }
    }

    @Override
    public LightingEngine getLightingEngine() {
        return this.lightingEngine;
    }
}
