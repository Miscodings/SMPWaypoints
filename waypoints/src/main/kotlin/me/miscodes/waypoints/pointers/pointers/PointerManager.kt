package me.miscodes.waypoints.pointers

import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import me.miscodes.waypoints.pointers.config.PointerConfiguration
import me.miscodes.waypoints.pointers.variants.PointerVariant
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin

/**
 * The PointerManager handles the creation of the selected PointerTypes and manages their tasks
 *
 * @property plugin The plugin to register the tasks for
 * @property hooks The callbacks this library requires to be implemented by the caller
 * @property configuration The configuration for the pointers
 * @constructor Creates a new PointerManager
 */
class PointerManager(
    internal val plugin: Plugin,
    internal val hooks: Hooks,
    internal var configuration: PointerConfiguration,
) : Listener {

  init {
    plugin.server.pluginManager.registerEvents(this, plugin)
  }

  private val players = ConcurrentHashMap<UUID, ManagedPlayer>()

  /**
   * Safely shuts down all pointers, recreates them based on the new configuration and restarts them
   *
   * @param newConfiguration The new configuration to use
   */
  fun applyNewConfiguration(newConfiguration: PointerConfiguration) {
    configuration = newConfiguration
    players.values.forEach { it.reapplyConfiguration() }
  }

  /**
   * Immediately cancels all pointer tasks and removes all display entities for every managed
   * player, then empties the player map. Does not persist any state changes.
   *
   * Use this before a full reload so no ghost entities remain between cleanup and re-setup.
   */
  fun clearAll() {
    players.values.forEach(ManagedPlayer::immediateCleanup)
    players.clear()
  }

  /**
   * This method should be called when a player edits his enabled pointers to apply the changes
   * immediately
   *
   * @param player the player to restart the pointers for
   * @see Hooks.loadEnabledPointers
   */
  fun reapplyConfiguration(player: Player) {
    players[player.uniqueId]?.reapplyConfiguration()
  }

  /**
   * Enables the pointer for a player towards the provided trackable.
   *
   * This will call [Hooks.saveActiveTrackables] to save this new active trackable
   */
  fun enable(player: Player, trackable: Trackable): Unit = enable(player, trackable, true)

  private fun enable(player: Player, trackable: Trackable, save: Boolean) {
    disable(player, { it !== trackable }, false)
    val managedPlayer = players.computeIfAbsent(player.uniqueId) { ManagedPlayer(this, player) }
    managedPlayer.show(trackable)
    if (save) {
      hooks.saveActiveTrackables(player, managedPlayer.readOnlyTracked)
    }
  }

  /**
   * Disables the pointer for the given player.
   *
   * This will call [Hooks.saveActiveTrackables]
   */
  fun disable(player: Player, predicate: TrackablePredicate): Unit =
      disable(player, predicate, true)

  private fun disable(player: Player, predicate: TrackablePredicate, save: Boolean) {
    players[player.uniqueId]?.let { managedPlayer ->
      managedPlayer.readOnlyTracked.filter(predicate).forEach { managedPlayer.hide(it) }
      if (save) {
        hooks.saveActiveTrackables(player, managedPlayer.readOnlyTracked)
      }
      if (managedPlayer.canBeDiscarded) {
        players -= player.uniqueId
      }
    }
  }

  /**
   * Disables all pointers where the trackable matches the [predicate].
   *
   * This will call [Hooks.saveActiveTrackables] for every player.
   */
  fun disableAll(predicate: TrackablePredicate) {
    players.keys.forEach { uuid -> plugin.server.getPlayer(uuid)?.let { disable(it, predicate) } }
  }

  /** Gets the current trackables for the player */
  fun getCurrentTargets(player: Player): Collection<Trackable> =
      players[player.uniqueId]?.readOnlyTracked ?: emptyList()

  @EventHandler
  internal fun onPlayerJoin(e: PlayerJoinEvent) {
    hooks.loadActiveTrackables(e.player).thenAccept { trackables ->
      trackables.forEach { enable(e.player, it, false) }
    }
  }

  @EventHandler
  internal fun onQuit(e: PlayerQuitEvent) {
    players.remove(e.player.uniqueId)?.immediateCleanup()
    hooks.saveActiveTrackables(e.player, emptyList())
  }

  @EventHandler
  internal fun onMove(e: PlayerMoveEvent) {
    if (e.from.blockX == e.to.blockX && e.from.blockY == e.to.blockY && e.from.blockZ == e.to.blockZ) return

    val trackables = getCurrentTargets(e.player)

    val disableWhenReachedRadius = configuration.disableWhenReachedRadius

    if (trackables.isEmpty() || disableWhenReachedRadius == 0) {
      return
    }

    trackables.forEach {
      if (e.player.world === it.location.world) {
        val distance = e.player.location.distanceSquared(it.location)

        if (distance <= disableWhenReachedRadius) {
          disable(e.player, it.asPredicate())
        }
      }
    }
  }

  @EventHandler
  internal fun onPluginDisable(e: PluginDisableEvent) {
    if (e.plugin !== plugin) return

    players.forEach { (uuid, managed) ->
      managed.immediateCleanup()
      plugin.server.getPlayer(uuid)?.let { hooks.saveActiveTrackables(it, emptyList()) }
    }
    players.clear()
  }

  /** Hooks that get called by the [PointerManager] and some pointers */
  interface Hooks {

    /**
     * Save the provided trackables, if possible, to a non-volatile storage.
     *
     * The ordering of the trackables must be preserved.
     *
     * @param player The player that had the trackable enabled
     * @param tracked The new trackable or <code>null</code> if it has been disabled
     */
    fun saveActiveTrackables(player: Player, tracked: Collection<Trackable>) {}

    /**
     * Load the last active trackables from non-volatile storage.
     *
     * This is called when a player joins the server.
     *
     * @param player The player that had the trackable enabled
     * @return The last trackable or <code>null</code> if it has been disabled
     */
    fun loadActiveTrackables(player: Player): CompletableFuture<Collection<Trackable>> {
      return CompletableFuture.completedFuture(emptyList())
    }

    /**
     * Load the pointers the player has enabled for himself.
     *
     * If a [PointerVariant] is not present in the map the default of `true` is used.
     *
     * @param player The player to load the enabled pointers for
     * @return The enabled pointers
     */
    fun loadEnabledPointers(player: Player): CompletableFuture<out Map<PointerVariant, Boolean>> {
      return CompletableFuture.completedFuture(emptyMap())
    }
  }
}
