package nge.lk.mods.simpletabs.tabs;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a chat tab.
 */
public class ChatTab extends GuiNewChat {

    /**
     * The magic value used to represent infinite history.
     */
    private static final int HISTORY_INFINITE = -1;

    /**
     * The filter which selects the messages to accept.
     */
    private Matcher filter;

    /**
     * Whether this tab has unread messages.
     */
    @Getter private boolean unread;

    /**
     * The pattern string, for saving.
     */
    @Getter private String pattern;

    /**
     * Whether the pattern is literal, for saving.
     */
    @Getter private boolean literal;

    /**
     * Whether this tab has a whitelist.
     */
    @Getter private boolean whitelist;

    /**
     * Whether this tab will play sound notifications.
     */
    @Getter private boolean notify;

    /**
     * The prefix for sent messages in this tab.
     */
    @Getter @Setter private String prefix;

    /**
     * How much history is kept. Stored as the float representation to prevent precision loss.
     */
    @Getter @Setter private float history;

    /**
     * Obtains the history size from a [0.0, 1.0] float.
     *
     * @param val The float.
     * @return The history size.
     */
    public static int getHistorySize(final float val) {
        final float maxVal = 0.99666f;  // Produces cutoff at approx. 1 million.
        if (val >= maxVal) {
            return HISTORY_INFINITE;
        } else {
            return (int) Math.floor(Math.pow(100.0f, 3.0f * val));
        }
    }

    /**
     * Constructor.
     *
     * @param mc The minecraft reference.
     */
    public ChatTab(final Minecraft mc, final String pattern, final boolean literal, final boolean whitelist,
                   final boolean notify, final String prefix, final float history) {
        super(mc);
        this.pattern = pattern;
        this.literal = literal;
        this.whitelist = whitelist;
        this.notify = notify;
        this.prefix = prefix;
        this.history = history;
        filter = Pattern.compile(pattern, literal ? Pattern.LITERAL : 0).matcher("");
    }

    /**
     * Updates the filter pattern.
     *
     * @param pattern   The new pattern.
     * @param literal   Whether the pattern will be escaped.
     * @param whitelist Whether the tab implements a whitelist or a blacklist.
     * @param notify    Whether this tab will play notification sounds.
     */
    public void updatePattern(final String pattern, final boolean literal, final boolean whitelist,
                              final boolean notify) {
        this.pattern = pattern;
        this.literal = literal;
        this.whitelist = whitelist;
        this.notify = notify;
        filter = Pattern.compile(pattern, literal ? Pattern.LITERAL : 0).matcher("");
    }

    @Override
    public void printChatMessageWithOptionalDeletion(final ITextComponent chatComponent, final int chatLineId) {
        super.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
        unread = true;
        if (notify) {
            final float before = Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS);
            Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.RECORDS, 1.0f);
            Minecraft.getMinecraft().thePlayer.playSound(SoundEvents.BLOCK_NOTE_PLING, 1.0f, 2.0f);
            Minecraft.getMinecraft().gameSettings.setSoundLevel(SoundCategory.RECORDS, before);
        }
    }

    @Override
    protected void setChatLine(final ITextComponent chatComponent, final int chatLineId, final int updateCounter,
                               final boolean displayOnly) {
        if (chatLineId != 0) {
            this.deleteChatLine(chatLineId);
        }

        final int maxLength = MathHelper.floor_float((float) getChatWidth() / getChatScale());
        final List<ITextComponent> splitComponents = GuiUtilRenderComponents.splitText(chatComponent, maxLength,
                Minecraft.getMinecraft().fontRendererObj, false, false);
        final boolean isChatOpen = getChatOpen();

        for (final ITextComponent comp : splitComponents) {
            if (isChatOpen && scrollPos > 0) {
                isScrolled = true;
                scroll(1);
            }

            drawnChatLines.add(0, new ChatLine(updateCounter, comp, chatLineId));
        }

        final int historySize = getHistorySize(history);
        while (historySize != HISTORY_INFINITE && drawnChatLines.size() > historySize) {
            drawnChatLines.remove(drawnChatLines.size() - 1);
        }

        if (!displayOnly) {
            chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));

            while (historySize != HISTORY_INFINITE && chatLines.size() > historySize) {
                chatLines.remove(chatLines.size() - 1);
            }
        }
    }

    /**
     * Whether this tab accepts the given message for display.
     *
     * @param message The message.
     * @return Whether it is accepted.
     */
    public boolean acceptsMessage(final CharSequence message) {
        filter.reset(message);
        return filter.find() == whitelist;
    }

    /**
     * Marks this tab as read.
     */
    public void markRead() {
        unread = false;
    }

    /**
     * Returns the export part of this tab.
     *
     * @return The export string.
     */
    public String getExport() {
        return pattern + "§" + literal + "§" + prefix + "§" + whitelist + "§" + notify + "§" + history;
    }
}
