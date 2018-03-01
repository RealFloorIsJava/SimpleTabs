package nge.lk.mods.simpletabs;

import net.minecraft.client.gui.GuiScreen;
import nge.lk.mods.commonlib.gui.factory.GuiFactory;
import nge.lk.mods.commonlib.gui.factory.Positioning;
import nge.lk.mods.commonlib.gui.factory.element.ButtonElement;
import nge.lk.mods.commonlib.gui.factory.element.InputElement;
import nge.lk.mods.commonlib.gui.factory.element.TextElement;
import nge.lk.mods.simpletabs.tabs.ChatTab;
import nge.lk.mods.simpletabs.tabs.TabManager;

import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * An editor for creating or changing tabs.
 */
public class GuiTabEditor extends GuiFactory implements Consumer<ButtonElement> {

    /**
     * The tab that is edited or null if the tab is new.
     */
    private final ChatTab editingTab;

    /**
     * The tab manager which owns the tabs.
     */
    private final TabManager tabManager;

    /**
     * The preset for the tab name.
     */
    private final String titlePreset;

    /**
     * The parent screen.
     */
    private GuiScreen parentScreen;

    /**
     * The input field for the name.
     */
    private InputElement nameElement;

    /**
     * The input field for the pattern.
     */
    private InputElement patternElement;

    /**
     * The input field for the prefix.
     */
    private InputElement prefixElement;

    /**
     * The button for enabling expert mode.
     */
    private ButtonElement expertModeButton;

    /**
     * The button for switching between whitelist and blacklist.
     */
    private ButtonElement whitelistButton;

    /**
     * The button to save the tab and return to the parent.
     */
    private ButtonElement saveButton;

    /**
     * The button to save the tab and return back ingame.
     */
    private ButtonElement saveCloseButton;

    /**
     * The button to discard changes to the tab and return to the parent.
     */
    private ButtonElement backButton;

    /**
     * The caption for the pattern input.
     */
    private TextElement patternCaption;

    /**
     * Constructor.
     *
     * @param parentScreen The parent screen.
     * @param tabManager The tab manager owning the tabs.
     * @param editingTab The currently edited tab, or {@code null}.
     * @param titlePreset The preset for the title.
     */
    public GuiTabEditor(final GuiScreen parentScreen, final TabManager tabManager, final ChatTab editingTab,
                        final String titlePreset) {
        this.parentScreen = parentScreen;
        this.editingTab = editingTab;
        this.titlePreset = titlePreset;
        this.tabManager = tabManager;
        createGui();
    }

