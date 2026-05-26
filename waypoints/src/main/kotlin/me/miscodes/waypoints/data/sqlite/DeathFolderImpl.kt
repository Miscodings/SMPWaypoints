package me.miscodes.waypoints.data.sqlite

import de.md5lukas.jdbc.select
import de.md5lukas.jdbc.selectFirst
import de.md5lukas.jdbc.update
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*
import kotlinx.coroutines.withContext
import me.miscodes.waypoints.api.Folder
import me.miscodes.waypoints.api.Icon
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.api.gui.GUIType
import me.miscodes.waypoints.data.SQLiteManager
import org.bukkit.permissions.Permissible

class DeathFolderImpl(
    private val dm: SQLiteManager,
    override val owner: UUID,
) : Folder {

  override val id: UUID
    get() = owner

  override val createdAt: OffsetDateTime = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))
  override val type: Type
    get() = Type.DEATH

  override val name: String
    get() = guiType.name

  override suspend fun setName(name: String) =
      throw UnsupportedOperationException("Changing the name of the death folder is not supported")

  override val icon: Icon?
    get() = null

  override suspend fun setIcon(icon: Icon?) =
      throw UnsupportedOperationException("Changing the icon of the death folder is not supported")

  override suspend fun getAmount(): Int =
      withContext(dm.asyncDispatcher) {
        dm.connection.selectFirst(
            "SELECT COUNT(*) FROM waypoints WHERE type = ? AND owner = ?;",
            Type.DEATH.name,
            owner.toString(),
        ) {
          getInt(1)
        }!!
      }

  override suspend fun getAmountVisibleForPlayer(permissible: Permissible): Int = getAmount()

  override suspend fun getFolders(): List<Folder> = emptyList()

  override suspend fun getWaypoints(): List<Waypoint> =
      withContext(dm.asyncDispatcher) {
        dm.connection.select(
            "SELECT * FROM waypoints WHERE type = ? AND owner = ?;",
            Type.DEATH.name,
            owner.toString(),
        ) {
          WaypointImpl(dm, this)
        }
      }

  override suspend fun delete() {
    withContext(dm.asyncDispatcher) {
      dm.connection.update(
          "DELETE FROM waypoints WHERE type = ? AND owner = ?;",
          Type.DEATH.name,
          owner.toString(),
      )
    }
  }

  override val guiType: GUIType = GUIType.DEATH_FOLDER

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Folder

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun toString(): String {
    return "DeathFolder(owner=$owner)"
  }
}
