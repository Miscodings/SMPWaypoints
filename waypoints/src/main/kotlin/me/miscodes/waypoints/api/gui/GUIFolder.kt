package me.miscodes.waypoints.api.gui

import me.miscodes.waypoints.api.Folder
import me.miscodes.waypoints.api.Waypoint

/**
 * An extension of the GUIDisplayable providing information necessary to use the displayable as a
 * folder
 */
interface GUIFolder : GUIDisplayable {

  /** Every folder contained in this displayable */
  suspend fun getFolders(): List<Folder>

  /** Every waypoint contained in this displayable */
  suspend fun getWaypoints(): List<Waypoint>
}
