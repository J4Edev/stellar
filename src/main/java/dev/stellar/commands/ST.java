package dev.stellar.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.stellar.Client;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ST {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ST::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, Object registryAccess) {

        dispatcher.register(literal("st").executes(ctx -> {
            Minecraft.getInstance().setScreenAndShow(
                    new Client.StellarScreen()
            );
            return 1;
        }));

        dispatcher.register(literal("stellar").redirect(dispatcher.getRoot().getChild("st")));
        dispatcher.register(literal("ste").redirect(dispatcher.getRoot().getChild("st")));
    }
}