package me.miscodes.waypoints.pointers.config

import me.miscodes.configurate.Positive
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
class PointerConfiguration {

  @Comment(
      "Automatically deselects the waypoint when the player gets into the defined radius. Set to zero to disable"
  )
  @Positive(true)
  var disableWhenReachedRadius: Int = 5
    get() = field * field
    private set

  @Comment(
      "Connected worlds in this list allow the translation of the coordinates 1:8, so you can for example navigate in the nether to a waypoint in the overworld"
  )
  var connectedWorlds: List<WorldConnection> = listOf(WorldConnection("world", "world_nether"))
    private set

  var hologram: HologramConfiguration = HologramConfiguration()
    private set

  @ConfigSerializable
  class WorldConnection() {

    var overworld: String = ""
    var underworld: String = ""

    constructor(overworld: String, underworld: String) : this() {
      this.overworld = overworld
      this.underworld = underworld
    }
  }
}
