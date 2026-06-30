package dev.stellar;

import com.ckittya.core.CKScreen;
import com.ckittya.layout.CKLayout;
import com.ckittya.util.CKColor;
import com.ckittya.util.Rect;
import com.ckittya.widgets.*;
import dev.stellar.commands.ST;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class Client implements ClientModInitializer {

    public static final String MOD_ID = "stellar";

    @Override
    public void onInitializeClient() {
        ST.register();
    }

    public static final class StellarScreen extends CKScreen {

        private CKSelectionStrip testStrip;

        public StellarScreen() {
            super("Stellar");
        }

        @Override
        protected void build() {
            int sw = screenWidth();
            int sh = screenHeight();

            int panelW = 220;
            int panelH = 140;
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

            testStrip = new CKSelectionStrip()
                    .container(c -> c
                            .axis(CKLayout.Axis.ROW)
                            .gap(4)
                            .align(CKLayout.Align.CENTER)
                    );
            testStrip.layoutConfig().fillWidth().height(22);
            testStrip.add(new CKLabel("Option A")
                    .align(CKLabel.Align.CENTER)
                    .color(CKColor.CK_TEXT_DIM));
            testStrip.add(new CKLabel("Option B")
                    .align(CKLabel.Align.CENTER)
                    .color(CKColor.CK_TEXT_DIM));
            testStrip.selectedIndex(0);
            panel.add(testStrip);

            panel.add(new CKSpacer().fillMain());

            CKButton closeBtn = new CKButton("Close", () -> Minecraft.getInstance().gui.setScreen(null));
            closeBtn.layoutConfig().fillWidth().height(20)
                    .margin(4, 0, 0, 0);
            closeBtn.bg(CKColor.of(180, 180, 60, 60))
                    .bgHover(CKColor.of(220, 200, 80, 80));
            panel.add(closeBtn);

            addRoot(panel);
        }
    }
}