package com.kwos.dronepilotapp.data

import androidx.lifecycle.Observer

object Util {

    /** Returns elements in [set] that are not in [other] */
    private fun <T> difference(set: Set<T>, other: Set<T>): Set<T> =
        set - other

    class SetDifference<T>(
        newSet: Set<T>,
        oldSet: Set<T>
    ) {
        val added: Set<T> = difference(newSet, oldSet)
        val removed: Set<T> = difference(oldSet, newSet)
    }

    /**
     * An observer that detects added/removed elements between LiveData updates
     */
    abstract class DiffObserver<T> : Observer<Set<T>> {
        private var last: Set<T> = emptySet()

        override fun onChanged(newSet: Set<T>) {
            val difference = SetDifference(newSet, last)
            if (difference.added.isNotEmpty()) onAdded(difference.added)
            if (difference.removed.isNotEmpty()) onRemoved(difference.removed)
            last = newSet
        }


        abstract fun onAdded(added: Collection<T>)
        abstract fun onRemoved(removed: Collection<T>)
    }
}
