package com.water1014.miowatery.core.desktop

/**
 * Resolves imported desktop placement without losing default shortcuts.
 * A shortcut may belong to only one placement group; the first occurrence wins.
 */
object DesktopOrderResolver {
    fun resolve(
        defaults: Map<String, List<String>>,
        saved: Map<String, List<String>>,
        validIds: Set<String>
    ): Map<String, List<String>> {
        val assigned = linkedSetOf<String>()
        return buildMap {
            defaults.forEach { (group, defaultIds) ->
                val resolved = buildList {
                    (saved[group].orEmpty() + defaultIds).forEach { id ->
                        if (id in validIds && assigned.add(id)) add(id)
                    }
                }
                put(group, resolved)
            }
        }
    }
}
