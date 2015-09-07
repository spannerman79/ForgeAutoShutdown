package roycurtis.autoshutdown;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import org.apache.logging.log4j.Logger;

/**
 * Static class for utility functions (syntactic sugar)
 */
class Util
{
    static void performShutdown(String reason)
    {
        MinecraftServer SERVER = MinecraftServer.getServer();
        Logger          LOGGER = ForgeAutoShutdown.LOGGER;

        reason = Util.translate(reason);

        for ( Object value : SERVER.getConfigurationManager().playerEntityList.toArray() )
        {
            EntityPlayerMP player = (EntityPlayerMP) value;
            player.playerNetServerHandler.kickPlayerFromServer(reason);
        }

        LOGGER.debug("Shutdown initiated because: %s", reason);
        SERVER.initiateShutdown();
    }

    /**
     * Attempts to a translate a given string/key using the local language, and then
     * using the fallback language.
     * @param msg String or language key to translate
     * @return Translated or same string
     */
    static String translate(String msg)
    {
        return StatCollector.canTranslate(msg)
            ? StatCollector.translateToLocal(msg)
            : StatCollector.translateToFallback(msg);
    }

    /**
     * Broadcasts an auto. translated, formatted encapsulated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    static void broadcast(MinecraftServer server, String msg, Object... parts)
    {
        server.getConfigurationManager()
            .sendChatMsg( prepareText(msg, parts) );
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a player
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    static void chat(ICommandSender sender, String msg, Object... parts)
    {
        sender.addChatMessage( prepareText(msg, parts) );
    }

    private static IChatComponent prepareText(String msg, Object... parts)
    {
        String translated = translate(msg);
        String formatted  = String.format(translated, parts);

        return new ChatComponentText(formatted);
    }
}