package com.web3auth.core

import java.util.concurrent.CompletableFuture

/**
 * Orchestrates the SFA connect sequence so the caller future always completes.
 *
 * Order:
 * 1. Clear prior citadel session data (async)
 * 2. Run share retrieval / session create (may throw synchronously or complete [future] async)
 *
 * Extracted for unit testing without Firebase / Torus / network.
 */
internal object SfaConnectFlow {

    /**
     * @param future Login future that must already exist before this is called
     * @param clearSession Clears citadel tokens; failure completes [future] exceptionally
     * @param runConnect Invoked only after a successful clear; may throw or complete [future] itself
     */
    fun start(
        future: CompletableFuture<*>,
        clearSession: () -> CompletableFuture<*>,
        runConnect: () -> Unit,
    ) {
        clearSession().whenComplete { _, clearError ->
            if (clearError != null) {
                future.completeExceptionally(clearError)
                return@whenComplete
            }
            try {
                runConnect()
            } catch (t: Throwable) {
                future.completeExceptionally(t)
            }
        }
    }
}
