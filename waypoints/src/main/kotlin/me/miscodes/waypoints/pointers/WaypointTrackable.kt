package me.miscodes.waypoints.pointers

import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import org.bukkit.Location
import org.bukkit.entity.Player

class WaypointTrackable(private val plugin: WaypointsPlugin, val waypoint: Waypoint) : Trackable {

  object Extract : (Trackable) -> Waypoint? {
    override fun invoke(trackable: Trackable): Waypoint? {
      return (trackable as? WaypointTrackable)?.waypoint
    }
  }

  override val location: Location
    get() = waypoint.location

  override fun getHologramText(player: Player, translatedTarget: Location) =
      plugin.apiExtensions.run {
        waypoint
            .getHologramTranslations()
            .withReplacements(*waypoint.getResolvers(player, translatedTarget))
      }

  override fun equals(other: Any?): Boolean {
    return waypoint == (other as? WaypointTrackable)?.waypoint
  }

  override fun hashCode(): Int {
    return waypoint.hashCode()
  }
}
