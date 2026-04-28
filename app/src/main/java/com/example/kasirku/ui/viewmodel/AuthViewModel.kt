package com.example.kasirku.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Address
import javax.inject.Inject

sealed interface AuthState {
    object Loading : AuthState
    object Authenticated : AuthState
    object Unauthenticated : AuthState
}

sealed class AuthUiEvent {
    data class ShowSnackbar(val message: String) : AuthUiEvent()
}

data class LoginState(val isLoading: Boolean = false)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val _registerResult = MutableStateFlow<Boolean?>(null)
    val registerResult: StateFlow<Boolean?> = _registerResult

    private val _eventFlow = MutableSharedFlow<AuthUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState

    // Flag untuk mencegah race condition
    private var isInitialCheckDone = false

    init {
        checkUserSession()
    }

    private fun checkUserSession() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getLoggedInUserId()
                _authState.value = if (userId != -1) {
                    AuthState.Authenticated
                } else {
                    AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
            } finally {
                isInitialCheckDone = true
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(AuthUiEvent.ShowSnackbar("Email dan password harus diisi."))
            }
            return
        }

        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true) }

            try {
                val isLoginSuccessful = authRepository.login(email, pass)

                if (isLoginSuccessful) {
                    // Beri waktu untuk menyimpan session
                    delay(100)
                    _authState.value = AuthState.Authenticated
                } else {
                    _eventFlow.emit(AuthUiEvent.ShowSnackbar("Login gagal. Periksa kembali email dan password Anda."))
                }
            } catch (e: Exception) {
                _eventFlow.emit(AuthUiEvent.ShowSnackbar("Terjadi kesalahan saat login."))
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    // PERBAIKAN: Logout yang lebih robust
    fun logout() {
        viewModelScope.launch {
            try {
                // 1. Set state loading terlebih dahulu
                _loginState.update { it.copy(isLoading = true) }

                // 2. Lakukan logout di repository
                authRepository.logout()

                // 3. Beri waktu untuk proses logout selesai
                delay(100)

                // 4. Pastikan session benar-benar terhapus
                val userId = authRepository.getLoggedInUserId()
                if (userId == -1) {
                    // 5. Update auth state ke Unauthenticated
                    _authState.value = AuthState.Unauthenticated

                    // 6. Reset semua state yang diperlukan
                    _loginState.value = LoginState()
                    _registerResult.value = null

                    Log.d("AuthViewModel", "Logout berhasil, user session cleared")
                } else {
                    // Fallback: force logout state meskipun session masih ada
                    _authState.value = AuthState.Unauthenticated
                    Log.e("AuthViewModel", "Logout anomaly: Session masih ada tapi force logout")
                }

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error during logout: ${e.message}")
                // Force logout meskipun ada error
                _authState.value = AuthState.Unauthenticated
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Fungsi lainnya tetap sama...
    fun register(name: String, email: String, phone: String, address: String, pass: String) {
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true) }

            try {
                val newUser = authRepository.registerUser(name, email, phone, address, pass)

                if (newUser != null) {
                    val autoLoginSuccess = authRepository.autoLoginAfterRegister(newUser)

                    if (autoLoginSuccess) {
                        delay(200)
                        _authState.value = AuthState.Authenticated
                        _registerResult.value = true
                    } else {
                        _eventFlow.emit(AuthUiEvent.ShowSnackbar("Registrasi berhasil tapi auto login gagal. Silakan login manual."))
                        _registerResult.value = true
                    }
                } else {
                    _eventFlow.emit(AuthUiEvent.ShowSnackbar("Email sudah terdaftar. Silakan gunakan email lain."))
                    _registerResult.value = false
                }
            } catch (e: Exception) {
                _eventFlow.emit(AuthUiEvent.ShowSnackbar("Terjadi kesalahan saat registrasi."))
                _registerResult.value = false
            } finally {
                _loginState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetRegisterResult() {
        _registerResult.value = null
    }
}