package cn.craftime.fqincatcher.mixin.client;

import cn.craftime.fqincatcher.copy.FqincatcherCopyController;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class CopyNetworkMixin {
    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void fqincatcher$onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        FqincatcherCopyController.markDirty(new ChunkPos(packet.getChunkX(), packet.getChunkZ()));
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("TAIL"))
    private void fqincatcher$onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        var sectionPos = ((ChunkDeltaUpdateS2CPacketAccessor) packet).fqincatcher$getSectionPos();
        FqincatcherCopyController.markDirty(sectionPos);
    }

    @Inject(method = "onBlockUpdate", at = @At("TAIL"))
    private void fqincatcher$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        FqincatcherCopyController.markDirty(new ChunkPos(packet.getPos()));
    }

    @Inject(method = "onBlockEntityUpdate", at = @At("TAIL"))
    private void fqincatcher$onBlockEntityUpdate(BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
        FqincatcherCopyController.markDirty(new ChunkPos(packet.getPos()));
    }
}

