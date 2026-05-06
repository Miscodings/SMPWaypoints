package me.miscodes.waypoints.integrations

import de.md5lukas.commons.paper.registerEvents
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.sequences.forEach
import kotlin.text.startsWith
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.api.event.WaypointCreateEvent
import me.miscodes.waypoints.api.event.WaypointCustomDataChangeEvent
import me.miscodes.waypoints.api.event.WaypointPostDeleteEvent
import me.miscodes.waypoints.config.WaypointsConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

abstract class WebMapIntegration(
    protected val plugin: WaypointsPlugin,
    private val customDataKey: String = "",
) : Listener {

  companion object {
    private val webMapIntegrations =
        arrayOf<
            Triple<
                WaypointsConfiguration.Integrations.() -> Boolean,
                String,
                (WaypointsPlugin) -> WebMapIntegration,
            >
        >(
            Triple({ bluemap.enabled }, BlueMapIntegration.PLUGIN_NAME, ::BlueMapIntegration),
            Triple({ dynmap.enabled }, DynMapIntegration.PLUGIN_NAME, ::DynMapIntegration),
            Triple({ pl3xmap.enabled }, Pl3xMapIntegration.PLUGIN_NAME, ::Pl3xMapIntegration),
            Triple({ squaremap.enabled }, SquareMapIntegration.PLUGIN_NAME, ::SquareMapIntegration),
        )

    fun startWebMapIntegrations(plugin: WaypointsPlugin): List<String> {
      extractIcons(plugin)

      val enabledIntegrations = mutableListOf<String>()

      webMapIntegrations.forEach { (isEnabled, requiredPluginName, supplier) ->
        if (
            plugin.waypointsConfig.integrations.isEnabled() &&
                plugin.server.pluginManager.getPlugin(requiredPluginName) != null
        ) {
          plugin.slF4JLogger.info("Found $requiredPluginName plugin for WebMap integration")

          val integration = supplier(plugin)

          integration.initialize()

          enabledIntegrations += requiredPluginName
        }
      }

      return enabledIntegrations
    }

    private fun extractIcons(plugin: Plugin) {
      plugin.getResource("resourceIndex")!!.bufferedReader(StandardCharsets.UTF_8).useLines { seq ->
        seq.filter { it.startsWith("icons/") }
            .forEach {
              if (!File(plugin.dataFolder, it).exists()) {
                plugin.saveResource(it, false)
              }
            }
      }
    }
  }

  protected val webMapSettings: WaypointsConfiguration.Integrations.GeneralWebMapSettings
    get() = plugin.waypointsConfig.integrations.generalWebMapSettings

  protected suspend fun getWaypointsToShow(): List<Waypoint> {
    val waypoints = mutableListOf<Waypoint>()
    if (webMapSettings.showPublicWaypoints) {
      waypoints += plugin.api.publicWaypoints.getAllWaypoints()
    }
    if (webMapSettings.showPermissionWaypoints) {
      waypoints += plugin.api.permissionWaypoints.getAllWaypoints()
    }
    if (webMapSettings.showPrivateWaypoints) {
      waypoints += plugin.api.getAllPrivateWaypoints()
    }
    return waypoints
  }

  @EventHandler
  fun on(e: WaypointCreateEvent) {
    if (webMapSettings.isTypeVisible(e.waypoint.type)) {
      onCreate(e.waypoint)
    }
  }

  @EventHandler
  fun on(e: WaypointPostDeleteEvent) {
    if (webMapSettings.isTypeVisible(e.waypoint.type)) {
      onDelete(e.waypoint)
    }
  }

  @EventHandler
  fun on(e: WaypointCustomDataChangeEvent) {
    if (webMapSettings.isTypeVisible(e.waypoint.type) && e.key == customDataKey) {
      onUpdate(e.waypoint, e.data)
    }
  }

  open fun initialize() {
    plugin.registerEvents(this)
  }

  abstract fun onCreate(waypoint: Waypoint)

  abstract fun onDelete(waypoint: Waypoint)

  open fun onUpdate(waypoint: Waypoint, newData: String?) {
    onDelete(waypoint)
    onCreate(waypoint)
  }
}
