package net.paulem.launchermc.discord;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import fr.flowarg.flowlogger.ILogger;
import net.paulem.launchermc.gson.GsonUtils;
import net.paulem.launchermc.gson.types.MinetoolsServer;
import net.paulem.launchermc.utils.Constants;

public final class RPC {
    private DiscordRPC lib;
    private DiscordRichPresence presence;
    private final ILogger logger;

    public RPC(ILogger logger) {
        this.logger = logger;
    }

    public void startPresence() {
        logger.info("Started RPC!");

        lib = DiscordRPC.INSTANCE;

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        handlers.ready = (user) -> logger.info("Connecté à Discord avec " + user.username + " !");
        lib.Discord_Initialize(Constants.RPC_APP_ID, handlers, true, null);

        defaultPresence();

        lib.Discord_UpdatePresence(presence);

        // in a worker thread
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if(isConnected(presence.details)) {
                    updateServerInfos();
                }

                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
            }
        }, "RPC-Callback-Handler").start();
    }

    public void editPresence(String details) {
        if(isConnected(details)) {
            presence.partyId = Constants.RPC_PARTY_ID;
            updateServerInfos();
        } else {
            defaultPresence();
        }

        presence.details = details;

        lib.Discord_UpdatePresence(presence);
    }

    public boolean isConnected(String details) {
        return details.equals(Constants.RPC_CONNECTED);
    }

    public void updateServerInfos() {
        try {
            MinetoolsServer server = GsonUtils.parseJson(MinetoolsServer.class);

            int onlinePlayers = server.players().online();

            if(onlinePlayers == 0) {
                presence.state = Constants.RPC_STATE_NO_PLAYERS;
            } else presence.state = Constants.RPC_STATE_PLAYERS;

            presence.partySize = onlinePlayers;
            presence.partyMax = server.players().max();

            presence.largeImageText = server.description();

            lib.Discord_UpdatePresence(presence);
        } catch (Exception e) {
            /*this.logger.err("Impossible de récupérer les informations sur le serveur !");
            this.logger.printStackTrace(e);*/
        }
    }

    public void defaultPresence() {
        presence = new DiscordRichPresence();

        presence.startTimestamp = System.currentTimeMillis() / 1000; // epoch second
        presence.details = Constants.RPC_LAUNCHER;
        presence.largeImageKey = Constants.RPC_LARGE_IMG_KEY;
    }
}
