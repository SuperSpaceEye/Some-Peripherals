package net.spaceeye.someperipherals.stuff.digitizer

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.spaceeye.someperipherals.SomePeripherals

class DigitizerScreen(menu: DigitizerMenu, inv: Inventory, component: Component):
AbstractContainerScreen<DigitizerMenu>(menu, inv, component) {
    private val TEXTURE = ResourceLocation(SomePeripherals.MOD_ID, "textures/gui/digitizer_container.png")

    override fun renderBg(poseStack: PoseStack, partialTick: Float, mouseX: Int, mouseY: Int) {
        RenderSystem.setShader { GameRenderer.getPositionShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, TEXTURE)
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2
        blit(poseStack, x, y, 0, 0, imageWidth, imageHeight)
        renderTooltip(poseStack, mouseX, mouseY)
    }

    override fun render(poseStack: PoseStack, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(poseStack)
        super.render(poseStack, mouseX, mouseY, delta)
        renderTooltip(poseStack, mouseX, mouseY)
    }
}