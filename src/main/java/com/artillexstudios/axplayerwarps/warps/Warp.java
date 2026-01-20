package com.artillexstudios.axplayerwarps.warps;

import com.artillexstudios.axapi.placeholders.PlaceholderHandler;
import com.artillexstudios.axapi.scheduler.Scheduler;
import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axapi.utils.PaperUtils;
import com.artillexstudios.axplayerwarps.AxPlayerWarps;
import com.artillexstudios.axplayerwarps.category.Category;
import com.artillexstudios.axplayerwarps.database.impl.Base;
import com.artillexstudios.axplayerwarps.enums.Access;
import com.artillexstudios.axplayerwarps.enums.AccessList;
import com.artillexstudios.axplayerwarps.hooks.currency.CurrencyHook;
import com.artillexstudios.axplayerwarps.placeholders.WarpPlaceholders;
import com.nickax.redisplayerlist.server.api.RedisPlayerListServerAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.artillexstudios.axplayerwarps.AxPlayerWarps.CONFIG;
import static com.artillexstudios.axplayerwarps.AxPlayerWarps.MESSAGEUTILS;

public class Warp {

    private static final ConcurrentHashMap<UUID, Consumer<String>> pendingValidations = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> pendingTeleports = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> pendingTeleportOriginServers = new ConcurrentHashMap<>();

    private final int id;
    private UUID owner;
    private String ownerName;
    private Location location;
    private String name;
    private @Nullable String description;
    private @Nullable Category category;
    private final long created;
    private Access access;
    private @Nullable CurrencyHook currency;
    private double teleportPrice;
    private double earnedMoney;
    private Material icon;
    private int favorites;
    private HashMap<UUID, Integer> rating = new HashMap<>();
    private int visits;
    private HashSet<UUID> visitors = new HashSet<>();
    private List<Base.AccessPlayer> whitelisted = Collections.synchronizedList(new ArrayList<>());
    private List<Base.AccessPlayer> blacklisted = Collections.synchronizedList(new ArrayList<>());
    private final String worldName;
    private String server;

    public Warp(int id, long created, @Nullable String description, String name,
                Location location, String worldName, @Nullable Category category,
                UUID owner, String ownerName, Access access, @Nullable CurrencyHook currency,
                double teleportPrice, double earnedMoney, @Nullable Material icon, String server
    ) {
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        location.add(0.5, 0, 0.5);
        this.id = id;
        this.created = created;
        this.description = description;
        this.name = name;
        this.location = location;
        this.worldName = worldName;
        this.category = category;
        this.owner = owner;
        this.ownerName = ownerName;
        this.access = access;
        this.currency = currency;
        this.teleportPrice = teleportPrice;
        this.earnedMoney = earnedMoney;
        this.icon = icon;
        this.server = server;

        AxPlayerWarps.getThreadedQueue().submit(() -> {
            favorites = AxPlayerWarps.getDatabase().getFavorites(this);
            rating = AxPlayerWarps.getDatabase().getAllRatings(this);
            visits = AxPlayerWarps.getDatabase().getVisits(this);
            visitors = AxPlayerWarps.getDatabase().getVisitors(this);
            whitelisted = AxPlayerWarps.getDatabase().getAccessList(this, AccessList.WHITELIST);
            blacklisted = AxPlayerWarps.getDatabase().getAccessList(this, AccessList.BLACKLIST);
        });
    }

    public void reload() {
        // reload category and other stuff
    }

