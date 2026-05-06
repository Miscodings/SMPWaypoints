package me.miscodes.waypoints.gui.items

import de.md5lukas.commons.paper.appendLore
import de.md5lukas.commons.paper.textComponent
import de.md5lukas.kinvs.items.GUICycleItem
import kotlinx.coroutines.launch
import me.miscodes.waypoints.api.OverviewSort
import me.miscodes.waypoints.gui.WaypointsGUI

class CycleSortItem(wpGUI: WaypointsGUI, onCycle: suspend (OverviewSort) -> Unit) :
    GUICycleItem<OverviewSort>(
        getOverviewSortCycleValues(wpGUI),
        {
          wpGUI.skedule {
            launch { wpGUI.viewerData.setSortBy(it) }
            onCycle(it)
          }
          wpGUI.playSound { click.normal }
        },
    ) {

  init {
    while (wpGUI.viewerData.sortBy != currentValue) {
      cycle()
    }
  }

  private companion object {
    fun getOverviewSortCycleValues(wpGUI: WaypointsGUI) =
        OverviewSort.entries
            .map { current ->
              val additionalLines =
                  wpGUI.translations.OVERVIEW_CYCLE_SORT_OPTIONS.map {
                        textComponent {
                          val copyFrom =
                              if (it.first === current) {
                                    wpGUI.translations.OVERVIEW_CYCLE_SORT_ACTIVE_COLOR
                                  } else {
                                    wpGUI.translations.OVERVIEW_CYCLE_SORT_INACTIVE_COLOR
                                  }
                                  .text
                          style(copyFrom.style())
                          content(it.second.rawText)
                        }
                      }
                      .toList()

              val item = wpGUI.translations.OVERVIEW_CYCLE_SORT.getItem()
              item.appendLore(additionalLines)

              current to item
            }
            .toList()
  }
}
