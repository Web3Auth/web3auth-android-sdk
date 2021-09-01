package com.openlogin.core

fun interface AuthStateChangeListener {
    fun onAuthStateChange(state: OpenLogin.State)
}