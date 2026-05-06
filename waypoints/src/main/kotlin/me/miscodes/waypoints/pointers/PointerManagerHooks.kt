package me.miscodes.waypoints.pointers

import com.okkero.skedule.future
import com.okkero.skedule.skedule
import java.util.concurrent.CompletableFuture
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.pointers.PointerManager.Hooks
import me.miscodes.waypoints.pointers.variants.PointerVariant
import org.bukkit.entity.Player

class PointerManagerHooks(private val plugin: WaypointsPlugin) : Hooks {

  override fun saveActiveTrackables(player: Player, tracked: Collection<Trackable>) {
    plugin.skedule {
      plugin.api
          .getWaypointPlayer(player.uniqueId)
          .setSelectedWaypoints(tracked.mapNotNull(WaypointTrackable.Extract))
    }
  }

  override fun loadActiveTrackables(player: Player): CompletableFuture<Collection<Trackable>> =
      CompletableFuture.completedFuture(emptyList())

  override fun loadEnabledPointers(player: Player) =
      plugin.future {
        val result = mutableMapOf<PointerVariant, Boolean>()
        plugin.api.getWaypointPlayer(player.uniqueId).enabledPointers.forEach { (storedKey, value)
          ->
          PointerVariant.entries.firstOrNull { it.key == storedKey }?.let { result[it] = value }
        }
        result
      }
}
