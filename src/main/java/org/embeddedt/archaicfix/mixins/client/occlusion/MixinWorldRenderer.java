package org.embeddedt.archaicfix.mixins.client.occlusion;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.shader.TesselatorVertexState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.chunk.EmptyChunk;
import org.embeddedt.archaicfix.occlusion.IWorldRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer implements IWorldRenderer {
    @Shadow public boolean isWaitingOnOcclusionQuery;

    @Shadow public World worldObj;

    @Shadow public int posX;

    @Shadow public int posZ;

    @Shadow public List tileEntityRenderers;

    @Shadow private List tileEntities;

    @Shadow public boolean needsUpdate;

    @Shadow public boolean isInitialized;

    @Shadow private int bytesDrawn;

    @Shadow private TesselatorVertexState vertexState;

    private int arch$lastCullUpdateFrame;

    @Inject(method = "markDirty", at = @At("TAIL"))
    private void resetOcclusionFlag(CallbackInfo ci) {
        this.isWaitingOnOcclusionQuery = false;
    }

    @Inject(method = "updateRenderer", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/world/chunk/Chunk;isLit:Z", ordinal = 0), cancellable = true)
    private void bailOnEmptyChunk(EntityLivingBase view, CallbackInfo ci) {
        if(worldObj.getChunkFromBlockCoords(posX, posZ) instanceof EmptyChunk) {
            if (tileEntityRenderers.size() > 0) {
                tileEntities.removeAll(tileEntityRenderers);
                tileEntityRenderers.clear();
            }
            needsUpdate = true;
            isInitialized = false;
            bytesDrawn = 0;
            vertexState = null;
            ci.cancel();
        }
    }

    @Override
    public int getLastCullUpdateFrame() {
        return arch$lastCullUpdateFrame;
    }

    @Override
    public void setLastCullUpdateFrame(int lastCullUpdateFrame) {
        arch$lastCullUpdateFrame = lastCullUpdateFrame;
    }
}
