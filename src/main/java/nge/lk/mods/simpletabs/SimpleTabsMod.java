package nge.lk.mods.simpletabs;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
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
    public void clientConnectedToServer(final FMLNetworkEvent.ClientConnectedToServerEvent event) {
        // Inject the packet filter into the queue for this server connection
        event.manager.channel().pipeline().addBefore("fml:packet_handler",
                "SimpleTabsInterceptOutbound", new OutboundInterceptor(tabManager));
    }

    /**
     * Interceptor for outgoing chat to add tab prefixes.
     */
    @RequiredArgsConstructor
    private static class OutboundInterceptor extends ChannelOutboundHandlerAdapter {

        private final TabManager tabManager;

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
            if (!(msg instanceof ByteBuf)) {
                if (msg instanceof C01PacketChatMessage) {
                    final C01PacketChatMessage chatMessage = (C01PacketChatMessage) msg;
                    if (!chatMessage.getMessage().startsWith("/")) {
                        final String prefix = tabManager.getActivePrefix();
                        chatMessage.message = prefix + chatMessage.message;
                        if (chatMessage.message.length() > 100) {
                            chatMessage.message = chatMessage.message.substring(0, 100);
                        }
                    }
                }
            }
            ctx.write(msg, promise);
        }
    }
}
