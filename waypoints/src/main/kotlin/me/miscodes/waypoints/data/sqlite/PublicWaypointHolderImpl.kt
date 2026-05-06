package me.miscodes.waypoints.data.sqlite

import de.md5lukas.jdbc.selectFirst
import java.util.UUID
import kotlinx.coroutines.withContext
import me.miscodes.waypoints.api.Folder
import me.miscodes.waypoints.api.PublicWaypointHolder
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.api.gui.GUIType
import me.miscodes.waypoints.data.SQLiteManager
import org.bukkit.Location

internal class PublicWaypointHolderImpl(dm: SQLiteManager, type: Type) :
    WaypointHolderImpl(dm, type, null), PublicWaypointHolder {

  override suspend fun getWaypointsAmount(creator: UUID): Int =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT COUNT(*) FROM waypoints WHERE type = ? AND owner IS ?;",
            type.name,
            creator.toString(),
        ) {
          getInt(1)
        }!!
      }

  override suspend fun getFoldersAmount(creator: UUID): Int =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT COUNT(*) FROM folders WHERE type = ? AND owner IS ?;",
            type.name,
            creator.toString(),
        ) {
          getInt(1)
        }!!
      }

  override suspend fun createWaypoint(name: String, location: Location, creator: UUID): Waypoint =
      super.createWaypointTyped(name, location, type, creator)

  override suspend fun createFolder(name: String, creator: UUID): Folder =
      super.createFolder0(name, creator)

  override val guiType: GUIType
    get() =
        when (type) {
          Type.PUBLIC -> GUIType.PUBLIC_HOLDER
          Type.PERMISSION -> GUIType.PERMISSION_HOLDER
          else -> throw IllegalStateException()
        }

  override fun toString(): String {
    return "PublicWaypointHolder(type=$type)"
  }
}
