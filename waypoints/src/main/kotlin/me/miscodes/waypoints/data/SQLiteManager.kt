package me.miscodes.waypoints.data

import de.md5lukas.commons.paper.editMeta
import de.md5lukas.jdbc.select
import de.md5lukas.jdbc.selectFirst
import de.md5lukas.jdbc.update
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.OffsetDateTime
import java.util.logging.Level
import kotlinx.coroutines.Dispatchers
import me.miscodes.waypoints.api.Icon
import me.miscodes.waypoints.api.OverviewSort
import me.miscodes.waypoints.api.Type
import me.miscodes.waypoints.api.WaypointsAPI
import me.miscodes.waypoints.config.WaypointsConfiguration
import me.miscodes.waypoints.data.sqlite.WaypointsAPIImpl
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin

class SQLiteManager(
    val plugin: Plugin,
    val databaseConfiguration: WaypointsConfiguration.Database,
    val file: Path?,
    val testing: Boolean = false,
) {

  private val schemaVersion: Int = 10

  val asyncDispatcher =
      if (testing) {
        Dispatchers.Unconfined
      } else {
        Dispatchers.IO
      }

  val api: WaypointsAPI by lazy { WaypointsAPIImpl(this) }

  // There is no need to retrieve generated keys because we don't use them.
  val connection: Connection =
      DriverManager.getConnection("jdbc:sqlite:${file ?: ":memory:"}?jdbc.get_generated_keys=false")

  fun initDatabase() {
    initConnection()
    createTables()
    upgradeDatabase()
    cleanDatabase()
  }

  private fun initConnection() {
    connection.update("PRAGMA foreign_keys = ON;")
  }

  private fun createTables() {
    with(connection) {
      update(
          """
                CREATE TABLE IF NOT EXISTS database_meta (
                  id INTEGER NOT NULL PRIMARY KEY,
                  schemaVersion INTEGER NOT NULL
                );
            """
      )
      update(
          "INSERT OR IGNORE INTO database_meta(id, schemaVersion) VALUES (?, ?);",
          0,
          schemaVersion,
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS player_data (
                  id TEXT NOT NULL PRIMARY KEY,
                  
                  showGlobals BOOLEAN NOT NULL DEFAULT 1,
                  sortBy TEXT NOT NULL DEFAULT '${OverviewSort.TYPE_ASCENDING.name}',
                  canBeTracked BOOLEAN NOT NULL DEFAULT 0,
                  canReceiveTemporaryWaypoints BOOLEAN NOT NULL DEFAULT 1,
                  enabledPointers TEXT
                );
            """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS player_data_typed (
                  playerId TEXT NOT NULL,
                  type TEXT NOT NULL,
                  
                  cooldownUntil TEXT,
                  teleportations INTEGER NOT NULL DEFAULT 0,
                  
                  PRIMARY KEY (playerId, type),
                  FOREIGN KEY (playerId) REFERENCES player_data(id) ON DELETE CASCADE
                );
                """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS folders (
                  id TEXT NOT NULL PRIMARY KEY,
                  
                  createdAt DATE NOT NULL,
                  
                  type TEXT NOT NULL,
                  owner TEXT,
                  
                  name TEXT NOT NULL,
                  description TEXT,
                  icon BLOB,
                  
                  FOREIGN KEY (owner) REFERENCES player_data(id) ON DELETE CASCADE 
                );
            """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS waypoints (
                  id TEXT NOT NULL PRIMARY KEY,
                  
                  createdAt DATE NOT NULL,
                  
                  type TEXT NOT NULL,
                  owner TEXT,
                  folder TEXT,
                  
                  name TEXT NOT NULL,
                  description TEXT,
                  permission TEXT,
                  icon BLOB,
                  beaconColor TEXT,
                  
                  world TEXT NOT NULL,
                  x REAL NOT NULL,
                  y REAL NOT NULL,
                  z REAL NOT NULL,
                  
                  FOREIGN KEY (owner) REFERENCES player_data(id) ON DELETE CASCADE,
                  FOREIGN KEY (folder) REFERENCES folders(id) ON DELETE SET NULL
                );
            """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS waypoint_meta (
                  waypointId TEXT NOT NULL,
                  playerId TEXT NOT NULL,
                  
                  teleportations INTEGER NOT NULL DEFAULT 0,
                  visited BOOLEAN NOT NULL DEFAULT 0,
                  
                  PRIMARY KEY (waypointId, playerId),
                  FOREIGN KEY (waypointId) REFERENCES waypoints(id) ON DELETE CASCADE,
                  FOREIGN KEY (playerId) REFERENCES player_data(id) ON DELETE CASCADE
                );
            """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS selected_waypoints (
                  playerId TEXT NOT NULL,
                  waypointId TEXT NOT NULL,
                  'index' INTEGER NOT NULL,
                  
                  PRIMARY KEY (playerId, waypointId),
                  FOREIGN KEY (playerId) REFERENCES player_data(id) ON DELETE CASCADE,
                  FOREIGN KEY (waypointId) REFERENCES waypoints(id) ON DELETE CASCADE
                );
            """
      )
      update(
          """
                CREATE TABLE IF NOT EXISTS waypoint_custom_data (
                  waypointId TEXT NOT NULL,
                  key TEXT NOT NULL,
                  
                  data TEXT NOT NULL,
                  
                  PRIMARY KEY (waypointId, key),
                  FOREIGN KEY (waypointId) REFERENCES waypoints(id) ON DELETE CASCADE
                );
                """
      )
      update(
          """
               CREATE TABLE IF NOT EXISTS compass_storage (
                 playerId TEXT NOT NULL PRIMARY KEY,
                 
                 world TEXT NOT NULL,
                 x REAL NOT NULL,
                 y REAL NOT NULL,
                 z REAL NOT NULL,
                 
                 FOREIGN KEY (playerId) REFERENCES player_data(id) ON DELETE CASCADE
               );
            """
      )
      update(
          """
               CREATE TABLE IF NOT EXISTS waypoint_shares (
                 owner TEXT NOT NULL,
                 sharedWith TEXT NOT NULL,
                 shareId TEXT NOT NULL,
                 expires DATE,
                 
                 PRIMARY KEY (owner, sharedWith, shareId),
                 FOREIGN KEY (owner) REFERENCES player_data(id) ON DELETE CASCADE,
                 FOREIGN KEY (sharedWith) REFERENCES player_data(id) ON DELETE CASCADE,
                 FOREIGN KEY (shareId) REFERENCES waypoints(id) ON DELETE CASCADE
               );
      """
      )
    }
  }

  fun cleanDatabase() {
    // Remove death waypoints older than the specified amount of time, if the amount is non-zero
    if (!databaseConfiguration.deathWaypointRetentionPeriod.isZero) {
      connection.update(
          "DELETE FROM waypoints WHERE type = ? AND datetime(createdAt) <= datetime(?);",
          Type.DEATH.name,
          OffsetDateTime.now().minus(databaseConfiguration.deathWaypointRetentionPeriod).toString(),
      )
      connection.update(
          "DELETE FROM waypoint_shares WHERE expires IS NOT NULL AND datetime(expires) <= datetime(?);",
          OffsetDateTime.now(),
      )
    }
  }

  private val databaseUpgrades =
      LinkedHashMap<Int, Connection.() -> Unit>().also {
        it[1] = {
          update(
              "ALTER TABLE player_data ADD COLUMN lastSelectedWaypoint TEXT REFERENCES waypoints (id) ON DELETE SET NULL;"
          )
        }
        it[2] = {
          update("ALTER TABLE player_data ADD COLUMN canBeTracked BOOLEAN NOT NULL DEFAULT 0;")
        }
        @Suppress("SqlResolve") // Table has been deleted, duh
        it[3] = {
          select("SELECT playerId, type, cooldownUntil FROM player_cooldown;") {
            update(
                "INSERT INTO player_data_typed(playerId, type, cooldownUntil) VALUES (?, ?, ?);",
                getString("playerId"),
                getString("type"),
                getString("cooldownUntil"),
            )
          }
          update("DROP TABLE player_cooldown;")
        }
        it[4] = {
          update("ALTER TABLE waypoint_meta ADD COLUMN visited BOOLEAN NOT NULL DEFAULT 0;")
        }
        it[5] = {} // beacon color migration (removed feature)
        it[6] = { update("ALTER TABLE player_data ADD COLUMN enabledPointers TEXT;") }
        it[7] = {
          update(
              "ALTER TABLE player_data ADD COLUMN canReceiveTemporaryWaypoints BOOLEAN NOT NULL DEFAULT 0;"
          )
          update("UPDATE player_data SET canReceiveTemporaryWaypoints = 1;")
        }
        @Suppress("SqlResolve") // material column dropped
        it[8] = {
          Material.values()
              .filter { material -> !material.isLegacy && material.name.endsWith("WALL_BANNER") }
              .forEach { material ->
                update(
                    "UPDATE waypoints SET material = ? WHERE material = ?;",
                    material.createBlockData().placementMaterial.name,
                    material.name,
                )
              }
        }
        it[9] = {} // beacon color migration (removed feature)
        @Suppress("SqlResolve") // material column dropped
        it[10] = {
          @Suppress("DEPRECATION") // That's why we are gonna migrate away from it
          fun parseIcon(string: String): ItemStack {
            val index = string.indexOf('|')

            return if (index >= 0) {
              ItemStack.of(Material.valueOf(string.take(index))).also { stack ->
                stack.editMeta<ItemMeta> { setCustomModelData(string.substring(index + 1).toInt()) }
              }
            } else {
              ItemStack.of(Material.valueOf(string))
            }
          }

          update("ALTER TABLE folders ADD COLUMN icon BLOB;")
          update("ALTER TABLE waypoints ADD COLUMN icon BLOB;")

          select("SELECT id, material FROM folders WHERE material IS NOT NULL;") {
            val newFormat = Icon.icon(parseIcon(getString("material"))).getBytes()
            update("UPDATE folders SET icon = ? WHERE id = ?;", newFormat, getString("id"))
          }
          select("SELECT id, material FROM waypoints WHERE material IS NOT NULL;") {
            val newFormat = Icon.icon(parseIcon(getString("material"))).getBytes()
            update("UPDATE waypoints SET icon = ? WHERE id = ?;", newFormat, getString("id"))
          }

          update("ALTER TABLE folders DROP material;")
          update("ALTER TABLE waypoints DROP material;")
        }
      }

  private fun upgradeDatabase() {
    with(connection) {
      val currentSchemaVersion =
          selectFirst("SELECT schemaVersion FROM database_meta WHERE id = ?;", 0) {
            getInt("schemaVersion")
          } ?: throw IllegalStateException("Could not retrieve schema version of database")
      if (currentSchemaVersion > schemaVersion) {
        throw IllegalStateException(
            "The database uses a schema that is newer than the plugin is made for (Database: $currentSchemaVersion, Plugin: $schemaVersion)"
        )
      }

      if (currentSchemaVersion != schemaVersion) {
        plugin.logger.log(
            Level.INFO,
            "Current database schema version: $currentSchemaVersion. Required database schema version: $schemaVersion",
        )
      }

      databaseUpgrades.forEach { (upgradesTo, upgrade) ->
        if (currentSchemaVersion < upgradesTo) {
          try {
            update("BEGIN TRANSACTION;")
            upgrade()
            update("UPDATE database_meta SET schemaVersion = ? WHERE id = ?;", upgradesTo, 0)
            update("COMMIT TRANSACTION;")
          } catch (e: Exception) {
            var suppressed: Exception? = null
            try {
              update("ROLLBACK TRANSACTION;")
            } catch (e2: Exception) {
              suppressed = e2
            }
            throw Exception("Could not perform database upgrade to version $upgradesTo", e).also {
              if (suppressed != null) {
                it.addSuppressed(suppressed)
              }
            }
          }
        }
      }
    }
  }

  fun createBackup(output: Path) {
    connection.update("BACKUP TO ?;", output.toString())
  }

  fun close() {
    connection.close()
  }
}
