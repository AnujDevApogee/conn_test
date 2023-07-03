package com.example.testingbl.data

sealed class ConnectionState {
    object Connected : ConnectionState()
    object DisConnected : ConnectionState()
    object UnInitialized : ConnectionState()
    object Initialized : ConnectionState()
}
