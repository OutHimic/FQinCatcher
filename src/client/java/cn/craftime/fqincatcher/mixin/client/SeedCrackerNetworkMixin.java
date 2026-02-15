package cn.craftime.fqincatcher.mixin.client;

import cn.craftime.fqincatcher.seed.FqincatcherSeedCrackerController;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class SeedCrackerNetworkMixin {
    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void fqincatcher$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        FqincatcherSeedCrackerController.onGameJoin(packet.commonPlayerSpawnInfo().seed());
    }

    @Inject(method = "onPlayerRespawn", at = @At("TAIL"))
    private void fqincatcher$onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        FqincatcherSeedCrackerController.onPlayerRespawn(packet.commonPlayerSpawnInfo().seed());
    }

    @Inject(method = "onChunkData", at = @At("TAIL"))
    private void fqincatcher$onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
        FqincatcherSeedCrackerController.onChunkData(new ChunkPos(packet.getChunkX(), packet.getChunkZ()));
    }
}

