package me.miscodes.waypoints.util

import de.md5lukas.commons.paper.placeholder
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.pointers.PlayerTrackable
import me.miscodes.waypoints.pointers.TemporaryWaypointTrackable
import me.miscodes.waypoints.pointers.Trackable
import me.miscodes.waypoints.pointers.WaypointTrackable
import net.kyori.adventure.text.Component

suspend fun formatCurrentTargets(
    plugin: WaypointsPlugin,
    trackables: Collection<Trackable>,
): List<Component> {
  return trackables.map { trackable ->
    when (trackable) {
      is WaypointTrackable ->
          trackable.waypoint.let {
            plugin.apiExtensions.run {
              when (it.type) {
                Type.PRIVATE -> plugin.translations.OVERVIEW_DESELECT_NAMES_WAYPOINT_PRIVATE
                Type.DEATH -> plugin.translations.OVERVIEW_DESELECT_NAMES_WAYPOINT_DEATH
                Type.PUBLIC -> plugin.translations.OVERVIEW_DESELECT_NAMES_WAYPOINT_PUBLIC
                Type.PERMISSION -> plugin.translations.OVERVIEW_DESELECT_NAMES_WAYPOINT_PERMISSION
              }.withReplacements(
                  "path" placeholder it.getFullPath(),
                  "created_at" placeholder it.createdAt,
              )
            }
          }
      is PlayerTrackable ->
          trackable.player.let {
            plugin.translations.OVERVIEW_DESELECT_NAMES_PLAYER_TRACKING.withReplacements(
                "name" placeholder it.displayName()
            )
          }
      is TemporaryWaypointTrackable ->
          plugin.translations.OVERVIEW_DESELECT_NAMES_WAYPOINT_TEMPORARY.text
      else -> plugin.translations.OVERVIEW_DESELECT_NAMES_UNKNOWN.text
    }
  }
}
