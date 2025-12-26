package com.kalpi.prochat.utils


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Implement the NetworkStatusObserver interface
class FakeNetworkStatusObserver : NetworkStatusObserver {
    // This MutableStateFlow will allow us to control the network status directly in tests
    private val _networkStatus = MutableStateFlow(true) // Default to connected

    // The observe method just exposes this flow
    override fun observe(): Flow<Boolean> = _networkStatus.asStateFlow()

    // Helper function for tests to change the network status
    fun setNetworkStatus(isConnected: Boolean) {
        _networkStatus.value = isConnected
    }
}