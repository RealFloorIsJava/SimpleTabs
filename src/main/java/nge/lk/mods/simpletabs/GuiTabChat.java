package nge.lk.mods.simpletabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * A chat with configurable tabs.
 */
public class GuiTabChat extends GuiNewChat {

    /**
     * The tab manager.
     */
    private final TabManager tabManager;

    /**
     * Constructor.
     *
     * @param mc The minecraft instance.
     */
    public GuiTabChat(final Minecraft mc, final TabManager tabManager) {
        super(mc);
        this.tabManager = tabManager;
    }

    @Override
    public void drawChat(final int updateCounter) {
        tabManager.updateTabs(getChatScale());
        tabManager.getActiveChat().drawChat(updateCounter);

        GlStateManager.pushMatrix();
        GlStateManager.translate(2.0F, 8.0F, 0.0F);
        GlStateManager.scale(getChatScale(), getChatScale(), 1.0f);

        if (getChatOpen()) {
            tabManager.drawTabLabels();
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void clearChatMessages(final boolean clearSent) {
        tabManager.getAllChats().forEach(chat -> chat.clearChatMessages(clearSent));
    }

    @Override
    public void printChatMessageWithOptionalDeletion(final ITextComponent chatComponent, final int chatLineId) {
        tabManager.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
    }

    @Override
    public void refreshChat() {
        tabManager.getAllChats().forEach(GuiNewChat::refreshChat);
    }

    @Override
    public void addToSentMessages(String message) {
        final String prefix = tabManager.getActiveChat().getPrefix();

        // Remove any artificially added prefix.
        if (!prefix.isEmpty() && message.startsWith(prefix)) {
            message = message.substring(prefix.length());
        }

        super.addToSentMessages(message);
    }

    @Override
    public void resetScroll() {
        tabManager.getAllChats().forEach(GuiNewChat::resetScroll);
    }

    @Override
    public void scroll(final int amount) {
        tabManager.getActiveChat().scroll(amount);
    }

    @Nullable
    @Override
    public ITextComponent getChatComponent(final int mouseX, final int mouseY) {
        return tabManager.getActiveChat().getChatComponent(mouseX, mouseY);
    }
}
