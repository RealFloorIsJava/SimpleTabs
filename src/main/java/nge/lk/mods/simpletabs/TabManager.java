package nge.lk.mods.simpletabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import nge.lk.mods.commonlib.util.DebugUtil;
import nge.lk.mods.commonlib.util.FileUtil;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The owner of all tabs, managing the tab configurations.
 */
public class TabManager {

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
     * The existing tabs in a {@code Name -> Tab} mapping.
     */
    private final Map<String, ChatTab> tabs;

    /**
     * The file where the tab configurations are stored in.
     */
    private final File saveFile;

    /**
     * The tab ID the mouse is over.
     */
    private int tabUnderMouse;

    /**
     * The offset of the leftmost tab.
     */
    private int tabOffset;

    /**
     * The currently active tab.
     */
    private String activeTab;

    /**
     * Constructor.
     *
     * @param saveFile The file where tab configurations are saved in.
     */
    public TabManager(final File saveFile) {
        this.saveFile = saveFile;
        tabs = new LinkedHashMap<>();

        loadState();

        if (tabs.isEmpty()) {
            addDefaultTab();
        }

        activeTab = tabs.keySet().stream().findFirst().get();
    }

    /**
     * Handles a click in the chat.
     *
     * @param mouseButton The mouse button.
     */
    public void handleClick(final int mouseButton) {
        if (tabUnderMouse == LEFT_PAGE) {
            tabOffset = Math.max(0, tabOffset - 5);
        } else if (tabUnderMouse == RIGHT_PAGE) {
            if (tabOffset + 5 < tabs.size()) {
                tabOffset += 5;
            }
        } else if (tabUnderMouse == ADD_TAB) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTabEditor(Minecraft.getMinecraft().currentScreen,
                    this, null, null));
        } else if (tabUnderMouse != NO_TAB) {
            boolean needsToSave = false;
            int skip = tabOffset + tabUnderMouse;
            for (final Iterator<Entry<String, ChatTab>> it = tabs.entrySet().iterator(); it.hasNext(); ) {
                final Entry<String, ChatTab> tab = it.next();
                if (skip-- > 0) {
                    continue;
                }
                if (mouseButton == 0) {
                    activeTab = tab.getKey();
                } else if (mouseButton == 1 && tabOffset + tabUnderMouse >= 0) {
                    Minecraft.getMinecraft().displayGuiScreen(new GuiTabEditor(Minecraft.getMinecraft().currentScreen,
                            this, tab.getValue(), tab.getKey()));
                } else if (mouseButton == 2 && tabOffset + tabUnderMouse >= 0) {
                    // Middle mouse button.
                    it.remove();
                    if (tabs.size() == 1) {
                        // Add a default tab back.
                        addDefaultTab();
                    }
                    resetSelectedTab();
                    needsToSave = true;
                }
                break;
            }
            if (needsToSave) {
                saveState();
            }
        }
    }

    /**
     * Draws the labels of the tabs.
     */
    public void drawTabLabels() {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        final int tabWidth = fontRenderer.getStringWidth(MAXIMUM_TAB_NAME) + 3;
        final int leftWidth = fontRenderer.getStringWidth("<") + 3;
        final int rightWidth = fontRenderer.getStringWidth(">") + 3;
        final int plusWidth = fontRenderer.getStringWidth("+") + 1;
        int finalBegin = -2 + (leftWidth + 1) + (tabWidth + 1) * 5;

        int posX = -2;

        // Navigate left button.
        Gui.drawRect(posX, 0, posX + leftWidth, 10,
                tabUnderMouse == LEFT_PAGE ? 0xAAFFFFFF : 0x66FFFFFF);
        fontRenderer.drawString("<", posX + 2, 1, 0xFF333333);
        posX += leftWidth + 1;

        int index = 0;
        int skip = tabOffset;
        for (final Entry<String, ChatTab> entry : tabs.entrySet()) {
            if (skip-- > 0) {
                continue;
            }
            if (index == 5) {
                break;
            }

            final int colorMask = entry.getValue().isUnread() ? 0xFFFF3F3F : 0xFFFFFFFF;
            Gui.drawRect(posX, 0, posX + tabWidth, 10,
                    (index == tabUnderMouse ? 0xAAFFFFFF : 0x66FFFFFF) & colorMask);
            fontRenderer.drawString(
                    (entry.getKey().equals(activeTab) ? "\u00a7l" : "") + entry.getKey(),
                    posX + 2,
                    1,
                    entry.getKey().equals(activeTab) ? 0xFF000000 : 0xFF333333
            );
            posX += tabWidth + 1;
            index++;
        }

        // Navigate right button.
        Gui.drawRect(finalBegin, 0, finalBegin + rightWidth, 10,
                tabUnderMouse == RIGHT_PAGE ? 0xAAFFFFFF : 0x66FFFFFF);
        fontRenderer.drawString(">", finalBegin + 2, 1, 0xFF333333);
        finalBegin += rightWidth + 1;

        // Add tab button.
        Gui.drawRect(finalBegin, 0, finalBegin + plusWidth, 10,
                tabUnderMouse == ADD_TAB ? 0xAAFFFFFF : 0x66FFFFFF);
        fontRenderer.drawString("+", finalBegin + 1, 1, 0xFF333333);
    }

    /**
     * Updates the selected tab.
     */
    public void updateTabs(final float chatScale) {
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        final int globalScale = scaledResolution.getScaleFactor();
        int x = Mouse.getX() / globalScale - 2;
        int y = Mouse.getY() / globalScale - 27;
        x = MathHelper.floor_float((float) x / chatScale);
        y = MathHelper.floor_float((float) y / chatScale);

        tabUnderMouse = -1;
        if (y < 0 && y >= -11) {
            final int tabWidth = fontRenderer.getStringWidth(MAXIMUM_TAB_NAME) + 3;
            final int leftWidth = fontRenderer.getStringWidth("<") + 3;
            final int rightWidth = fontRenderer.getStringWidth(">") + 3;
            final int plusWidth = fontRenderer.getStringWidth("+") + 1;
            final int finalBegin = -2 + (leftWidth + 1) + (tabWidth + 1) * 5;

            // Special case <.
            if (x >= -2 && x <= -2 + leftWidth) {
                tabUnderMouse = LEFT_PAGE;
            }

            // Special case >.
            if (x >= finalBegin && x <= finalBegin + rightWidth) {
                tabUnderMouse = RIGHT_PAGE;
            }

            // Special case +.
            if (x >= finalBegin + (rightWidth + 1) && x <= finalBegin + (rightWidth + 1) + plusWidth) {
                tabUnderMouse = ADD_TAB;
            }

            final int offsetX = x - (-2 + leftWidth + 1);
            final int section = offsetX / (tabWidth + 1);
            if (offsetX >= 0 && section < 5) {
                tabUnderMouse = (offsetX % (tabWidth + 1) == tabWidth) ? -1 : section;
            }
        }

        // Mark the current tab read (there could have been new messages since the last switch).
        tabs.get(activeTab).markRead();
    }

    /**
     * Called when a chat message is received.
     *
     * @param chatComponent The chat message.
     * @param chatLineId The chat line.
     */
    public void printChatMessageWithOptionalDeletion(final IChatComponent chatComponent, final int chatLineId) {
        final String plainMessageWithColors = chatComponent.getUnformattedText();
        final StringBuilder plainBuilder = new StringBuilder();
        boolean isEscape = false;
        for (final char c : plainMessageWithColors.toCharArray()) {
            if (c == '§') {
                isEscape = true;
            } else {
                if (!isEscape) {
                    plainBuilder.append(c);
                }
                isEscape = false;
            }
        }
        final String plainMessage = plainBuilder.toString();

        for (final ChatTab tab : tabs.values()) {
            if (tab.acceptsMessage(plainMessage)) {
                tab.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
            }
        }
    }

    /**
     * Creates a new tab.
     *
     * @param title The title of the tab.
     * @param pattern The pattern the tab listens for.
     * @param literal Whether the pattern is literal.
     * @param prefix The prefix for sent chat messages.
     */
    public void createTab(final String title, final String pattern, final boolean literal, final boolean whitelist,
                          final String prefix) {
        tabs.put(title, new ChatTab(Minecraft.getMinecraft(), pattern, literal, whitelist, prefix));
    }

    /**
     * Returns whether the given tab exists.
     *
     * @param tab The tab's name.
     *
     * @return Whether it exists.
     */
    public boolean doesTabExist(final String tab) {
        return tabs.containsKey(tab);
    }

    /**
     * Saves the tab configurations to the save file.
     */
    public void saveState() {
        try {
            FileUtil.writeLineStorage(3, saveFile, getExportIterator());
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
    }

    /**
     * Loads the tab configurations from the save file.
     */
    private void loadState() {
        try {
            FileUtil.readLineStorage(saveFile, (line, lineNo) -> {
                // Avoid trimming of the array by adding a high limit.
                final String[] split = line.split("§", 99);
                createTab(split[0], split[1], Boolean.parseBoolean(split[2]), Boolean.parseBoolean(split[4]), split[3]);
            }, (version, line) -> {
                int newVersion = version;
                String newLine = line;
                if (newVersion == 1) { // Converter: v1 -> v2
                    // Change: Added prefix as last split token.
                    newLine += "§";
                    newVersion++;
                }
                if (newVersion == 2) { // Converter: v2 -> v3
                    // Change: Added whitelist flag.
                    newLine += "§true";
                    // newVersion++; // Only need this when converting between multiple versions.
                }
                return newLine;
            });
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
    }

    /**
     * Resets the selected tab to the first tab.
     */
    private void resetSelectedTab() {
        activeTab = tabs.keySet().stream().findFirst().get();
        tabOffset = 0;
        tabUnderMouse = -1;
    }

    /**
     * Returns the currently active tab.
     *
     * @return The currently active tab.
     */
    public ChatTab getActiveChat() {
        return tabs.get(activeTab);
    }

    /**
     * Returns a collection of all existing chats.
     *
     * @return All existing chats.
     */
    public Collection<? extends GuiNewChat> getAllChats() {
        return tabs.values();
    }

    /**
     * Returns an iterator iterating over export strings of tabs.
     *
     * @return An iterator over all export strings.
     */
    private Iterator<String> getExportIterator() {
        return tabs.entrySet().stream().map(entry -> entry.getKey() + "§" + entry.getValue().getExport()).iterator();
    }

    /**
     * Adds a default tab.
     */
    private void addDefaultTab() {
        tabs.put("General", new ChatTab(Minecraft.getMinecraft(), ".*", false, true, ""));
    }
}
