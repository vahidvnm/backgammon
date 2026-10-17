package com.dara.backgammon.core

/**
 * Orders the writers of the saved-match file.
 *
 * Autosave captures a snapshot on the UI thread but writes it on an IO thread, while
 * `onCleared` has to write one final snapshot inline right after that thread is gone. Without
 * ordering, a write that was already in flight could land *after* the inline one and quietly
 * restore an older match on the next launch.
 *
 * [next] reserves the position of a snapshot; [write] runs its body only when that snapshot is
 * newer than everything attempted so far. Both run inside the same lock, so two writers can
 * never interleave and the file always ends up holding the newest snapshot that was offered.
 * The gate only ever moves forward: a failed write still consumes its position, because
 * falling back to an older snapshot is worse than keeping the previous file contents.
 *
 * Thread safe, no dependencies beyond the Kotlin standard library.
 */
class SaveOrderGate {
    private val lock = Any()
    private var issued = 0L
    private var attempted = 0L

    /** Reserves the position of a freshly captured snapshot. Call once per snapshot. */
    fun next(): Long = synchronized(lock) { ++issued }

    /** Highest position that reached [write], whether or not its body reported success. */
    val attemptedSeq: Long get() = synchronized(lock) { attempted }

    /**
     * Runs [body] when [seq] supersedes every snapshot attempted so far and reports what
     * [body] returned. A superseded snapshot is rejected without touching the store.
     */
    fun write(seq: Long, body: () -> Boolean): Boolean = synchronized(lock) {
        if (seq <= attempted) return false
        val saved = body()
        attempted = seq
        saved
    }
}
