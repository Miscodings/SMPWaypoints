package me.miscodes.waypoints.integrations

import com.flowpowered.math.vector.Vector3d
import com.okkero.skedule.skedule
import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.markers.POIMarker
import me.miscodes.waypoints.WaypointsPlugin
import me.miscodes.waypoints.api.Waypoint
import org.bukkit.World

class BlueMapIntegration(
    plugin: WaypointsPlugin,
) : WebMapIntegration(plugin) {

  companion object Constants {
    const val PLUGIN_NAME = "BlueMap"
  }

  private lateinit var api: BlueMapAPI
  private val markerSets = HashMap<World, MarkerSet>()

  override fun initialize() {
    BlueMapAPI.onEnable {
      api = it

      plugin.skedule {
        plugin.api.publicWaypoints.getAllWaypoints().forEach { waypoint -> createMarker(waypoint) }
      }

      super.initialize()
      plugin.slF4JLogger.info("Delayed initialization of BlueMap integration completed")
    }
  }

  override fun onCreate(waypoint: Waypoint) {
    createMarker(waypoint)
  }

  override fun onDelete(waypoint: Waypoint) {
    getMarkerSet(waypoint.location.world!!).remove(waypoint.id.toString())
  }

  override fun onUpdate(waypoint: Waypoint, newData: String?) {}

  private fun createMarker(waypoint: Waypoint) {
    getMarkerSet(waypoint.location.world!!).markers[waypoint.id.toString()] =
        POIMarker(
            waypoint.name,
            Vector3d(waypoint.location.x, waypoint.location.y, waypoint.location.z),
        )
  }

  private fun getMarkerSet(world: World) =
      markerSets.computeIfAbsent(world) {
        val markerSet = MarkerSet(plugin.translations.INTEGRATIONS_MAPS_LABEL.rawText)
        api.getWorld(world).ifPresent { blueMapWorld ->
          blueMapWorld.maps.forEach { it.markerSets["waypoints_public"] = markerSet }
        }
        markerSet
      }
}
