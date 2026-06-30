package dev.stellar;

import com.ckittya.core.CKScreen;
import com.ckittya.layout.CKLayout;
import com.ckittya.util.CKColor;
import com.ckittya.util.Rect;
import com.ckittya.widgets.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class Client implements ClientModInitializer {

    public static final String MOD_ID = "stellar";

    private static KeyMapping OPEN_SCREEN;

    public static boolean quartzClickerEnabled = false; // off by default
    public static boolean doubleClickDelayEnabled = true;
    public static int quartzClickDelayMs = 300;

    private static int trackedHandlerSyncId = -1;
    private static int[] lastHandledRevision = new int[0];
    private static long[] lastClickTimes = new long[0];

    @Override
    public void onInitializeClient() {
        OPEN_SCREEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.stellar.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SCREEN.consumeClick()) {
                if (client.gui.screen() instanceof StellarScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null) {
                    client.gui.setScreen(new StellarScreen());
                }
            }

            if (quartzClickerEnabled) {
                checkOpenContainerForQuartz(client);
            } else {
                resetQuartzTracking();
            }
        });
    }

    private static void checkOpenContainerForQuartz(Minecraft client) {
        if (client == null || client.player == null || client.gameMode == null) {
            resetQuartzTracking();
            return;
        }

        if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            resetQuartzTracking();
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        if (handler == null || handler.containerId < 0) {
            resetQuartzTracking();
            return;
        }

        if (handler.containerId != trackedHandlerSyncId) {
            trackedHandlerSyncId = handler.containerId;
            lastHandledRevision = new int[handler.slots.size()];
            lastClickTimes = new long[handler.slots.size()];
            Arrays.fill(lastHandledRevision, -1);
        } else if (lastHandledRevision.length != handler.slots.size()) {
            lastHandledRevision = new int[handler.slots.size()];
            lastClickTimes = new long[handler.slots.size()];
            Arrays.fill(lastHandledRevision, -1);
        }

        int revision = handler.getStateId();
        long now = System.currentTimeMillis();

        for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
            boolean quartzNow = handler.getSlot(slotIndex).hasItem()
                    && handler.getSlot(slotIndex).getItem().is(Items.QUARTZ_BLOCK);

            boolean delayPassed = !doubleClickDelayEnabled
                    || now - lastClickTimes[slotIndex] >= quartzClickDelayMs;

            if (quartzNow
                    && lastHandledRevision[slotIndex] != revision
                    && delayPassed) {

                lastHandledRevision[slotIndex] = revision;
                lastClickTimes[slotIndex] = now;

                clickInventorySlot(client, handler, slotIndex);
            }
        }
    }

    private static void resetQuartzTracking() {
        trackedHandlerSyncId = -1;
        lastHandledRevision = new int[0];
        lastClickTimes = new long[0];
    }

    private static void clickInventorySlot(Minecraft client, AbstractContainerMenu handler, int slotIndex) {
        if (client == null || client.player == null || client.gameMode == null || handler == null) {
            return;
        }

        // MultiPlayerGameMode#handleContainerInput(containerId, slotNum, buttonNum, ContainerInput, player)
        // -> menu.clicked(...) for prediction, then sends ServerboundContainerClickPacket to the server.
        client.gameMode.handleContainerInput(
                handler.containerId,
                slotIndex,
                0,
                ContainerInput.PICKUP,
                client.player
        );
    }

    public static final class StellarScreen extends CKScreen {

        private CKButton quartzToggleBtn;
        private CKSelectionStrip delayToggleStrip;
        private CKTextBox delayBox;

        public StellarScreen() {
            super("Stellar");
        }

        @Override
        protected void build() {
            int sw = screenWidth();
            int sh = screenHeight();

            int panelW = 260;
            int panelH = 300;
            float px = (sw - panelW) / 2f;
            float py = (sh - panelH) / 2f;

            CKPanel panel = new CKPanel()
                    .container(c -> c
                            .axis(CKLayout.Axis.COLUMN)
                            .pad(14, 14, 14, 14)
                            .gap(8)
                    );
            panel.setBounds(new Rect(px, py, panelW, panelH));

            panel.add(new CKLabel("Stellar")
                    .color(CKColor.CK_TEXT)
                    .shadow(true)
                    .align(CKLabel.Align.CENTER)
            );

            panel.add(new CKDivider());

            CKButton normalBtn = new CKButton("Say Meow!", () -> {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.sendSystemMessage(Component.literal("[Stellar] MEOW!"));
                }
            });
            normalBtn.layoutConfig().fillWidth().height(20);
            panel.add(normalBtn);

            quartzToggleBtn = new CKButton(quartzLabel(), this::toggleQuartz);
            quartzToggleBtn.layoutConfig().fillWidth().height(20);
            quartzToggleBtn.setTooltip("Auto-clicks quartz blocks");
            styleToggleBtn();
            panel.add(quartzToggleBtn);

            panel.add(new CKDivider());

            panel.add(new CKLabel("Delay gate")
                    .dim()
                    .align(CKLabel.Align.LEFT));

            delayToggleStrip = new CKSelectionStrip()
                    .container(c -> c
                            .axis(CKLayout.Axis.ROW)
                            .gap(4)
                            .align(CKLayout.Align.CENTER)
                    )
                    .onSelect(i -> doubleClickDelayEnabled = (i == 1));
            delayToggleStrip.layoutConfig().fillWidth().height(22);
            delayToggleStrip.add(new CKLabel("Disabled")
                    .align(CKLabel.Align.CENTER)
                    .color(CKColor.CK_TEXT_DIM));
            delayToggleStrip.add(new CKLabel("Enabled")
                    .align(CKLabel.Align.CENTER)
                    .color(CKColor.CK_TEXT_DIM));
            delayToggleStrip.selectedIndex(doubleClickDelayEnabled ? 1 : 0);
            panel.add(delayToggleStrip);

            panel.add(new CKLabel("Delay (ms)")
                    .dim()
                    .align(CKLabel.Align.LEFT));

            delayBox = new CKTextBox()
                    .placeholder("300")
                    .maxLength(6)
                    .filter(s -> s.isEmpty() || s.matches("\\d+"))
                    .onChange(text -> {
                        if (text.isEmpty()) {
                            return;
                        }
                        try {
                            quartzClickDelayMs = Math.max(0, Integer.parseInt(text));
                        } catch (NumberFormatException ignored) {
                        }
                    });
            delayBox.layoutConfig().fillWidth().height(20);
            delayBox.setText(String.valueOf(quartzClickDelayMs));
            panel.add(delayBox);

            CKButton closeBtn = new CKButton("Close", () -> Minecraft.getInstance().gui.setScreen(null));
            closeBtn.layoutConfig().fillWidth().height(20)
                    .margin(4, 0, 0, 0);
            closeBtn.bg(CKColor.of(180, 180, 60, 60))
                    .bgHover(CKColor.of(220, 200, 80, 80));
            panel.add(closeBtn);

            // fillMain() is required so the spacer actually claims the remaining
            // column space and pushes the status label to the bottom.
            panel.add(new CKSpacer().fillMain());

            CKLabel status = new CKLabel("")
                    .live(() -> "Quartz clicker is " + (quartzClickerEnabled ? "active" : "disabled")
                            + " | delay gate: " + (doubleClickDelayEnabled ? "enabled" : "disabled")
                            + " | delay: " + quartzClickDelayMs + " ms")
                    .dim()
                    .align(CKLabel.Align.CENTER);
            status.layoutConfig().fillWidth();
            panel.add(status);

            addRoot(panel);
        }

        private void toggleQuartz() {
            Client.quartzClickerEnabled = !Client.quartzClickerEnabled;
            quartzToggleBtn.label(quartzLabel());
            styleToggleBtn();
        }

        private static String quartzLabel() {
            return "Quartz Clicker: " + (quartzClickerEnabled ? "ON" : "OFF");
        }

        private void styleToggleBtn() {
            if (quartzClickerEnabled) {
                quartzToggleBtn
                        .bg(CKColor.of(200, 30, 130, 60))
                        .bgHover(CKColor.of(220, 40, 160, 80))
                        .bgPress(CKColor.of(200, 25, 110, 50));
            } else {
                quartzToggleBtn
                        .bg(CKColor.CK_SURFACE)
                        .bgHover(CKColor.CK_ACCENT)
                        .bgPress(CKColor.CK_ACCENT_DIM);
            }
        }
    }
}