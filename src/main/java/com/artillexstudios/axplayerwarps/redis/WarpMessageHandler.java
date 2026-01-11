package com.artillexstudios.axplayerwarps.redis;

import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.warps.Warp;
import com.artillexstudios.axplayerwarps.warps.WarpManager;
import com.nickax.redisplayerlist.server.api.RedisPlayerListServerAPI;
import com.nickax.yadro.core.messaging.MessageHandler;
import org.bukkit.Bukkit;

import java.util.UUID;

public class WarpMessageHandler implements MessageHandler {

    @Override
    public void handle(String channel, String message) {
        String[] parts = message.split(":");
        if (parts.length == 0) return;
        
        String type = parts[0];
        String currentServer = RedisPlayerListServerAPI.getServerId();
        
        switch (type) {
            case "validate_req" -> {
                if (parts.length < 7) return; // Updated to 7 for noConfirm
                String originServer = parts[1];
                String targetServer = parts[2];
            
                if (!currentServer.equalsIgnoreCase(targetServer)) return;

                UUID requestId = UUID.fromString(parts[3]);
                UUID playerUuid = UUID.fromString(parts[4]);
                String warpName = parts[5];
                boolean noConfirm = Boolean.parseBoolean(parts[6]);

                Warp warp = WarpManager.getWarps().stream().filter(w -> w.getName().equalsIgnoreCase(warpName)).findFirst().orElse(null);
                if (warp == null) {
                    return;
                }

                // Run full validation on the target server
                Bukkit.getScheduler().runTask(AxPlayerWarps.getInstance(), () -> warp.validateTeleportRemote(playerUuid, noConfirm, (allowed, errorKey) -> {
                    if (allowed) {
                        sendResponse(originServer, requestId, "SUCCESS");
                    } else {
                        sendResponse(originServer, requestId, "ERROR:" + errorKey);
                    }
                }));
            }
            case "validate_res" -> {
                if (parts.length < 4) return;
                String targetServer = parts[1];
                if (!currentServer.equalsIgnoreCase(targetServer)) return;

                UUID requestId = UUID.fromString(parts[2]);
                String result = parts[3] + (parts.length > 4 ? ":" + parts[4] : "");
                Warp.handleValidationResponse(requestId, result);
            }
            case "teleport" -> {
                if (parts.length < 4) return;
                String targetServer = parts[1];
                if (!currentServer.equalsIgnoreCase(targetServer)) return;

                UUID playerUuid = UUID.fromString(parts[2]);
                String warpName = parts[3];

                Warp.addPendingTeleport(playerUuid, warpName);
            }
            case "warp_delete" -> {
                if (parts.length < 3) return;
                String senderServer = parts[2];
                if (currentServer.equalsIgnoreCase(senderServer)) return;

                int warpId = Integer.parseInt(parts[1]);
                WarpManager.getWarps().removeIf(w -> w.getId() == warpId);
            }
            case "warp_create", "warp_update" -> {
                if (parts.length < 3) return;
                String senderServer = parts[2];
                // Skip if we are the ones who sent the update
                if (currentServer.equalsIgnoreCase(senderServer)) return;

                int warpId = Integer.parseInt(parts[1]);
                WarpManager.reloadWarp(warpId);
            }
        }
    }

    private void sendResponse(String targetServer, UUID requestId, String result) {
        String msg = String.format("validate_res:%s:%s:%s", targetServer, requestId, result);
        AxPlayerWarps.PUBLISHER.publish(AxPlayerWarps.REDIS_CHANNEL, msg);
    }
}