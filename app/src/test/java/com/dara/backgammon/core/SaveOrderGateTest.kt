package com.dara.backgammon.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * The autosave path writes from an IO thread while `onCleared` writes inline on the UI thread;
 * these tests pin the ordering guarantee that keeps the last move of a match on disk.
 */
class SaveOrderGateTest {
    @Test fun snapshotsReachTheStoreInCaptureOrder() {
        val gate = SaveOrderGate()
        val file = StringBuilder()
        val first = gate.next()
        val second = gate.next()
        assertTrue(gate.write(first) { file.append("first"); true })
        assertTrue(gate.write(second) { file.append("|second"); true })
        assertEquals("first|second", file.toString())
        assertEquals(second, gate.attemptedSeq)
    }

    @Test fun anInFlightWriteCannotOverwriteANewerSnapshot() {
        val gate = SaveOrderGate()
        var file = "previous launch"
        val queued = gate.next()   // published by autosave, still waiting for the IO thread
        val newest = gate.next()   // captured later, written inline by onCleared
        assertTrue(gate.write(newest) { file = "newest"; true })
        var bodyRan = false
        assertFalse(gate.write(queued) { bodyRan = true; file = "stale"; true })
        assertFalse("a superseded snapshot must not even touch the store", bodyRan)
        assertEquals("newest", file)
        assertEquals(newest, gate.attemptedSeq)
    }

    @Test fun positionsAreNeverReusedAndTheGateOnlyMovesForward() {
        val gate = SaveOrderGate()
        val first = gate.next()
        val second = gate.next()
        assertEquals(first + 1, second)
        assertTrue(gate.write(second) { true })
        assertFalse(gate.write(first) { true })
        assertFalse(gate.write(second) { true })
        assertEquals(second, gate.attemptedSeq)
    }

    @Test fun aFailedWriteStillConsumesItsPosition() {
        val gate = SaveOrderGate()
        val failed = gate.next()
        assertFalse("the store reported failure", gate.write(failed) { false })
        assertEquals(failed, gate.attemptedSeq)
        assertFalse("an older snapshot must not be written after a newer attempt", gate.write(failed - 1) { true })
        assertTrue("a later snapshot can still be written", gate.write(gate.next()) { true })
    }

    @Test fun concurrentWritersLeaveTheNewestSnapshotOnDisk() {
        val gate = SaveOrderGate()
        val captured = (1..120).map { gate.next() }
        var file = 0L
        val executed = Collections.synchronizedList(mutableListOf<Long>())
        val start = CountDownLatch(1)
        // Every writer races to write a different snapshot, newest first, so the run is far
        // likelier to interleave than a plain sequential loop.
        val writers = captured.reversed().map { seq ->
            thread {
                start.await()
                gate.write(seq) { executed.add(seq); file = seq; true }
            }
        }
        start.countDown()
        writers.forEach { it.join() }
        assertEquals(captured.last(), file)
        assertEquals(captured.last(), gate.attemptedSeq)
        assertEquals("executed writes must be monotonic", executed.sorted(), executed.toList())
        assertFalse("nothing may be written after the newest snapshot", gate.write(captured.first()) { file = -1; true })
        assertEquals(captured.last(), file)
    }
}
