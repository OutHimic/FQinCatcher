package cn.craftime.fqincatcher.client;

import cn.craftime.fqincatcher.screen.FqincatcherMainScreen;
import cn.craftime.fqincatcher.copy.FqincatcherCopyController;
import cn.craftime.fqincatcher.seed.FqincatcherSeedCrackerController;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FqincatcherClient implements ClientModInitializer {
    private static final KeyBinding OPEN_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.fqincatcher.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.fqincatcher"
    ));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_KEY.wasPressed()) {
                openScreen(client);
            }
            FqincatcherSeedCrackerController.tick();
            FqincatcherCopyController.tick();
        });
    }

    private static void openScreen(MinecraftClient client) {
        if (client == null) {
            return;
        }
        client.setScreen(new FqincatcherMainScreen(client.currentScreen));
    }
}
/*
每个爱装X的玩家其实都有一个当服主的梦(bushi
   By OutHimic
*/
