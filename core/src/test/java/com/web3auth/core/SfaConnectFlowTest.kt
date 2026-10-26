package com.web3auth.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Regression tests for the SFA connect hang: clear-session / getTorusKey failures
 * must complete the login future, and success must still complete normally.
 * No Firebase, network, or Torus nodes.
 */
class SfaConnectFlowTest {

    @Test(timeout = 5_000)
    fun getTorusKeyStyleException_completesFutureExceptionallyWithinTimeout() {
        val future = CompletableFuture<String>()
        val clearDone = CompletableFuture<Void>()

        SfaConnectFlow.start(
            future = future,
            clearSession = {
                clearDone.complete(null)
                clearDone
            },
            runConnect = {
                // Mimics getTorusKey() throwing before any async session create.
                throw IllegalStateException("torus key retrieval failed")
            },
        )

        try {
            future.get(2, TimeUnit.SECONDS)
            fail("Expected ExecutionException")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is IllegalStateException)
            assertEquals("torus key retrieval failed", e.cause?.message)
        } catch (e: TimeoutException) {
            fail("connect hung — future never completed exceptionally")
        }
        assertTrue(future.isCompletedExceptionally)
    }

    @Test(timeout = 5_000)
    fun clearSessionDataFailure_completesFutureExceptionally() {
        val future = CompletableFuture<String>()
        val clearFailed = CompletableFuture<Void>()
        clearFailed.completeExceptionally(RuntimeException("citadel clear failed"))
        val runConnectCalled = AtomicBoolean(false)

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearFailed },
            runConnect = { runConnectCalled.set(true) },
        )

        try {
            future.get(2, TimeUnit.SECONDS)
            fail("Expected ExecutionException")
        } catch (e: ExecutionException) {
            assertEquals("citadel clear failed", e.cause?.message)
        } catch (e: TimeoutException) {
            fail("connect hung — clearSession failure never completed the future")
        }

        assertFalse("runConnect must not run after clearSession failure", runConnectCalled.get())
        assertTrue(future.isCompletedExceptionally)
    }

    @Test(timeout = 5_000)
    fun successfulSfaLogin_completesNormally() {
        val future = CompletableFuture<String>()
        val clearDone = CompletableFuture<Void>()
        clearDone.complete(null)

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearDone },
            runConnect = {
                // Mimics createSession success path completing the login future.
                future.complete("0xabc")
            },
        )

        val result = try {
            future.get(2, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            fail("successful SFA path hung")
            return
        }

        assertEquals("0xabc", result)
        assertTrue(future.isDone)
        assertFalse(future.isCompletedExceptionally)
        assertFalse(future.isCancelled)
    }

    @Test(timeout = 5_000)
    fun asyncClearSession_thenSyncThrow_stillCompletesExceptionally() {
        val future = CompletableFuture<String>()
        val clearPending = CompletableFuture<Void>()

        SfaConnectFlow.start(
            future = future,
            clearSession = { clearPending },
            runConnect = { throw Exception("invalid jwt") },
        )

        assertFalse("future must not complete before clearSession finishes", future.isDone)

        clearPending.complete(null)

        try {
            future.get(2, TimeUnit.SECONDS)
            fail("Expected ExecutionException")
        } catch (e: ExecutionException) {
            assertEquals("invalid jwt", e.cause?.message)
        }
    }
}
