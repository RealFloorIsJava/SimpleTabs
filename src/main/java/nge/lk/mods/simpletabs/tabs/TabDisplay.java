package nge.lk.mods.simpletabs.tabs;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Handles displaying tabs and UI interaction with them.
 */
@RequiredArgsConstructor
public class TabDisplay {

    /**
     * The number of tabs per page.
     */
    public static final int TABS_PER_PAGE = 5;

    /**
     * The longest possible tab name.
     */
    private static final String MAXIMUM_TAB_NAME = "\u00a7lMMMMMMMM";

    /**
     * The mouse is over no tab.
     */
    private static final int NO_TAB = -1;

    /**
     * The mouse is over the 'Prior page' button.
     */
    private static final int LEFT_PAGE = -2;

    /**
     * The mouse is over the 'Next page' button.
     */
    private static final int RIGHT_PAGE = -3;

    /**
     * The mouse is over the 'Add tab' button.
     */
    private static final int ADD_TAB = -4;

    /**
     * The mouse is over the 'Cycle Group' button.
     */
    private static final int CYCLE_GROUP = -5;

    /**
     * The padding between tab labels.
     */
    private static final int PADDING = 3;

    /**
     * For some reason, the leftmost x coordinate is not 0.
     */
    private static final int LEFTMOST_X_COORDINATE = -2;

    /**
     * The color of the label background.
     */
    private static final int COLOR_BG_NORMAL = 0x66FFFFFF;

    /**
     * The color of the label background when the label is highlighted.
     */
    private static final int COLOR_BG_HIGHLIGHT = 0xAAFFFFFF;

    /**
     * The font color of the labels.
     */
    private static final int COLOR_FONT = 0xFF333333;

    /**
     * The font color of highlighted labels.
     */
    private static final int COLOR_FONT_HIGHLIGHT = 0xFF000000;

    /**
     * The tab manager.
     */
    private final TabManager tabManager;

    /**
     * The tab ID the mouse is over.
     */
    private int tabUnderMouse;

    /**
     * Handles a click in the chat.
     *
     * @param mouseButton The mouse button.
     */
    public void handleClick(final int mouseButton) {
        if (tabUnderMouse == LEFT_PAGE) {
            tabManager.previousTabPage();
        } else if (tabUnderMouse == RIGHT_PAGE) {
            tabManager.nextTabPage();
        } else if (tabUnderMouse == ADD_TAB) {
            tabManager.editTab(null, null);
        } else if (tabUnderMouse == CYCLE_GROUP) {
            tabManager.cycleTabGroup();
        } else if (tabUnderMouse != NO_TAB) {
            assert tabUnderMouse >= 0 : "unchecked flag value";

            int skip = tabManager.getTabOffset() + tabUnderMouse;
            final Iterator<Entry<String, ChatTab>> it = tabManager.getActiveTabGroup().iterator();
            while (it.hasNext()) {
                final Entry<String, ChatTab> tab = it.next();
                if (skip-- > 0) {
                    continue;
                }

                // This is the clicked tab (skip ignored all the other ones)
                if (mouseButton == 0) {
                    // Left mouse button.
                    tabManager.makeTabActive(tab.getKey());
                } else if (mouseButton == 1) {
                    // Right mouse button.
                    tabManager.editTab(tab.getKey(), tab.getValue());
                } else if (mouseButton == 2) {
                    // Middle mouse button.
                    it.remove();
                    resetSelectedTab();
                    tabManager.saveState();
                }

                // Only look at this tab.
                break;
            }
        }
    }

