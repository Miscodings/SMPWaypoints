package me.miscodes.waypoints.api.event

import me.miscodes.waypoints.api.Waypoint
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** This event is triggered when a new waypoint is created */
class WaypointCreateEvent(
    val isAsync: Boolean,
    /** The just created waypoint */
    val waypoint: Waypoint,
) : Event(isAsync) {

  private companion object {
    @JvmStatic // Automatically creates static getHandlerList()
    val handlerList = HandlerList()
  }

  override fun getHandlers(): HandlerList = handlerList
}
