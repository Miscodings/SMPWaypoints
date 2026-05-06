package me.miscodes.waypoints.pointers

import de.md5lukas.commons.paper.placeholder
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.util.getResolvers
import org.bukkit.Location
import org.bukkit.entity.Player

class PlayerTrackable(private val plugin: WaypointsPlugin, val player: Player) : Trackable {

  override val location: Location
    get() = player.location

  override fun getHologramText(player: Player, translatedTarget: Location) =
      plugin.translations.POINTERS_HOLOGRAM_PLAYER_TRACKING.withReplacements(
          "name" placeholder this.player.displayName(),
          *location.getResolvers(plugin, player, translatedTarget),
      )

  override fun equals(other: Any?): Boolean {
    return player == (other as? PlayerTrackable)?.player
  }

  override fun hashCode(): Int {
    return player.hashCode()
  }
}