    /**
     * Draws the labels of the tabs.
     */
    public void drawTabLabels() {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        final int tabWidth = fontRenderer.getStringWidth(MAXIMUM_TAB_NAME) + PADDING;
        final int leftWidth = fontRenderer.getStringWidth("<") + PADDING;
        final int rightWidth = fontRenderer.getStringWidth(">") + PADDING;
        final int plusWidth = fontRenderer.getStringWidth("+") + PADDING;
        final int groupWidth =
                fontRenderer.getStringWidth(Integer.toString(tabManager.getActiveGroup() + 1)) + PADDING;

        // The position after the tabs -- the ones represent the margins.
        int finalBegin = LEFTMOST_X_COORDINATE + (leftWidth + 1) + (tabWidth + 1) * 5;
        int posX = LEFTMOST_X_COORDINATE;

        // Navigate left button.
        Gui.drawRect(posX, 0, posX + leftWidth, 10,
                tabUnderMouse == LEFT_PAGE ? COLOR_BG_HIGHLIGHT : COLOR_BG_NORMAL);
        fontRenderer.drawString("<", posX + 2, 1, COLOR_FONT);
        posX += leftWidth + 1;

        int index = 0;
        int skip = tabManager.getTabOffset();
        for (final Entry<String, ChatTab> entry : tabManager.getActiveTabGroup()) {
            // Skip hidden tabs.
            if (skip-- > 0) {
                continue;
            }

            // Limit to 5 tabs.
            if (index == 5) {
                break;
            }

            // Change the color of unread tab labels.
            final int colorMask = entry.getValue().isUnread() ? 0xFFFF3F3F : 0xFFFFFFFF;

            final boolean isTabActive = tabManager.isTabActive(entry.getKey());

            Gui.drawRect(posX, 0, posX + tabWidth, 10,
                    (index == tabUnderMouse ? COLOR_BG_HIGHLIGHT : COLOR_BG_NORMAL) & colorMask);
            fontRenderer.drawString((isTabActive ? "\u00a7l" : "") + entry.getKey(), posX + 2, 1,
                    isTabActive ? COLOR_FONT_HIGHLIGHT : COLOR_FONT);
            posX += tabWidth + 1;
            index++;
        }

        // Navigate right button.
        Gui.drawRect(finalBegin, 0, finalBegin + rightWidth, 10,
                tabUnderMouse == RIGHT_PAGE ? COLOR_BG_HIGHLIGHT : COLOR_BG_NORMAL);
        fontRenderer.drawString(">", finalBegin + 2, 1, COLOR_FONT);
        finalBegin += rightWidth + 1;

        // Add tab button.
        Gui.drawRect(finalBegin, 0, finalBegin + plusWidth, 10,
                tabUnderMouse == ADD_TAB ? COLOR_BG_HIGHLIGHT : COLOR_BG_NORMAL);
        fontRenderer.drawString("+", finalBegin + 2, 1, COLOR_FONT);
        finalBegin += plusWidth + 1;

        // Cycle group button.
        Gui.drawRect(finalBegin, 0, finalBegin + groupWidth, 10,
                tabUnderMouse == CYCLE_GROUP ? COLOR_BG_HIGHLIGHT : COLOR_BG_NORMAL);
        fontRenderer.drawString(Integer.toString(tabManager.getActiveGroup() + 1), finalBegin + 2, 1,
                COLOR_FONT);
    }

    /**
     * Updates the selected tab.
     */
    public void updateTabs(final float chatScale) {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        final int globalScale = scaledResolution.getScaleFactor();

        // These magic values are somehow needed to get the coordinates to match the ones in the draw method.
        int x = Mouse.getX() / globalScale - 2;
        int y = Mouse.getY() / globalScale - 40;

        x = MathHelper.floor((float) x / chatScale);
        y = MathHelper.floor((float) y / chatScale);

        tabUnderMouse = NO_TAB;
        // First: Check the correct Y position. This is the same for all labels.
        if (y < 0 && y >= -11) {
            final int tabWidth = fontRenderer.getStringWidth(MAXIMUM_TAB_NAME) + PADDING;
            final int leftWidth = fontRenderer.getStringWidth("<") + PADDING;
            final int rightWidth = fontRenderer.getStringWidth(">") + PADDING;
            final int plusWidth = fontRenderer.getStringWidth("+") + PADDING;
            final int groupWidth =
                    fontRenderer.getStringWidth(Integer.toString(tabManager.getActiveGroup() + 1)) + PADDING;
            final int finalBegin = LEFTMOST_X_COORDINATE + (leftWidth + 1) + (tabWidth + 1) * 5;

            // Special case <.
            if (x >= LEFTMOST_X_COORDINATE && x <= LEFTMOST_X_COORDINATE + leftWidth) {
                tabUnderMouse = LEFT_PAGE;
            }

            // Special case >.
            if (x >= finalBegin && x <= finalBegin + rightWidth) {
                tabUnderMouse = RIGHT_PAGE;
            }

            // Special case +.
            if (x >= finalBegin + rightWidth + 1 && x <= finalBegin + rightWidth + plusWidth) {
                tabUnderMouse = ADD_TAB;
            }

            // Special case group cycle.
            if (x >= finalBegin + rightWidth + plusWidth + 2
                    && x <= finalBegin + rightWidth + plusWidth + groupWidth + 1) {
                tabUnderMouse = CYCLE_GROUP;
            }

            final int offsetX = x - (LEFTMOST_X_COORDINATE + (leftWidth + 1));
            final int section = offsetX / (tabWidth + 1);
            if (offsetX >= 0 && section < 5) {
                tabUnderMouse = (offsetX % (tabWidth + 1) == tabWidth) ? -1 : section;
            }
        }

        // Mark the current tab read (there could have been new messages since the last switch).
        if (tabManager.getActiveChat() != null) {
            tabManager.getActiveChat().markRead();
        }
    }

    /**
     * Resets the selected tab to the first tab.
     */
    private void resetSelectedTab() {
        tabManager.resetSelectedTab();
        tabUnderMouse = NO_TAB;
    }
}
