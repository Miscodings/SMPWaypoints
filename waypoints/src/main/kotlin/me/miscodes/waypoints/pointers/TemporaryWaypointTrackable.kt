package me.miscodes.waypoints.pointers

import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.util.getResolvers
import org.bukkit.Location
import org.bukkit.entity.Player

class TemporaryWaypointTrackable(
    private val plugin: WaypointsPlugin,
    override val location: Location,
) : Trackable {

  override fun getHologramText(player: Player, translatedTarget: Location) =
      plugin.apiExtensions.run {
        plugin.translations.POINTERS_HOLOGRAM_TEMPORARY.withReplacements(
            *location.getResolvers(plugin, player, translatedTarget)
        )
      }
}
