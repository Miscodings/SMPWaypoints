package me.miscodes.waypoints.tasks

import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.time.measureTime
import kotlin.time.toJavaDuration
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.config.WaypointsConfiguration

class BackupDatabaseTask(private val plugin: WaypointsPlugin) : Runnable {

  private val config: WaypointsConfiguration.Database.Backup
    get() = plugin.waypointsConfig.database.backup

  private val fileFormat =
      DateTimeFormatterBuilder().run {
        append(DateTimeFormatter.ISO_LOCAL_DATE)
        appendLiteral(' ')
        append(DateTimeFormatter.ISO_LOCAL_TIME)
        toFormatter()
      }
  private val backupDirectory = plugin.dataPath.resolve("backups")

  override fun run() {
    val now = LocalDateTime.now()
    val retainN = config.retainLastN
    var backups =
        backupDirectory.listDirectoryEntries().mapNotNullTo(mutableListOf()) { entry ->
          if (Files.isRegularFile(entry) && entry.extension == "db") {
            try {
              return@mapNotNullTo LocalDateTime.parse(entry.nameWithoutExtension, fileFormat) to
                  entry
            } catch (_: DateTimeParseException) {}
          }
          null
        }
    backups.sortByDescending { it.first }
    if (backups.size > retainN) {
      backups = backups.subList(retainN, backups.size)
    }

    val timeRequired =
        measureTime {
              plugin.databaseManager.createBackup(
                  backupDirectory.resolve("${now.format(fileFormat)}.db")
              )
            }
            .toJavaDuration()

    backups.forEach { backup -> Files.delete(backup.second) }

    plugin.slF4JLogger.info(
        "Created database backup in ${plugin.durationFormatter.formatDuration(timeRequired)}. Pruned ${backups.size} old backups."
    )
  }
}
