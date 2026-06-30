package dev.stellar.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.stellar.Client;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ST {

    private static final AtomicBoolean OPEN_REQUESTED = new AtomicBoolean(false);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ST::registerCommands);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (OPEN_REQUESTED.compareAndSet(true, false)) {
                client.setScreenAndShow(new Client.StellarScreen());
            }
        });
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, Object registryAccess) {

        dispatcher.register(literal("st").executes(ctx -> {
            OPEN_REQUESTED.set(true);
            return 1;
        }));

        dispatcher.register(literal("stellar").redirect(dispatcher.getRoot().getChild("st")));
        dispatcher.register(literal("ste").redirect(dispatcher.getRoot().getChild("st")));
    }
}