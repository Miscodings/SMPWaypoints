package me.miscodes.waypoints.data.sqlite

import de.md5lukas.jdbc.update
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.withContext
import me.miscodes.waypoints.api.WaypointShare
import me.miscodes.waypoints.data.SQLiteManager

class WaypointShareImpl
private constructor(
    private val dm: SQLiteManager,
    override val owner: UUID,
    override val sharedWith: UUID,
    override val waypointId: UUID,
    expires: OffsetDateTime?,
) : WaypointShare {

  constructor(
      dm: SQLiteManager,
      row: ResultSet,
  ) : this(
      dm = dm,
      owner = UUID.fromString(row.getString("owner")),
      sharedWith = UUID.fromString(row.getString("sharedWith")),
      waypointId = UUID.fromString(row.getString("shareId")),
      expires = row.getString("expires")?.let { OffsetDateTime.parse(it) },
  )

  override suspend fun getWaypoint() = dm.api.getWaypointByID(waypointId)!!

  override var expires: OffsetDateTime? = expires
    private set

  override suspend fun setExpires(expires: OffsetDateTime?) {
    this.expires = expires
    withContext(dm.asyncDispatcher) {
      dm.connection.update(
          "UPDATE waypoint_shares SET expires = ? WHERE owner = ? AND sharedWith = ? AND shareId = ?;",
          expires?.toString(),
          owner.toString(),
          sharedWith.toString(),
          waypointId.toString(),
      )
    }
  }

  override suspend fun delete() {
    withContext(dm.asyncDispatcher) {
      dm.connection.update(
          "DELETE FROM waypoint_shares WHERE owner = ? AND sharedWith = ? AND shareId = ?;",
          owner.toString(),
          sharedWith.toString(),
          waypointId.toString(),
      )
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as WaypointShareImpl

    if (owner != other.owner) return false
    if (sharedWith != other.sharedWith) return false
    return waypointId == other.waypointId
  }

  override fun hashCode(): Int {
    var result = owner.hashCode()
    result = 31 * result + sharedWith.hashCode()
    result = 31 * result + waypointId.hashCode()
    return result
  }

  override fun toString(): String {
    return "WaypointShare(owner=$owner, sharedWith=$sharedWith, waypointId=$waypointId, expires=$expires)"
  }
}
