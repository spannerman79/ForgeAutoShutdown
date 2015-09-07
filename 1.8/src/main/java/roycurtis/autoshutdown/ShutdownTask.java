package roycurtis.autoshutdown;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Logger;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton that acts as a timer task and an event handler for daily shutdown.
 *
 * The use of a tick handler ensures the shutdown process is run in the main thread,
 * to prevent issues with cross-thread contamination. As the handler runs 40 times a
 * second, the event is just a boolean check. This means the scheduled task's role is
 * to unlock the tick handler.
 */
public class ShutdownTask extends TimerTask
{
    static final Format DATE = new SimpleDateFormat("HH:mm MMM d");

    private static ShutdownTask    INSTANCE;
    private static MinecraftServer SERVER;
    private static Logger          LOGGER;

    /** Creates a timer task to run at the configured time of day */
    public static void create()
    {
        if (INSTANCE != null)
            throw new RuntimeException("ShutdownTask can only be created once");

        INSTANCE = new ShutdownTask();
        SERVER   = MinecraftServer.getServer();
        LOGGER   = ForgeAutoShutdown.LOGGER;

        Timer    timer      = new Timer("ForgeAutoShutdown timer");
        Calendar shutdownAt = Calendar.getInstance();
        shutdownAt.set(Calendar.HOUR_OF_DAY, Config.scheduleHour);
        shutdownAt.set(Calendar.MINUTE, Config.scheduleMinute);
        shutdownAt.set(Calendar.SECOND, 0);

        // Adjust for when current time surpasses shutdown schedule
        // (e.g. if shutdown time is 07:00 and current time is 13:21)
        if ( shutdownAt.before( Calendar.getInstance() ) )
            shutdownAt.add(Calendar.DAY_OF_MONTH, 1);

        Date shutdownAtDate = shutdownAt.getTime();

        timer.schedule(INSTANCE, shutdownAtDate, 60 * 1000);
        LOGGER.info( "Next automatic shutdown: %s", DATE.format(shutdownAtDate) );
    }

    boolean executeTick  = false;
    Byte    warningsLeft = 5;

    @Override
    /**
     * Tick handler is registered on first timer call, so as not to have a useless
     * handler running every tick for the server's lifetime
     */
    public void run()
    {
        // Safe call from timer thread; event bus collection is ConcurrentHashMap
        FMLCommonHandler.instance().bus().register(this);
        executeTick = true;
        LOGGER.debug("Timer called; next ShutdownTask tick will run");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerTick(TickEvent.ServerTickEvent event)
    {
        // Refrain from running at the end of server ticking
        if (!executeTick || event.phase == TickEvent.Phase.END)
            return;

        if (warningsLeft == 0)
            Util.performShutdown(Config.msgKick);
        else
            performWarning();

        executeTick = false;
        LOGGER.debug("ShutdownTask ticked; %d warnings to go", warningsLeft);
    }

    private ShutdownTask() { }

    void performWarning()
    {
        String warning = Config.msgWarn.replace( "%m", warningsLeft.toString() );

        Util.broadcast(SERVER, "*** " + warning);
        LOGGER.info(warning);
        warningsLeft--;
    }
}