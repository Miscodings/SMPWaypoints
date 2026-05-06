package me.miscodes.waypoints.data.sqlite

import de.md5lukas.jdbc.select
import de.md5lukas.jdbc.selectFirst
import de.md5lukas.jdbc.update
import java.util.UUID
import kotlinx.coroutines.withContext
import me.miscodes.waypoints.api.Folder
import me.miscodes.waypoints.api.PublicWaypointHolder
import me.miscodes.waypoints.api.Statistics
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.api.WaypointsAPI
import me.miscodes.waypoints.api.WaypointsPlayer
import me.miscodes.waypoints.data.SQLiteManager

internal class WaypointsAPIImpl(
    private val dm: SQLiteManager,
) : WaypointsAPI {

  override suspend fun getWaypointPlayer(uuid: UUID): WaypointsPlayer =
      withContext(dm.asyncDispatcher) {
        // Must add the canBeTracked 0, because some users might have an old database that has 1
        // has the default value and that cannot be altered.
        dm.connection.update(
            "INSERT INTO player_data(id, canBeTracked) VALUES(?, 0) ON CONFLICT DO NOTHING;",
            uuid.toString(),
        )
        dm.connection.selectFirst("SELECT * FROM player_data WHERE id = ?;", uuid.toString()) {
          WaypointsPlayerImpl(dm, this)
        }!!
      }

  override suspend fun waypointsPlayerExists(uuid: UUID): Boolean =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT EXISTS(SELECT 1 FROM player_data WHERE id = ?);",
            uuid.toString(),
        ) {
          getInt(1) == 1
        } ?: false
      }

  override val publicWaypoints: PublicWaypointHolder = PublicWaypointHolderImpl(dm, Type.PUBLIC)
  override val permissionWaypoints: PublicWaypointHolder =
      PublicWaypointHolderImpl(dm, Type.PERMISSION)

  override suspend fun getWaypointByID(uuid: UUID): Waypoint? =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst("SELECT * FROM waypoints WHERE id = ?;", uuid.toString()) {
          WaypointImpl(dm, this)
        }
      }

  override suspend fun getFolderByID(uuid: UUID): Folder? =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst("SELECT * FROM folders WHERE id = ?;", uuid.toString()) {
          FolderImpl(dm, this)
        }
      }

  override suspend fun getAllPrivateWaypoints(): List<Waypoint> =
      withContext(dm.asyncDispatcher) {
        dm.connection.select("SELECT * FROM waypoints WHERE type = ?;", Type.PRIVATE.name) {
          WaypointImpl(dm, this)
        }
      }

  override val statistics: Statistics = StatisticsImpl(dm)
}
