package me.miscodes.waypoints.integrations

import com.okkero.skedule.skedule
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import me.miscodes.waypoints.events.ConfigReloadEvent
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.server.PluginDisableEvent
import xyz.jpenilla.squaremap.api.*
import xyz.jpenilla.squaremap.api.marker.Marker
import xyz.jpenilla.squaremap.api.marker.MarkerOptions

class SquareMapIntegration(plugin: WaypointsPlugin) : WebMapIntegration(plugin, CUSTOM_DATA_KEY) {

  companion object Constants {
    const val PLUGIN_NAME = "squaremap"
    const val CUSTOM_DATA_KEY = "squaremap-icon"
  }

  private lateinit var api: Squaremap
  private lateinit var layerKey: Key
  private val layerProviders: MutableMap<UUID, SimpleLayerProvider?> = HashMap()

  private val iconFolder = File(plugin.dataFolder, "icons")

  override fun initialize() {
    api = SquaremapProvider.get()

    layerKey = Key.of("waypoints")

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
              BukkitAdapter.point(waypoint.location),
              getMarkerForWaypoint(waypoint),
              plugin.waypointsConfig.integrations.squaremap.iconSize,
          )
      marker.markerOptions(MarkerOptions.builder().hoverTooltip(waypoint.name))
      provider.addMarker(Key.of(waypoint.id.toString()), marker)
    }
  }

  private val warnedKeys = mutableSetOf<String>()

  private suspend fun getMarkerForWaypoint(waypoint: Waypoint): Key {
    val rawKey =
        waypoint.getCustomData(CUSTOM_DATA_KEY)
            ?: plugin.waypointsConfig.integrations.squaremap.icon
    var key = Key.of(rawKey)

    if (api.iconRegistry().hasEntry(key)) {
      return key
    }

    key = Key.of("waypoints-$rawKey")

    if (!api.iconRegistry().hasEntry(key)) {
      val image = File(iconFolder, "$rawKey.png")
      if (image.exists()) {
        try {
          api.iconRegistry().register(key, ImageIO.read(image))
        } catch (e: Exception) {
          throw RuntimeException(
              "Could not load icon file ${image.absolutePath} for public waypoint ${waypoint.name}",
              e,
          )
        }
      } else if (rawKey !in warnedKeys) {
        warnedKeys.add(rawKey)
        plugin.slF4JLogger.error(
            "The public waypoint {} has the custom map icon key '{}' set, but the actual image is not present at {}",
            waypoint.name,
            rawKey,
            image.absolutePath,
        )
      }
    }

    return key
  }

  private val World.layerProvider: SimpleLayerProvider?
    get() =
        layerProviders.computeIfAbsent(uid) {
          mapWorld?.let { mapWorld ->
            val provider =
                SimpleLayerProvider.builder(plugin.translations.INTEGRATIONS_MAPS_LABEL.rawText)
                    .build()

            mapWorld.layerRegistry().register(layerKey, provider)

            provider
          }
        }

  private fun unregisterLayerProviders() {
    api.mapWorlds().forEach {
      it.layerRegistry().let { registry ->
        if (registry.hasEntry(layerKey)) {
          registry.unregister(layerKey)
        }
      }
    }
    layerProviders.clear()
  }

  private val World.mapWorld: MapWorld?
    get() = api.getWorldIfEnabled(BukkitAdapter.worldIdentifier(this)).orElse(null)

  private fun Waypoint.removeMarker() {
    location.world?.layerProvider?.removeMarker(Key.of(id.toString()))
  }
}
