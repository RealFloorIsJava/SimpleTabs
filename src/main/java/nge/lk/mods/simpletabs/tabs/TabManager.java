package nge.lk.mods.simpletabs.tabs;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import nge.lk.mods.simpletabs.GuiTabEditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The owner of all tabs, managing the tab configurations.
 */
public class TabManager {

    /**
     * The existing tabs in a {@code Name -> Tab} mapping, one for each tab group.
     */
    private final List<Map<String, ChatTab>> tabs;

    /**
     * The IO manager.
     */
    private final TabIO tabIO;

    /**
     * The display manager.
     */
    private final TabDisplay tabDisplay;

    /**
     * The active tab group.
     */
    @Getter private int activeGroup;

    /**
     * The currently active tab.
     */
    private String activeTab;

    /**
     * The offset of the leftmost tab.
     */
    @Getter private int tabOffset;

    /**
     * Constructor.
     *
     * @param saveFile The file where tab configurations are saved in.
     */
    public TabManager(final File saveFile) {
        tabIO = new TabIO(saveFile);
        tabDisplay = new TabDisplay(this);

        tabs = new ArrayList<>();
        tabs.add(new LinkedHashMap<>());

        loadState();

        activeGroup = 0;
        if (tabs.isEmpty()) {
            // Only add the default tab if there is no tab group whatsoever.
            tabs.add(new LinkedHashMap<>());
            addDefaultTab();
        }

        // At this point, all tab groups must not be empty.
        activeTab = tabs.get(activeGroup).keySet().stream().findFirst().get();
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

        for (final Map<String, ChatTab> tabMap : tabs) {
            for (final ChatTab tab : tabMap.values()) {
                if (tab.acceptsMessage(plainMessage)) {
                    tab.printChatMessageWithOptionalDeletion(chatComponent, chatLineId);
                }
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
        tabs.get(activeGroup).put(title, new ChatTab(Minecraft.getMinecraft(), pattern, literal, whitelist, prefix));
    }

    /**
     * Returns whether the given tab exists.
     *
     * @param tab The tab's name.
     *
     * @return Whether it exists.
     */
    public boolean doesTabExistInActiveGroup(final String tab) {
        return tabs.get(activeGroup).containsKey(tab);
    }

    /**
     * Saves the tab configurations to the save file.
     */
    public void saveState() {
        tabIO.saveState(tabs);
    }

    /**
     * Shows the previous tab page.
     */
    public void previousTabPage() {
        tabOffset = Math.max(0, tabOffset - TabDisplay.TABS_PER_PAGE);
    }

    /**
     * Shows the next tab page.
     */
    public void nextTabPage() {
        final int tabAmount = tabs.get(activeGroup).size();
        if (tabOffset + TabDisplay.TABS_PER_PAGE < tabAmount) {
            tabOffset += TabDisplay.TABS_PER_PAGE;
        }
    }

    /**
     * Cycles the current tab group.
     */
    public void cycleTabGroup() {
        if (activeGroup + 1 < tabs.size()) {
            activeGroup++;
        } else {
            assert activeGroup + 1 == tabs.size() : "invalid current group";
            // Either the first group is selected, or a new group is created.
            // A new group is only created if the current group is changed in any way.
            final Map<String, ChatTab> group = tabs.get(activeGroup);
            if (group.size() == 1) {
                final String tabName = group.keySet().stream().findFirst().get();
                if (tabName.equals("General")) {
                    final ChatTab tab = group.values().stream().findFirst().get();
                    if (!tab.isLiteral() && tab.isWhitelist() && tab.getPattern().equals(".*")
                            && tab.getPrefix().isEmpty()) {
                        activeGroup = 0;
                        return;
                    }
                }
            }
            activeGroup++;
            tabs.add(new LinkedHashMap<>());
            addDefaultTab();
        }
    }

    /**
     * Checks whether the currently active tab has the given name.
     *
     * @param name The name.
     *
     * @return Whether the name is the name of the currently active tab.
     */
    public boolean isTabActive(final String name) {
        return name.equals(activeTab);
    }

    /**
     * Opens the editor to create a new tab.
     */
    public void editTab(final String tabName, final ChatTab tab) {
        Minecraft.getMinecraft().displayGuiScreen(new GuiTabEditor(Minecraft.getMinecraft().currentScreen,
                this, tab, tabName));
    }

    /**
     * Makes the tab with the given name active.
     *
     * @param key The tab name.
     */
    public void makeTabActive(final String key) {
        if (doesTabExistInActiveGroup(key)) {
            activeTab = key;
        }
        // Invariant: The current tab is valid, so no change is needed even if the tab name doesn't exist.
    }

    /**
     * Deletes the tab with the given name.
     *
     * @param key The name.
     */
    public void deleteTab(final String key) {
        if (doesTabExistInActiveGroup(key)) {
            tabs.get(activeGroup).remove(key);
        }
    }

    public void resetSelectedTab() {
        activeTab = tabs.get(activeGroup).isEmpty() ? null : tabs.get(activeGroup).keySet().stream().findFirst().get();
        tabOffset = 0;
    }

    /**
     * Updates the selected tab.
     */
    public void updateTabs(final float chatScale) {
        tabDisplay.updateTabs(chatScale);
    }

    /**
     * Draws the labels of the tabs.
     */
    public void drawTabLabels() {
        tabDisplay.drawTabLabels();
    }

    /**
     * Handles a click in the chat.
     *
     * @param mouseButton The mouse button.
     */
    public void handleClick(final int mouseButton) {
        tabDisplay.handleClick(mouseButton);
    }

    /**
     * Loads the tab configurations from the save file.
     */
    private void loadState() {
        tabs.clear();
        tabs.addAll(tabIO.loadState());
    }

    /**
     * Adds a default tab.
     */
    private void addDefaultTab() {
        tabs.get(activeGroup).put("General", new ChatTab(Minecraft.getMinecraft(), ".*", false,
                true, ""));
    }

    /**
     * Get the tabs from the currently active tab group.
     *
     * @return The tabs.
     */
    public Collection<Entry<String, ChatTab>> getActiveTabGroup() {
        return Collections.unmodifiableCollection(tabs.get(activeGroup).entrySet());
    }

    /**
     * Returns the currently active tab.
     *
     * @return The currently active tab.
     */
    public ChatTab getActiveChat() {
        if (!tabs.get(activeGroup).containsKey(activeTab)) {
            return null;
        }
        return tabs.get(activeGroup).get(activeTab);
    }

    /**
     * Fetches the prefix of the active tab.
     *
     * @return The prefix.
     */
    public String getActivePrefix() {
        if (getActiveChat() != null) {
            return getActiveChat().getPrefix();
        }
        return "";
    }

    /**
     * Returns a collection of all existing chats.
     *
     * @return All existing chats.
     */
    public Collection<GuiNewChat> getAllChats() {
        final Collection<GuiNewChat> chats = new LinkedList<>();
        for (final Map<String, ChatTab> tabMap : tabs) {
            chats.addAll(tabMap.values());
        }
        return chats;
    }
}
