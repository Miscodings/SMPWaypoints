package me.miscodes.waypoints.tasks

import me.miscodes.waypoints.WaypointsPlugin

class CleanDatabaseTask(private val plugin: WaypointsPlugin) : Runnable {

  override fun run() {
    plugin.databaseManager.cleanDatabase()
  }
}
