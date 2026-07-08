package com.example.sanathanatattva.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sanathanatattva.AppContainer
import com.example.sanathanatattva.data.apiCall
import com.example.sanathanatattva.data.model.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Activity-scoped session state: are we logged in, and who is it.
 * Guests can browse the shop; checkout and account surfaces require login.
 */
class SessionViewModel(private val container: AppContainer) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean?> = container.tokenStore.token
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _consumer = MutableStateFlow<Consumer?>(null)
    val consumer: StateFlow<Consumer?> = _consumer

    /** Email awaiting OTP verification, carried from Register/Login to the verify screen. */
    val pendingVerificationEmail = MutableStateFlow<String?>(null)

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            if (container.tokenStore.current() == null) {
                _consumer.value = null
                return@launch
            }
            apiCall { container.api.me() }
                .onSuccess { _consumer.value = it.consumer }
                .onFailure { e ->
                    val code = (e as? com.example.sanathanatattva.data.ApiException)?.code
                    if (code == 401 || code == 403) logout()
                }
        }
    }

    fun onLoggedIn(token: String, consumer: Consumer?) {
        viewModelScope.launch {
            container.tokenStore.save(token)
            _consumer.value = consumer
            if (consumer == null) refreshProfile()
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.tokenStore.clear()
            _consumer.value = null
        }
    }
}