    public int getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        setServer(RedisPlayerListServerAPI.getServerId());
    }

    public String getName() {
        return name;
    }

    public boolean setName(String name) {
        if (AxPlayerWarps.getDatabase().warpExists(name)) return false;
        this.name = name;
        return true;
    }

    public String getDescription() {
        if (description == null) {
            return CONFIG.getString("warp-description.default", "");
        }
        return description;
    }

    @Nullable
    public String getRealDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public void setDescription(List<String> description) {
        String newDesc = String.join("\n", description);
        this.description = newDesc.isBlank() ? null : newDesc;
    }

    @Nullable
    public Category getCategory() {
        return category;
    }

    public void setCategory(@Nullable Category category) {
        this.category = category;
    }

    public long getCreated() {
        return created;
    }

    public Access getAccess() {
        return access;
    }

    public void setAccess(Access access) {
        this.access = access;
    }

    @Nullable
    public CurrencyHook getCurrency() {
        return currency;
    }

    public void setCurrency(@Nullable CurrencyHook currency) {
        this.currency = currency;
    }

    public double getTeleportPrice() {
        return teleportPrice;
    }

    public void setTeleportPrice(double teleportPrice) {
        this.teleportPrice = teleportPrice;
    }

    public double getEarnedMoney() {
        return earnedMoney;
    }

    public Material getIcon() {
        if (icon == null) {
            return Material.matchMaterial(CONFIG.getString("default-material", "PLAYER_HEAD"));
        }
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public HashMap<UUID, Integer> getAllRatings() {
        return rating;
    }

    public float getRating() {
        return (float) rating.values().stream().mapToDouble(Integer::doubleValue).average().orElse(0);
    }

    public int getRatingAmount() {
        return rating.size();
    }

    public int getVisits() {
        return visits;
    }

    public void setVisits(int visits) {
        this.visits = visits;
    }

    public HashSet<UUID> getVisitors() {
        return visitors;
    }

    public int getUniqueVisits() {
        return visitors.size();
    }

    public List<Base.AccessPlayer> getBlacklisted() {
        return blacklisted;
    }

    public List<Base.AccessPlayer> getWhitelisted() {
        return whitelisted;
    }

    public List<Base.AccessPlayer> getAccessList(AccessList al) {
        return al == AccessList.WHITELIST ? whitelisted : blacklisted;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public boolean isPaid() {
        return currency != null && teleportPrice > 0;
    }

    public CompletableFuture<Boolean> isDangerous() {
        if (!CONFIG.getBoolean("check-unsafe-warps", true)) {
            return CompletableFuture.completedFuture(false);
        }

        return PaperUtils.getChunkAtAsync(location).thenApply((chunk) -> {
            int x = location.getBlockX() & 15;
            int y = location.getBlockY();
            int z = location.getBlockZ() & 15;
            Block at = chunk.getBlock(x, y, z);
            Block under = at.getRelative(BlockFace.DOWN);
            Block above = at.getRelative(BlockFace.UP);

            if (!at.getType().isAir()) {
                return true;
            }
            if (!above.getType().isAir()) {
                return true;
            }
            return !under.getType().isSolid();
        });
    }

    private final Cooldown<Player> confirmUnsafe = Cooldown.create();
    private final Cooldown<Player> confirmPaid = Cooldown.create();

    public static void handleValidationResponse(UUID requestId, String result) {
        Consumer<String> callback = pendingValidations.remove(requestId);
        if (callback != null) {
            callback.accept(result);
        }
    }

    public static void addPendingTeleport(UUID playerUuid, String warpName) {
        pendingTeleports.put(playerUuid, warpName);
        pendingTeleportOriginServers.remove(playerUuid);
    }

    public static void addPendingTeleportWithOrigin(UUID playerUuid, String warpName, String originServer) {
        pendingTeleports.put(playerUuid, warpName);
        pendingTeleportOriginServers.put(playerUuid, originServer);
    }

    public static void handlePlayerJoin(Player player) {
        String warpName = pendingTeleports.remove(player.getUniqueId());
        String originServer = pendingTeleportOriginServers.remove(player.getUniqueId());
        if (warpName == null) return;

        WarpManager.getWarps().stream()
                .filter(w -> w.getName().equalsIgnoreCase(warpName))
                .findFirst().ifPresent(warp -> warp.completeTeleportPlayer(player, originServer));
    }


    public void teleportPlayer(Player player) {
        String currentServer = RedisPlayerListServerAPI.getServerId();

        if (server != null && !server.equalsIgnoreCase(currentServer)) {
            UUID requestId = UUID.randomUUID();
            boolean noConfirm = confirmPaid.hasCooldown(player) || confirmUnsafe.hasCooldown(player);

            // Format: validate_req:OriginServer:TargetServer:RequestId:PlayerUUID:WarpName:noConfirm
            String message = String.format("validate_req:%s:%s:%s:%s:%s:%b",
                    currentServer, server, requestId, player.getUniqueId(), name, noConfirm);

            pendingValidations.put(requestId, (result) -> Bukkit.getScheduler().runTask(AxPlayerWarps.getInstance(), () -> {
                if (result.equals("SUCCESS")) {
                    // Before sending player, take the money on the origin server
                    if (isPaid() && currency != null && !player.getUniqueId().equals(owner)) {
                        currency.takeBalance(player.getUniqueId(), teleportPrice);
                    }

                    // Remote validation passed, now send the player
                    String telMessage = String.format("teleport:%s:%s:%s:%s", server, currentServer, player.getUniqueId(), name);
                    AxPlayerWarps.PUBLISHER.publish(AxPlayerWarps.REDIS_CHANNEL, telMessage);
                    if (AxPlayerWarps.SERVER_TRANSFER != null) {
                        AxPlayerWarps.SERVER_TRANSFER.send(player, server);
                    }
                } else if (result.startsWith("ERROR:")) {
                    String langKey = result.substring(6);

                    // If the remote server says it's unsafe, start the local confirmation cooldown
                    if (langKey.equals("confirm.unsafe")) {
                        confirmUnsafe.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
                    } else if (langKey.equals("confirm.paid")) {
                        confirmPaid.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
                    }

                    Map<String, String> replacements = Map.of(
                            "%warp%", getName(),
                            "%price%", currency != null ? currency.getDisplayName().replace("%price%", WarpPlaceholders.format(teleportPrice)) : ""
                    );
                    MESSAGEUTILS.sendLang(player, langKey, replacements);
                }
            }));

            AxPlayerWarps.PUBLISHER.publish(AxPlayerWarps.REDIS_CHANNEL, message);
            return;
        }

        validateTeleport(player, false, bool -> {
            if (!bool) return;
            if (player.hasPermission("axplayerwarps.delay-bypass")) {
                completeTeleportPlayer(player);
                return;
            }
            Scheduler.get().runAt(player.getLocation(), player::closeInventory);
            WarpQueue.addToQueue(player, this);
        });
    }

    public void validateTeleportRemote(UUID playerUuid, boolean noConfirm, BiConsumer<Boolean, String> callback) {
        boolean isOwner = playerUuid.equals(owner);

        // 1. World Check
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            callback.accept(false, "errors.invalid-world");
            return;
        }

        // 2. Access/Blacklist/Whitelist
        if (access == Access.PRIVATE && !isOwner) {
            callback.accept(false, "errors.private");
            return;
        }
        if (blacklisted.stream().anyMatch(ap -> ap.player().getUniqueId().equals(playerUuid))) {
            callback.accept(false, "errors.blacklisted");
            return;
        }
        if (access == Access.WHITELISTED && !isOwner && whitelisted.stream().noneMatch(ap -> ap.player().getUniqueId().equals(playerUuid))) {
            callback.accept(false, "errors.whitelisted");
            return;
        }

        // 3. Balance Check
        if (!isOwner && isPaid() && currency.getBalance(playerUuid) < teleportPrice) {
            callback.accept(false, "errors.not-enough-balance");
            return;
        }

        // 4. Payment Confirmation Check
        if (!isOwner && isPaid() && !noConfirm) {
            callback.accept(false, "confirm.paid");
            return;
        }

        // 5. Safety Check
        isDangerous().thenAccept(dangerous -> {
            if (dangerous && !noConfirm) {
                callback.accept(false, "confirm.unsafe");
            } else {
                callback.accept(true, null);
            }
        });
    }

    public void validateTeleport(Player player, boolean noConfirm, Consumer<Boolean> response) {
        if (!location.isWorldLoaded()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                MESSAGEUTILS.sendLang(player, "errors.invalid-world");
                response.accept(false);
                return;
            }
            location.setWorld(world);
        }

        boolean isOwner = player.getUniqueId().equals(owner);

        // Check if payment confirmation is needed and not yet confirmed
        if (!isOwner && isPaid() && !confirmPaid.hasCooldown(player)) {
            confirmPaid.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
            MESSAGEUTILS.sendLang(player, "confirm.paid",
                    Map.of("%warp%", getName(), "%price%",
                            currency.getDisplayName()
                                    .replace("%price%", WarpPlaceholders.format(teleportPrice))
                    ));
            response.accept(false);
            return;
        }

        isDangerous().thenAccept(dangerous -> {
            if (!noConfirm) {
                if (dangerous && !confirmUnsafe.hasCooldown(player)) {
                    confirmUnsafe.addCooldown(player, CONFIG.getLong("confirmation-milliseconds"));
                    MESSAGEUTILS.sendLang(player, "confirm.unsafe", Map.of("%warp%", getName()));
                    response.accept(false);
                    return;
                }

                // check whitelist/blacklist, check access state
                if (access == Access.PRIVATE && !isOwner) {
                    MESSAGEUTILS.sendLang(player, "errors.private", Map.of("%warp%", getName()));
                    response.accept(false);
                    return;
                }
            }

            if (blacklisted.stream().anyMatch(accessPlayer -> accessPlayer.player().equals(player))) {
                MESSAGEUTILS.sendLang(player, "errors.blacklisted", Map.of("%warp%", getName()));
                response.accept(false);
                return;
            }

            if (access == Access.WHITELISTED && !isOwner && whitelisted.stream().noneMatch(accessPlayer -> accessPlayer.player().equals(player))) {
                MESSAGEUTILS.sendLang(player, "errors.whitelisted", Map.of("%warp%", getName()));
                response.accept(false);
                return;
            }

            // check balance
            if (!isOwner && isPaid() && currency.getBalance(player.getUniqueId()) < teleportPrice) {
                MESSAGEUTILS.sendLang(player, "errors.not-enough-balance");
                response.accept(false);
                return;
            }

            response.accept(true);
        });
    }

    public void completeTeleportPlayer(Player player) {
        completeTeleportPlayer(player, null);
    }

    public void completeTeleportPlayer(Player player, @Nullable String originServer) {
        // If this is a cross-server teleport, skip validation (already done on the origin server)
        if (originServer != null && !originServer.equals(RedisPlayerListServerAPI.getServerId())) {
            executeCompleteTeleport(player, originServer);
            return;
        }

        // For local teleporting, validate first
        validateTeleport(player, true, bool -> {
            if (!bool) return;
            executeCompleteTeleport(player, null);
        });
    }


    private void executeCompleteTeleport(Player player, @Nullable String originServer) {
        player.closeInventory();
        boolean isOwner = player.getUniqueId().equals(owner);

        // Only take money if NOT a cross-server teleport (it was already taken on origin server)
        if (!isOwner && isPaid() && (originServer == null || originServer.equals(RedisPlayerListServerAPI.getServerId()))) {
            if (currency != null) {
                currency.takeBalance(player.getUniqueId(), teleportPrice);
                Bukkit.getLogger().info("[AxPlayerWarps] Money taken on server: " + RedisPlayerListServerAPI.getServerId() + ", player: " + player.getUniqueId() + ", amount: " + teleportPrice);
            }
            earnedMoney += teleportPrice;
            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(this));

            MESSAGEUTILS.sendLang(player, "money.take", Map.of("%price%",
                    currency.getDisplayName().replace("%price%", WarpPlaceholders.format(teleportPrice))));
        } else if (!isOwner && isPaid() && originServer != null && !originServer.equals(RedisPlayerListServerAPI.getServerId())) {
            // For cross-server teleports, only update earned money (money was already taken on origin)
            earnedMoney += teleportPrice;
            AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(this));
        }

        // send a message
        MESSAGEUTILS.sendLang(player, "teleport.success", Map.of("%warp%", getName()));
        confirmUnsafe.remove(player);
        confirmPaid.remove(player);

        // Schedule teleport for next tick with 5L delay to avoid PlayerJoinEvent conflicts with Folia
        Scheduler.get().runLater(scheduledTask -> PaperUtils.teleportAsync(player, location), 5L);

        for (String m : CONFIG.getStringList("teleport-commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderHandler.parse(m.replace("%player%", player.getName()), this, player));
        }

        AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().addVisit(player, this));
    }

    public void delete() {
        Player player = Bukkit.getPlayer(owner);
        AxPlayerWarps.getThreadedQueue().submit(() -> {
            MESSAGEUTILS.sendLang(player, "delete.deleted", Map.of("%warp%", getName()));
            AxPlayerWarps.getDatabase().deleteWarp(this);
        });
    }

    public void withdrawMoney() {
        Player player = Bukkit.getPlayer(owner);
        if (earnedMoney <= 0 || currency == null) {
            MESSAGEUTILS.sendLang(player, "errors.nothing-withdrawable");
            return;
        }
        currency.giveBalance(owner, earnedMoney);
        MESSAGEUTILS.sendLang(player, "money.got", Map.of("%price%",
                currency.getDisplayName()
                        .replace("%price%", WarpPlaceholders.format(earnedMoney))));
        earnedMoney = 0;
        AxPlayerWarps.getThreadedQueue().submit(() -> AxPlayerWarps.getDatabase().updateWarp(this));
    }
}
