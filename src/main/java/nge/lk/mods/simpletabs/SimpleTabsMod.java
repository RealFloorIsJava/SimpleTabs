package nge.lk.mods.simpletabs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import nge.lk.mods.commonlib.util.DebugUtil;
import nge.lk.mods.simpletabs.tabs.TabManager;
import org.lwjgl.input.Mouse;

import java.io.File;

/**
 * Main mod class.
 */
@Mod(modid = SimpleTabsMod.MODID, version = SimpleTabsMod.VERSION, clientSideOnly = true)
public class SimpleTabsMod {

    /**
     * The ID of the mod.
     */
    public static final String MODID = "simpletabs";

    /**
     * The version of the mod.
     */
    public static final String VERSION = "@VERSION@";

    /**
     * Whether the chat already was replaced by the mod's chat.
     */
    private boolean chatReplaced;

    /**
     * The tab manager.
     */
    private TabManager tabManager;

    /**
     * The storage file for tab configurations.
     */
    private File tabStorageFile;

    @EventHandler
    public void onPreInit(final FMLPreInitializationEvent event) {
        DebugUtil.initializeLogger(MODID);
        tabStorageFile = new File(event.getModConfigurationDirectory(), "simpletabs.dat");
    }

    @EventHandler
    public void onInit(final FMLInitializationEvent event) {
        tabManager = new TabManager(tabStorageFile);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(final PlayerTickEvent event) {
        if (!chatReplaced) {
            chatReplaced = true;

            // Field 'persistantChatGUI' of 'GuiIngame'
            ReflectionHelper.setPrivateValue(GuiIngame.class, Minecraft.getMinecraft().ingameGUI,
                    new GuiTabChat(Minecraft.getMinecraft(), tabManager), 6);
        }
    }

    @SubscribeEvent
    public void onMouse(final Pre event) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) {
            if (!Mouse.getEventButtonState()) {
                return;
            }
            final int mouseButton = Mouse.getEventButton();
            tabManager.handleClick(mouseButton);
        }
    }

    @SubscribeEvent
    public void onChat(final ClientChatEvent event) {
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) {
            if (!event.getMessage().startsWith("/")) {
                final String prefix = tabManager.getActivePrefix();
                event.setMessage(prefix + event.getMessage());
                if (event.getMessage().length() > 256) {
                    event.setMessage(event.getMessage().substring(0, 256));
                }
            }
        }
    }
}
