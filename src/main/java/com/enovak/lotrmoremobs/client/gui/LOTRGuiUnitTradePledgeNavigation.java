package com.enovak.lotrmoremobs.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lotr.client.LOTRClientProxy;
import lotr.client.gui.LOTRGuiFactions;
import lotr.client.gui.LOTRGuiUnitTrade;
import lotr.common.LOTRDimension;
import lotr.common.LOTRLevelData;
import lotr.common.LOTRMod;
import lotr.common.LOTRPlayerData;
import lotr.common.entity.npc.LOTRUnitTradeEntries;
import lotr.common.entity.npc.LOTRUnitTradeEntry;
import lotr.common.entity.npc.LOTRUnitTradeEntry.PledgeType;
import lotr.common.entity.npc.LOTRUnitTradeable;
import lotr.common.fac.LOTRAlignmentValues;
import lotr.common.fac.LOTRFaction;
import lotr.common.inventory.LOTRContainerUnitTrade;
import lotr.common.inventory.LOTRSlotAlignmentReward;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * Native LOTR unit-trade GUI with one real, fixed-label pledge button. The
 * inherited preview/background/button lifecycle is unchanged.
 */
public final class LOTRGuiUnitTradePledgeNavigation
        extends LOTRGuiUnitTrade {
    private static final ResourceLocation UNIT_TRADE_TEXTURE =
            new ResourceLocation("lotr:gui/npc/unit_trade.png");
    private static final int PLEDGE_NAVIGATION_BUTTON_ID = 0x4D554D;
    private static final int PLEDGE_X = 64;
    private static final int PLEDGE_TEXT_X = 83;
    private static final int PLEDGE_Y = 101;
    private static final int PLEDGE_HEIGHT = 16;
    private static final int EXTRA_INFO_X = 49;
    private static final int EXTRA_INFO_Y = 106;
    private static final int EXTRA_INFO_WIDTH = 9;
    private static final int EXTRA_INFO_HEIGHT = 7;

    private final LOTRUnitTradeable unitTrader;
    private final LOTRUnitTradeEntries trades;
    private final LOTRFaction traderFaction;
    private int currentTradeEntryIndex;
    private PledgeNavigationButton pledgeButton;

    public LOTRGuiUnitTradePledgeNavigation(
            EntityPlayer player,
            LOTRUnitTradeable trader,
            World world
    ) {
        super(player, trader, world);
        this.unitTrader = trader;
        this.trades = trader.getUnits();
        this.traderFaction = trader.getFaction();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.pledgeButton = new PledgeNavigationButton(
                PLEDGE_NAVIGATION_BUTTON_ID,
                this.guiLeft + PLEDGE_X,
                this.guiTop + PLEDGE_Y
        );
        this.buttonList.add(this.pledgeButton);
        this.updatePledgeButton();
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        this.updatePledgeButton();
        super.drawScreen(mouseX, mouseY, partialTicks);

        /*
         * This is the GUI's normal final-tooltip stage, after the button and
         * foreground. The fixed label is never redrawn or mutated on hover.
         */
        if (this.pledgeButton != null
                && this.pledgeButton.visible
                && this.pledgeButton.isMouseOver(
                mouseX,
                mouseY
        )) {
            String requirement = this.currentTrade()
                    .getPledgeType()
                    .getCommandReqText(this.traderFaction);
            List lines = this.fontRendererObj
                    .listFormattedStringToWidth(
                            requirement,
                            200
                    );
            this.func_146283_a(lines, mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == this.pledgeButton
                && button.enabled
                && button.visible) {
            this.openFactionPage(this.traderFaction);
            return;
        }

        if (button.enabled && button.id == 0
                && this.currentTradeEntryIndex > 0) {
            --this.currentTradeEntryIndex;
        } else if (button.enabled && button.id == 2
                && this.currentTradeEntryIndex
                < this.trades.tradeEntries.length - 1) {
            ++this.currentTradeEntryIndex;
        }
        super.actionPerformed(button);
    }

    /**
     * Matches LOTR's native foreground exactly except that a faction pledge
     * row is owned by the real GuiButton drawn in the normal button pass.
     * Non-faction pledge types and every native tooltip remain native.
     */
    @Override
    protected void drawGuiContainerForegroundLayer(
            int mouseX,
            int mouseY
    ) {
        LOTRUnitTradeEntry trade = this.currentTrade();
        this.drawCenteredString(
                this.unitTrader.getNPCName(),
                110,
                11,
                4210752
        );
        this.fontRendererObj.drawString(
                StatCollector.translateToLocal(
                        "container.inventory"
                ),
                30,
                162,
                4210752
        );
        this.drawCenteredString(
                trade.getUnitTradeName(),
                138,
                50,
                4210752
        );

        int requirementX = 64;
        int requirementTextX = requirementX + 19;
        int requirementY = 65;
        int requirementTextOffsetY = 4;
        int requirementGap = 18;

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
        RenderItem.getInstance().renderItemAndEffectIntoGUI(
                this.fontRendererObj,
                this.mc.getTextureManager(),
                new ItemStack(LOTRMod.silverCoin),
                requirementX,
                requirementY
        );
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int cost = trade.getCost(
                this.mc.thePlayer,
                this.unitTrader
        );
        this.fontRendererObj.drawString(
                String.valueOf(cost),
                requirementTextX,
                requirementY + requirementTextOffsetY,
                4210752
        );

        int nextRequirementY =
                requirementY + requirementGap;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(
                LOTRClientProxy.alignmentTexture
        );
        this.drawTexturedModalRect(
                requirementX,
                nextRequirementY,
                0,
                36,
                16,
                16
        );
        String alignment = LOTRAlignmentValues
                .formatAlignForDisplay(
                        trade.alignmentRequired
                );
        this.fontRendererObj.drawString(
                alignment,
                requirementTextX,
                nextRequirementY + requirementTextOffsetY,
                4210752
        );

        if (trade.getPledgeType() != PledgeType.NONE) {
            nextRequirementY += requirementGap;
            if (trade.getPledgeType() != PledgeType.FACTION) {
                this.drawNativeNonFactionPledge(
                        trade,
                        requirementX,
                        requirementTextX,
                        nextRequirementY,
                        requirementTextOffsetY,
                        mouseX,
                        mouseY
                );
            }
        }

        LOTRContainerUnitTrade container =
                (LOTRContainerUnitTrade)this.inventorySlots;
        if (container.alignmentRewardSlots > 0) {
            Slot rewardSlot = this.inventorySlots.getSlot(0);
            if (rewardSlot.getHasStack()) {
                GL11.glEnable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_CULL_FACE);
                RenderItem.getInstance()
                        .renderItemAndEffectIntoGUI(
                                this.fontRendererObj,
                                this.mc.getTextureManager(),
                                new ItemStack(LOTRMod.silverCoin),
                                160,
                                100
                        );
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glColor4f(
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                );
                this.fontRendererObj.drawString(
                        String.valueOf(
                                LOTRSlotAlignmentReward
                                        .REWARD_COST
                        ),
                        179,
                        104,
                        4210752
                );
            } else if (LOTRLevelData.getData(
                    this.mc.thePlayer
            ).getAlignment(this.traderFaction) < 1500.0F
                    && this.func_146978_c(
                    rewardSlot.xDisplayPosition,
                    rewardSlot.yDisplayPosition,
                    16,
                    16,
                    mouseX,
                    mouseY
            )) {
                this.drawCreativeTabHoveringText(
                        StatCollector.translateToLocalFormatted(
                                "container.lotr.unitTrade"
                                        + ".requiresAlignment",
                                new Object[] {
                                        Float.valueOf(1500.0F)
                                }
                        ),
                        mouseX - this.guiLeft,
                        mouseY - this.guiTop
                );
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glColor4f(
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                );
            }
        }

        if (trade.hasExtraInfo()) {
            this.drawNativeExtraInfo(
                    trade,
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawNativeNonFactionPledge(
            LOTRUnitTradeEntry trade,
            int requirementX,
            int requirementTextX,
            int requirementY,
            int requirementTextOffsetY,
            int mouseX,
            int mouseY
    ) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(
                LOTRClientProxy.alignmentTexture
        );
        this.drawTexturedModalRect(
                requirementX,
                requirementY,
                0,
                212,
                16,
                16
        );
        this.fontRendererObj.drawString(
                StatCollector.translateToLocal(
                        "container.lotr.unitTrade.pledge"
                ),
                requirementTextX,
                requirementY + requirementTextOffsetY,
                4210752
        );

        int relativeMouseX =
                mouseX - this.guiLeft - requirementX;
        int relativeMouseY =
                mouseY - this.guiTop - requirementY;
        if (relativeMouseX >= 0
                && relativeMouseX < 16
                && relativeMouseY >= 0
                && relativeMouseY < 16) {
            this.drawCreativeTabHoveringText(
                    trade.getPledgeType()
                            .getCommandReqText(
                                    this.traderFaction
                            ),
                    mouseX - this.guiLeft,
                    mouseY - this.guiTop
            );
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glColor4f(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }

    private void drawNativeExtraInfo(
            LOTRUnitTradeEntry trade,
            int mouseX,
            int mouseY
    ) {
        boolean hovered =
                mouseX >= this.guiLeft + EXTRA_INFO_X
                        && mouseX < this.guiLeft
                        + EXTRA_INFO_X
                        + EXTRA_INFO_WIDTH
                        && mouseY >= this.guiTop
                        + EXTRA_INFO_Y
                        && mouseY < this.guiTop
                        + EXTRA_INFO_Y
                        + EXTRA_INFO_HEIGHT;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(
                UNIT_TRADE_TEXTURE
        );
        this.drawTexturedModalRect(
                EXTRA_INFO_X,
                EXTRA_INFO_Y,
                220,
                38 + (hovered ? 1 : 0)
                        * EXTRA_INFO_HEIGHT,
                EXTRA_INFO_WIDTH,
                EXTRA_INFO_HEIGHT
        );
        if (!hovered) {
            return;
        }

        float previousZ = this.zLevel;
        List description = this.fontRendererObj
                .listFormattedStringToWidth(
                        trade.getFormattedExtraInfo(),
                        200
                );
        this.func_146283_a(
                description,
                mouseX - this.guiLeft,
                mouseY - this.guiTop
        );
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.zLevel = previousZ;
    }

    private void updatePledgeButton() {
        if (this.pledgeButton == null) {
            return;
        }

        LOTRUnitTradeEntry trade = this.currentTrade();
        boolean factionPledge =
                trade.getPledgeType() == PledgeType.FACTION;
        this.pledgeButton.visible = factionPledge;
        this.pledgeButton.enabled = factionPledge;
        if (!factionPledge) {
            return;
        }

        this.pledgeButton.xPosition =
                this.guiLeft + PLEDGE_X;
        this.pledgeButton.yPosition =
                this.guiTop + PLEDGE_Y;
        this.pledgeButton.displayString =
                this.getPledgeLabel();
        this.pledgeButton.width = Math.min(
                this.xSize - PLEDGE_X,
                19 + this.fontRendererObj.getStringWidth(
                        this.pledgeButton.displayString
                ) + 2
        );
    }

    private LOTRUnitTradeEntry currentTrade() {
        return this.trades.tradeEntries[
                this.currentTradeEntryIndex
                ];
    }

    private String getPledgeLabel() {
        LOTRPlayerData playerData = LOTRLevelData.getData(
                this.mc.thePlayer
        );
        if (playerData.isPledgedTo(this.traderFaction)) {
            return StatCollector.translateToLocal(
                    "gui.lotrmoremobs.unitTrade.pledged"
            );
        }
        return StatCollector.translateToLocalFormatted(
                "gui.lotrmoremobs.unitTrade.pledgeTo",
                new Object[] {
                        this.traderFaction.factionName()
                }
        );
    }

    private void openFactionPage(LOTRFaction faction) {
        LOTRPlayerData playerData = LOTRLevelData.getData(
                this.mc.thePlayer
        );
        playerData.setViewingFaction(faction);
        playerData.setRegionLastViewedFaction(
                faction.factionRegion,
                faction
        );

        Map<LOTRDimension.DimensionRegion, LOTRFaction>
                viewedRegions =
                new HashMap<LOTRDimension.DimensionRegion, LOTRFaction>();
        viewedRegions.put(
                faction.factionRegion,
                faction
        );
        LOTRClientProxy.sendClientInfoPacket(
                faction,
                viewedRegions
        );
        this.mc.displayGuiScreen(new LOTRGuiFactions());
    }

    private void drawCenteredString(
            String text,
            int centerX,
            int y,
            int color
    ) {
        this.fontRendererObj.drawString(
                text,
                centerX
                        - this.fontRendererObj
                        .getStringWidth(text) / 2,
                y,
                color
        );
    }

    private static final class PledgeNavigationButton
            extends GuiButton {
        private PledgeNavigationButton(int id, int x, int y) {
            super(id, x, y, 16, PLEDGE_HEIGHT, "");
        }

        @Override
        public void drawButton(
                Minecraft minecraft,
                int mouseX,
                int mouseY
        ) {
            if (!this.visible) {
                return;
            }

            this.field_146123_n = this.isMouseOver(
                    mouseX,
                    mouseY
            );
            minecraft.getTextureManager().bindTexture(
                    LOTRClientProxy.alignmentTexture
            );
            this.drawTexturedModalRect(
                    this.xPosition,
                    this.yPosition,
                    0,
                    212,
                    16,
                    16
            );
            minecraft.fontRenderer.drawString(
                    this.displayString,
                    this.xPosition + PLEDGE_TEXT_X
                            - PLEDGE_X,
                    this.yPosition + 4,
                    this.field_146123_n
                            ? 0x7F5A20
                            : 4210752
            );
            this.mouseDragged(
                    minecraft,
                    mouseX,
                    mouseY
            );
        }

        private boolean isMouseOver(int mouseX, int mouseY) {
            return mouseX >= this.xPosition
                    && mouseX < this.xPosition + this.width
                    && mouseY >= this.yPosition
                    && mouseY < this.yPosition + this.height;
        }
    }
}
