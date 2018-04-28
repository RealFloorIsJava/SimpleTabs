package nge.lk.mods.simpletabs.tabs;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import nge.lk.mods.commonlib.util.DebugUtil;
import nge.lk.mods.commonlib.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Handles IO of tabs.
 */
@RequiredArgsConstructor
public class TabIO {

    /**
     * The file where the tab configurations are stored in.
     */
    private final File saveFile;

    /**
     * Returns an iterator iterating over export strings of tabs.
     *
     * @param tabs The tabs which should be iterated over.
     *
     * @return An iterator over all export strings.
     */
    private static Iterator<String> getExportIterator(final Iterable<Map<String, ChatTab>> tabs) {
        final Collection<String> mapped = new LinkedList<>();
        int i = 0;
        for (final Map<String, ChatTab> tabMap : tabs) {
            for (final Entry<String, ChatTab> tab : tabMap.entrySet()) {
                mapped.add(i + "§" + tab.getKey() + "§" + tab.getValue().getExport());
            }
            i++;
        }
        return mapped.iterator();
    }

    /**
     * Saves the tab configurations to the save file.
     */
    public void saveState(final Iterable<Map<String, ChatTab>> tabs) {
        try {
            FileUtil.writeLineStorage(4, saveFile, getExportIterator(tabs));
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
    }

    /**
     * Loads the tab configurations from the save file.
     */
    public List<Map<String, ChatTab>> loadState() {
        final List<Map<String, ChatTab>> tabResults = new ArrayList<>();
        try {
            FileUtil.readLineStorage(saveFile, new TabBuilder(tabResults), new TabVersionConverter());
        } catch (final IOException e) {
            DebugUtil.recoverableError(e);
        }
        return tabResults;
    }

    /**
     * Converts tab data between different versions.
     */
    private static class TabVersionConverter implements BiFunction<Integer, String, String> {

        @Override
        public String apply(final Integer version, final String line) {
            int newVersion = version;
            String newLine = line;

            if (newVersion == 1) { // Converter: v1 -> v2
                // Change: Added prefix as last split token.
                newLine += "§";
                newVersion++;
            }

            if (newVersion == 2) { // Converter: v2 -> v3
                // Change: Added whitelist flag and tab groups.
                newLine += "§true";
                newLine = "0§" + newLine;
                newVersion++;
            }

            if (newVersion == 3) { // Converter: v3 -> v4
                // Change: Added notify flag.
                newLine += "§false";
                // newVersion++; // Only need this when converting between more versions.
            }

            return newLine;
        }
    }

    /**
     * Builds tabs from line data.
     */
    @RequiredArgsConstructor
    private static class TabBuilder implements BiConsumer<String, Integer> {

        private final List<Map<String, ChatTab>> results;
        /**
         * The current group.
         */
        private String currentGroup;

        @Override
        public void accept(final String line, final Integer lineNo) {
            // Avoid trimming of the array by adding a high limit.
            final String[] split = line.split("§", 99);

            // Advance the group, if needed.
            if (!split[0].equals(currentGroup)) {
                results.add(new LinkedHashMap<>());
                currentGroup = split[0];
            }

            // Create the tab.
            final String tabName = split[1];
            final String pattern = split[2];
            final boolean literal = Boolean.parseBoolean(split[3]);
            final String prefix = split[4];
            final boolean whitelist = Boolean.parseBoolean(split[5]);
            final boolean notify = Boolean.parseBoolean(split[6]);
            results.get(results.size() - 1).put(tabName,
                    new ChatTab(Minecraft.getMinecraft(), pattern, literal, whitelist, notify, prefix));
        }
    }
}
