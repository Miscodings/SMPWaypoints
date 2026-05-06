package me.miscodes.waypoints.integrations

import com.okkero.skedule.skedule
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.events.ConfigReloadEvent
import net.pl3x.map.core.Pl3xMap
import net.pl3x.map.core.image.IconImage
import net.pl3x.map.core.markers.layer.SimpleLayer
import net.pl3x.map.core.markers.marker.Marker
import net.pl3x.map.core.markers.option.Options
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.server.PluginDisableEvent

class Pl3xMapIntegration(plugin: WaypointsPlugin) : WebMapIntegration(plugin, CUSTOM_DATA_KEY) {

  companion object Constants {
    const val PLUGIN_NAME = "Pl3xMap"
    const val CUSTOM_DATA_KEY = "pl3xmap-icon"
  }

  private lateinit var api: Pl3xMap
  private val layerKey: String = "waypoints"
  private val layerProviders: MutableMap<UUID, SimpleLayer?> = HashMap()

  private val iconFolder = File(plugin.dataFolder, "icons")

  override fun initialize() {
    api = Pl3xMap.api()

    plugin.skedule { getWaypointsToShow().forEach { createMarker(it) } }

    super.initialize()
  }

  @EventHandler
  fun onDisable(e: PluginDisableEvent) {
    if (e.plugin !== plugin) {
      return
    }

    layerProviders.values.forEach { it?.clearMarkers() }
  }

  @EventHandler
  @Suppress("UNUSED_PARAMETER")
  fun onConfigReload(e: ConfigReloadEvent) {
    plugin.skedule {
      plugin.api.publicWaypoints.getAllWaypoints().let { waypoints ->
        waypoints.forEach { it.removeMarker() }
        unregisterLayerProviders()
        waypoints.forEach { createMarker(it) }
      }
    }
  }

  override fun onCreate(waypoint: Waypoint) {
    plugin.skedule { createMarker(waypoint) }
  }

  override fun onDelete(waypoint: Waypoint) {
    waypoint.removeMarker()
  }

  private suspend fun createMarker(waypoint: Waypoint) {
    val worldNotNull = waypoint.location.world ?: return

    worldNotNull.layerProvider?.let { provider ->
      val marker =
          Marker.icon(
              waypoint.id.toString(),
              waypoint.location.x,
              waypoint.location.z,
              getMarkerForWaypoint(waypoint),
              plugin.waypointsConfig.integrations.pl3xmap.iconSize,
          )
      marker.setOptions(Options.builder().tooltipContent(waypoint.name))
      provider.addMarker(marker)
    }
  }

  private val warnedKeys = mutableSetOf<String>()

  private suspend fun getMarkerForWaypoint(waypoint: Waypoint): String {
    val key =
        waypoint.getCustomData(CUSTOM_DATA_KEY) ?: plugin.waypointsConfig.integrations.pl3xmap.icon

    if (api.iconRegistry.has(key)) {
      return key
    }

    val prefixedKey = "waypoints-$key"

    if (!api.iconRegistry.has(prefixedKey)) {
      val image = File(iconFolder, "$key.png")
      if (image.exists()) {
        try {
          api.iconRegistry.register(IconImage(prefixedKey, ImageIO.read(image), "png"))
        } catch (e: Exception) {
          throw RuntimeException(
              "Could not load icon file ${image.absolutePath} for public waypoint ${waypoint.name}",
              e,
          )
        }
      } else if (key !in warnedKeys) {
        warnedKeys.add(key)
        plugin.slF4JLogger.error(
            "The public waypoint {} has the custom map icon key '{}' set, but the actual image is not present at {}",
            waypoint.name,
            key,
            image.absolutePath,
        )
      }
    }

    return prefixedKey
  }

  private val World.layerProvider: SimpleLayer?
    get() =
        layerProviders.computeIfAbsent(uid) {
          mapWorld?.let { mapWorld ->
            val layer =
                SimpleLayer(layerKey) { plugin.translations.INTEGRATIONS_MAPS_LABEL.rawText }

            mapWorld.layerRegistry.register(layer)

            layer
          }
        }

  private fun unregisterLayerProviders() {
    api.worldRegistry.forEach {
      it.layerRegistry.let { registry ->
        if (registry.has(layerKey)) {
          registry.unregister(layerKey)
        }
      }
    }
    layerProviders.clear()
  }

  private val World.mapWorld
    get() = api.worldRegistry.get(name)

  private fun Waypoint.removeMarker() {
    location.world?.layerProvider?.removeMarker(id.toString())
  }
}