    @Override
    public void accept(final ButtonElement buttonElement) {
        if (buttonElement == backButton) {
            closeGui();
        } else if (buttonElement == saveButton || buttonElement == saveCloseButton) {
            if (editingTab != null) {
                // Save the changes.
                editingTab.updatePattern(patternElement.getTextField().getText(),
                        !((Boolean) expertModeButton.getMetadata()), ((Boolean) whitelistButton.getMetadata()));
                editingTab.setPrefix(prefixElement.getTextField().getText());
            } else {
                // Create a new tab.
                tabManager.createTab(
                        nameElement.getTextField().getText(),
                        patternElement.getTextField().getText(),
                        !((Boolean) expertModeButton.getMetadata()),
                        ((Boolean) whitelistButton.getMetadata()),
                        prefixElement.getTextField().getText()
                );
            }
            tabManager.saveState();

            if (buttonElement == saveCloseButton) {
                parentScreen = null;
            }
            closeGui();
        } else if (buttonElement == expertModeButton) {
            // Toggle the button's state which is stored in the metadata.
            expertModeButton.setMetadata(!((Boolean) expertModeButton.getMetadata()));

            // Update visuals to reflect the change.
            updateCaptions();
        } else if (buttonElement == whitelistButton) {
            // Toggle the button's state which is stored in the metadata.
            whitelistButton.setMetadata(!((Boolean) whitelistButton.getMetadata()));

            // Update visuals to reflect the change.
            updateCaptions();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        boolean canSave = !nameElement.getTextField().getText().isEmpty()
                && !patternElement.getTextField().getText().isEmpty();

        // Pattern check.
        try {
            final int patternFlags = ((Boolean) expertModeButton.getMetadata()) ? 0 : Pattern.LITERAL;
            Pattern.compile(patternElement.getTextField().getText(), patternFlags);
        } catch (final PatternSyntaxException ignored) {
            canSave = false;
        }

        // Duplicate name check.
        if (tabManager.doesTabExistInActiveGroup(nameElement.getTextField().getText()) && editingTab == null) {
            canSave = false;
        }

        saveButton.getButton().enabled = canSave;
        saveCloseButton.getButton().enabled = canSave;
    }

    @Override
    protected void createGui() {
        setPadding(0.05, 0.05, 0.1, 0.05);

        addText(new Positioning().center()).setText("Tab Editor", 0xA0A0A0);
        addBlank(new Positioning().breakRow().absoluteHeight(15));

        addText(new Positioning().breakRow()).setText("Tab Name", 0xA0A0A0);
        nameElement = addInput(new Positioning().relativeWidth(40).absoluteHeight(20));
        nameElement.getTextField().setMaxStringLength(8); // See also TabManager#MAXIMUM_TAB_NAME
        nameElement.getTextField().setText(titlePreset != null ? titlePreset : "");
        nameElement.getTextField().setEnabled(editingTab == null);
        nameElement.getTextField().setCursorPositionZero();
        addBlank(new Positioning().relativeWidth(4));

        expertModeButton = addButton(this,
                new Positioning().relativeWidth(40).absoluteHeight(20).breakRow());
        expertModeButton.setMetadata(editingTab != null && !editingTab.isLiteral());
        addBlank(new Positioning().breakRow().absoluteHeight(10));

        patternCaption = addText(new Positioning().breakRow());
        patternElement = addInput(new Positioning().relativeWidth(40).absoluteHeight(20));
        patternElement.getTextField().setMaxStringLength(1024);
        patternElement.getTextField().setText(editingTab != null ? editingTab.getPattern() : "");
        patternElement.getTextField().setCursorPositionZero();
        addBlank(new Positioning().relativeWidth(4));

        whitelistButton = addButton(this,
                new Positioning().relativeWidth(40).absoluteHeight(20).breakRow());
        whitelistButton.setMetadata(editingTab == null || editingTab.isWhitelist());
        addBlank(new Positioning().breakRow().absoluteHeight(10));

        // At this point, all elements having captions are created.
        updateCaptions();

        addText(new Positioning().breakRow()).setText("Tab Prefix (for messages sent in this tab)",
                0xA0A0A0);
        prefixElement = addInput(new Positioning().relativeWidth(85).absoluteHeight(20).breakRow());
        prefixElement.getTextField().setMaxStringLength(255);
        prefixElement.getTextField().setText(editingTab != null ? editingTab.getPrefix() : "");
        prefixElement.getTextField().setCursorPositionZero();
        addBlank(new Positioning().breakRow().absoluteHeight(10));

        addText(new Positioning().breakRow()).setText("You can delete tabs by middle clicking them",
                0xA0A0A0);
        addText(new Positioning().breakRow()).setText("You can edit existing tabs by right clicking them",
                0xA0A0A0);
        addText(new Positioning()).setText("Don't use expert mode unless you understand regular expressions!",
                0xA0A0A0);

        saveButton = addButton(this, new Positioning().alignBottom().relativeWidth(27).absoluteHeight(20));
        saveButton.getButton().displayString = "Save & Back";
        saveButton.getButton().enabled = false;
        addBlank(new Positioning().alignBottom().relativeWidth(3));

        saveCloseButton = addButton(this,
                new Positioning().alignBottom().relativeWidth(27).absoluteHeight(20));
        saveCloseButton.getButton().displayString = "Save & Close";
        saveCloseButton.getButton().enabled = false;
        addBlank(new Positioning().alignBottom().relativeWidth(3));

        backButton = addButton(this, new Positioning().alignBottom().relativeWidth(27).absoluteHeight(20));
        backButton.getButton().displayString = parentScreen == null ? "Close" : "Back";
    }

    @Override
    protected void closeGui() {
        mc.displayGuiScreen(parentScreen);
    }

    /**
     * Updates the captions of the tab pattern input and the toggle buttons.
     */
    private void updateCaptions() {
        final String indicator = ((Boolean) expertModeButton.getMetadata()) ? "On" : "Off";
        expertModeButton.getButton().displayString = "Expert Mode: " + indicator;

        whitelistButton.getButton().displayString =
                ((Boolean) whitelistButton.getMetadata()) ? "Whitelist" : "Blacklist";

        if ((Boolean) expertModeButton.getMetadata()) {
            // Provide expert caption.
            patternCaption.setText("Tab Pattern (regular expression)", 0xA0A0A0);
        } else {
            // Provide simple caption.
            patternCaption.setText("Tab Pattern (keyword for this tab)", 0xA0A0A0);
        }
    }
}
