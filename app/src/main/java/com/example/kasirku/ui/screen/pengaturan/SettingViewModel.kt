package com.example.kasirku.ui.screen.pengaturan


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kasirku.data.repository.AuthRepository
import com.example.kasirku.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
data class SettingUiState(
    val outletName: String = "Memuat...",
    val outletAddress: String = "",
    val outletPhone: String = "",
    val outletFooter: String = "",

    // Tambahkan ini untuk menampilkan Logo di Menu Utama
    val outletLogo: String? = null,

    val showLogoutDialog: Boolean = false,
    val showResetDialog: Boolean = false,
    val showEditProfileSheet: Boolean = false,

    // State Input Form Edit
    val editName: String = "",
    val editPhone: String = "",
    val editAddress: String = "",
    val editFooter: String = "",

    // Tambahkan ini untuk menampung URI Logo saat Edit
    val editLogo: String? = null,

    val isLoggedOut: Boolean = false
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                _uiState.update {
                    it.copy(
                        outletName = user.name,
                        outletAddress = user.address ?: "Alamat belum diatur",
                        outletPhone = user.phoneNumber ?: "",
                        outletFooter = user.footerNote ?: "",
                        outletLogo = user.logo // Load logo ke tampilan utama
                    )
                }
            } else {
                _uiState.update { it.copy(outletName = "Tamu") }
            }
        }
    }

    // Buka Sheet Edit
    fun onEditProfileClicked() {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                showEditProfileSheet = true,
                editName = current.outletName,
                editPhone = current.outletPhone,
                editAddress = if (current.outletAddress == "Alamat belum diatur") "" else current.outletAddress,
                editFooter = current.outletFooter,
                editLogo = current.outletLogo // Masukkan logo saat ini ke form edit
            )
        }
    }

    fun onDismissEditProfileSheet() {
        _uiState.update { it.copy(showEditProfileSheet = false) }
    }

    // Form Input Handlers
    fun onEditNameChange(v: String) = _uiState.update { it.copy(editName = v) }
    fun onEditPhoneChange(v: String) = _uiState.update { it.copy(editPhone = v) }
    fun onEditAddressChange(v: String) = _uiState.update { it.copy(editAddress = v) }
    fun onEditFooterChange(v: String) = _uiState.update { it.copy(editFooter = v) }

    // Handler Ganti Logo
    fun onEditLogoChange(uri: String?) = _uiState.update { it.copy(editLogo = uri) }


    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Buat nama file unik
                val fileName = "logo_outlet_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)

                // Buka input stream dari URI galeri
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(file)

                // Salin data
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                // Kembalikan path file lokal yang permanen
                file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Panggil ini dari UI saat user memilih gambar
    fun onLogoSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            // Simpan gambar ke internal storage dulu
            val savedPath = saveImageToInternalStorage(context, uri)

            if (savedPath != null) {
                // Update state dengan path file lokal yang permanen
                _uiState.update { it.copy(editLogo = savedPath) }
            } else {
                Toast.makeText(context, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun saveProfile(context: Context) {
        viewModelScope.launch {
            val userId = authRepository.getLoggedInUserId()
            if (userId != -1) {
                authRepository.updateOutletProfile(
                    id = userId,
                    name = _uiState.value.editName,
                    phone = _uiState.value.editPhone,
                    address = _uiState.value.editAddress,
                    footer = _uiState.value.editFooter,

                    // Simpan Logo
                    logo = _uiState.value.editLogo
                )
                Toast.makeText(context, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                loadUserProfile()
                onDismissEditProfileSheet()
            }
        }
    }

    fun showLogoutDialog(show: Boolean) = _uiState.update { it.copy(showLogoutDialog = show) }
    fun showResetDialog(show: Boolean) = _uiState.update { it.copy(showResetDialog = show) }
    fun logout() {
        viewModelScope.launch {
            try {
                // Cukup set state untuk menampilkan dialog konfirmasi
                // Logout sebenarnya akan diproses oleh AuthViewModel
                _uiState.update {
                    it.copy(
                        showLogoutDialog = false,
                        isLoggedOut = true // Trigger untuk AuthViewModel
                    )
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    fun resetOrderNumber(context: Context) {
        viewModelScope.launch {
            orderRepository.resetOrderSequence()
            Toast.makeText(context, "Nomor Pesanan direset ke 0001", Toast.LENGTH_SHORT).show()
            _uiState.update { it.copy(showResetDialog = false) }
        }
    }

    fun backupData(context: Context) {

        try {
            val dbName = "kasirku_db"
            val dbPath = context.getDatabasePath(dbName)
            val backupDir = context.getExternalFilesDir("Backup")
            if (backupDir != null && !backupDir.exists()) backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_kasirku_${System.currentTimeMillis()}.db")
            if (dbPath.exists()) {
                dbPath.copyTo(backupFile, overwrite = true)
                Toast.makeText(context, "Backup Sukses!\nLokasi: ${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal Backup: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    fun restoreData(context: Context) {
        Toast.makeText(context, "Fitur Restore akan tersedia di update berikutnya.", Toast.LENGTH_SHORT).show()
    }
}

