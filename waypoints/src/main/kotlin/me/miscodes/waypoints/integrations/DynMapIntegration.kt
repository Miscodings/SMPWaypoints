package me.miscodes.waypoints.integrations

import com.okkero.skedule.skedule
import java.util.*
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.events.ConfigReloadEvent
import org.bukkit.event.EventHandler
import org.dynmap.DynmapAPI
import org.dynmap.markers.MarkerAPI
import org.dynmap.markers.MarkerIcon
import org.dynmap.markers.MarkerSet

/**
 * For docs see:
 * https://github.com/webbukkit/dynmap/blob/v3.0/DynmapCoreAPI/src/main/java/org/dynmap/markers/MarkerAPI.java
 * https://github.com/webbukkit/dynmap/blob/v3.0/DynmapCoreAPI/src/main/java/org/dynmap/markers/MarkerSet.java
 */
class DynMapIntegration(plugin: WaypointsPlugin) : WebMapIntegration(plugin, CUSTOM_DATA_KEY) {

  companion object Constants {
    const val PLUGIN_NAME = "dynmap"
    const val CUSTOM_DATA_KEY = "dynmap-icon"
  }

  private lateinit var markerApi: MarkerAPI
  private lateinit var markerSet: MarkerSet
  private lateinit var defaultMarkerIcon: MarkerIcon

  override fun initialize() {
    markerApi = (plugin.server.pluginManager.getPlugin(PLUGIN_NAME) as DynmapAPI).markerAPI
    defaultMarkerIcon = markerApi.getMarkerIcon(plugin.waypointsConfig.integrations.dynmap.icon)

    markerSet =
        markerApi.createMarkerSet(
            "waypoints",
            plugin.translations.INTEGRATIONS_MAPS_LABEL.rawText,
            null,
            false,
        ) // id, label, iconlimit, persistent

    plugin.skedule { plugin.api.publicWaypoints.getAllWaypoints().forEach { createMarker(it) } }

    super.initialize()
  }

  @EventHandler
  fun onConfigReload(e: ConfigReloadEvent) {
    val newId = e.config.integrations.dynmap.icon
    if (newId != defaultMarkerIcon.markerIconID) {
      defaultMarkerIcon = markerApi.getMarkerIcon(newId)
    }
    plugin.skedule {
      markerSet.markers.forEach {
        it.markerIcon =
            getMarkerForWaypoint(plugin.api.getWaypointByID(UUID.fromString(it.markerID))!!)
      }
    }
  }

  override fun onCreate(waypoint: Waypoint) {
    plugin.skedule { createMarker(waypoint) }
  }

  override fun onDelete(waypoint: Waypoint) {
    markerSet.findMarker(waypoint.id.toString())?.deleteMarker()
  }

  override fun onUpdate(waypoint: Waypoint, newData: String?) {
    plugin.skedule {
      markerSet.findMarker(waypoint.id.toString())?.markerIcon =
          getMarkerForWaypoint(waypoint, newData)
    }
  }

  private suspend fun createMarker(waypoint: Waypoint) {
    with(waypoint.location) {
      val worldNotNull = world ?: return
      markerSet.createMarker(
          waypoint.id.toString(), // ID
          waypoint.name, // Label
          worldNotNull.name, // World ID
          x, // X
          y, // Y
          z, // z
          getMarkerForWaypoint(waypoint), // Marker icon
          false, // is persistent
      )
    }
  }

  private suspend fun getMarkerForWaypoint(
      waypoint: Waypoint,
      directIcon: String? = null,
  ): MarkerIcon {
    val icon = directIcon ?: waypoint.getCustomData(CUSTOM_DATA_KEY)
    return if (icon === null) {
      defaultMarkerIcon
    } else {
      val customIcon: MarkerIcon? = markerApi.getMarkerIcon(icon)

      if (customIcon === null) {
        plugin.slF4JLogger.error(
            "The public waypoint {} has the icon with the name '{}', but that icon does not exist!",
            waypoint.name,
            icon,
        )
        defaultMarkerIcon
      } else {
        customIcon
      }
    }
  }
}
