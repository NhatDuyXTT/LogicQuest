package com.example.ui

import android.app.Application
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ActionLog
import com.example.data.AppDatabase
import com.example.data.GuestAccount
import com.example.data.GuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileInfo(
    val name: String,
    val size: Long,
    val lastModified: Long,
    val path: String,
    val uid: String? = null,
    val pass: String? = null
)

class MainViewModel(
    private val application: Application,
    private val repository: GuestRepository
) : AndroidViewModel(application) {

    // Creator form state (using Compose state for ultra-fast performance and no father/parent screen recompositions)
    var accountId by mutableStateOf("")
    var accountUid by mutableStateOf("")
    var accountPass by mutableStateOf("")
    var saveAsTemplate by mutableStateOf(true)
    var templateLabel by mutableStateOf("")

    // Storage state
    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

    private val _basePath = MutableStateFlow("")
    val basePath: StateFlow<String> = _basePath.asStateFlow()

    private val _activityFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val activityFiles: StateFlow<List<FileInfo>> = _activityFiles.asStateFlow()

    private val _indoFiles = MutableStateFlow<List<FileInfo>>(emptyList())
    val indoFiles: StateFlow<List<FileInfo>> = _indoFiles.asStateFlow()

    // Inspected file detail
    private val _inspectedFile = MutableStateFlow<FileInfo?>(null)
    val inspectedFile: StateFlow<FileInfo?> = _inspectedFile.asStateFlow()

    // Room lists
    val guestAccounts: StateFlow<List<GuestAccount>> = repository.allGuestAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val actionLogs: StateFlow<List<ActionLog>> = repository.allActionLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Status message for toasted notices
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        // We now wait for MainActivity to pass the saved tree URI via updateCustomPath()
        _isSandboxMode.value = false
        _basePath.value = ""
        viewModelScope.launch(Dispatchers.IO) {
            delay(500) // Delay to let the UI finish its initial drawing
            // refreshFiles will just return if basePath is empty
            refreshFiles()
        }
        logAction("CẤU HÌNH", "Khởi tạo hệ thống thư mục theo chuần SAF")
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun switchToSandbox() {
        // Simplified: sandbox replaced by real device operations
        switchToRealFreeFire()
    }

    fun switchToRealFreeFire() {
        _isSandboxMode.value = false
        // For Android 11+ we wait for the user to select the folder through MainActivity.
        // It will call updateCustomPath()
        _basePath.value = ""
    }

    fun updateCustomPath(path: String) {
        _basePath.value = path
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = Uri.parse(path)
                if (uri.scheme == "content") {
                    val root = DocumentFile.fromTreeUri(application, uri)
                    ensureSubDirectories(root)
                    refreshFiles()
                } else {
                    android.util.Log.e("MainViewModel", "Invalid custom URI scheme: $path")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Exception in updateCustomPath: $path", e)
            }
        }
        logAction("CẤU HÌNH", "Cập nhật đường dẫn tùy chỉnh: $path")
    }

    private fun ensureSubDirectories(baseDir: DocumentFile?) {
        if (baseDir == null) return
        try {
            var actDir = baseDir.findFile("ACTIVITY")
            if (actDir == null) {
                actDir = baseDir.createDirectory("ACTIVITY")
            }
            if (actDir == null) {
                android.util.Log.e("IO_ERROR", "Cannot create directory ACTIVITY")
            }

            var indoDir = baseDir.findFile("ACC")
            if (indoDir == null) {
                indoDir = baseDir.createDirectory("ACC")
            }
            if (indoDir == null) {
                android.util.Log.e("IO_ERROR", "Cannot create directory ACC")
            }
        } catch (e: Exception) {
            android.util.Log.e("IO_ERROR", "Exception creating directories", e)
        }
    }

    private fun createSandboxDemoFilesIfEmpty() {
        val baseDir = File(application.getExternalFilesDir(null), "com.dts.freefire")
        val actDir = File(baseDir, "ACTIVITY")
        val indoDir = File(baseDir, "ACC")
        
        try {
            val actFiles = actDir.listFiles()
            val indoFiles = indoDir.listFiles()
            
            if ((actFiles == null || actFiles.isEmpty()) && (indoFiles == null || indoFiles.isEmpty())) {
                // Write 2 dummy files in ACTIVITY
                writeGuestFileToFolder(actDir, "guest100067.dat(VipId999)", "999888777", "mypass123")
                writeGuestFileToFolder(actDir, "guest100067.dat(IndoPro)", "112233445", "secured99")
                
                // Write 1 dummy file in ACC
                writeGuestFileToFolder(indoDir, "guest100067.dat(ReserveAcct)", "556677889", "reservepass")
            }
        } catch (e: Exception) {
            android.util.Log.e("SandboxDemo", "Failed to create demo files", e)
        }
    }

    private fun writeGuestFileToFolder(folder: File, fileName: String, uid: String, pass: String) {
        try {
            if (!folder.exists() && !folder.mkdirs()) {
                android.util.Log.e("IO_ERROR", "Cannot create directory: ${folder.absolutePath}")
            }
            val targetFile = File(folder, fileName)
            val rawJson = "{\"guest_account_info\":{\"com.garena.msdk.guest_password\":\"$pass\",\"com.garena.msdk.guest_uid\":\"$uid\"}}"
            targetFile.writeText(rawJson, Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("IO_ERROR", "Exception writing to file $fileName", e)
        }
    }

    fun refreshFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                val basePathStr = _basePath.value.ifEmpty { return@launch }
                val uri = Uri.parse(basePathStr)
                if (uri.scheme == "content") {
                    val root = DocumentFile.fromTreeUri(application, uri)
                    val actDir = root?.findFile("ACTIVITY")
                    val indoDir = root?.findFile("ACC")
                    
                    ensureSubDirectories(root)

                    _activityFiles.value = listFilesFromDir(actDir)
                    _indoFiles.value = listFilesFromDir(indoDir)
                } else {
                    android.util.Log.e("MainViewModel", "Invalid background URI scheme: $basePathStr")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error inside refreshFiles", e)
            }
            android.util.Log.d("PERF", "time_refresh=${System.currentTimeMillis() - start}ms")
        }
    }

    private val dateFormat = SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault())

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    private fun listFilesFromDir(dir: DocumentFile?): List<FileInfo> {
        try {
            if (dir == null || !dir.exists() || !dir.isDirectory()) return emptyList()
            val files = dir.listFiles()
            return files.filter { it.isFile() }.map { file ->
                val modified = file.lastModified()
                FileInfo(
                    name = file.name ?: "Unknown",
                    size = file.length(),
                    lastModified = modified,
                    path = file.uri.toString(),
                    uid = null,
                    pass = null
                )
            }.sortedByDescending { it.lastModified }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error in listFilesFromDir", e)
            return emptyList()
        }
    }

    private val parseCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Pair<String, String>>>()

    // Helper to extract UID and Password from Garena DAT file format
    private fun parseGuestFile(uriString: String): Pair<String, String>? {
        return try {
            val uri = Uri.parse(uriString)
            val documentFile = DocumentFile.fromSingleUri(application, uri) ?: return null
            if (!documentFile.exists()) return null
            val lastMod = documentFile.lastModified()
            val cached = parseCache[uriString]
            if (cached != null && cached.first == lastMod) {
                return cached.second
            }
            
            // Only read first 8000 chars to avoid memory crash on large accidental binary files
            val text = application.contentResolver.openInputStream(uri)?.reader()?.use {
                val buffer = CharArray(8000)
                val readChars = it.read(buffer)
                if (readChars > 0) String(buffer, 0, readChars) else ""
            } ?: ""
            
            // Parsing manually to guarantee safety and protect against malformed JSON formats
            val uidRegex = """"com\.garena\.msdk\.guest_uid"\s*:\s*"([^"]+)"""".toRegex()
            val passRegex = """"com\.garena\.msdk\.guest_password"\s*:\s*"([^"]+)"""".toRegex()
            
            val uidMatch = uidRegex.find(text)
            val passMatch = passRegex.find(text)
            
            val result = if (uidMatch != null && passMatch != null) {
                Pair(uidMatch.groupValues[1], passMatch.groupValues[1])
            } else {
                null
            }
            if (result != null) {
                parseCache[uriString] = Pair(lastMod, result)
            }
            result
        } catch (e: java.io.FileNotFoundException) {
            android.util.Log.e("MainViewModel", "File not found during parse: $uriString", e)
            null
        } catch (e: SecurityException) {
            android.util.Log.e("MainViewModel", "Security/Permission exception during parse: $uriString", e)
            null
        } catch (e: java.io.IOException) {
            android.util.Log.e("MainViewModel", "I/O error during parse: $uriString", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Unknown error during parse: $uriString", e)
            null
        }
    }

    fun createGuestAccount(
        accountId: String, 
        uid: String, 
        pass: String, 
        saveAsTemplate: Boolean, 
        label: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (accountId.isEmpty() || uid.isEmpty() || pass.isEmpty()) {
                _statusMessage.value = "❌ Vui lòng điền đầy đủ Account ID, UID và Password!"
                return@launch
            }

            try {
                val basePathStr = _basePath.value.ifEmpty { return@launch }
                val root = DocumentFile.fromTreeUri(application, Uri.parse(basePathStr))
                ensureSubDirectories(root)
                val actDir = root?.findFile("ACTIVITY") ?: return@launch

                val fileName = "guest100067.dat($accountId)"
                var targetFile = actDir.findFile(fileName)
                if (targetFile == null) {
                    targetFile = actDir.createFile("application/octet-stream", fileName)
                }

                if (targetFile != null) {
                    val rawJson = "{\"guest_account_info\":{\"com.garena.msdk.guest_password\":\"$pass\",\"com.garena.msdk.guest_uid\":\"$uid\"}}"
                    application.contentResolver.openOutputStream(targetFile.uri)?.use {
                        it.write(rawJson.toByteArray(Charsets.UTF_8))
                    }
                    logAction("TẠO FILE GUEST", "Đã tạo: $fileName thành công vào ACTIVITY")
                } else {
                    _statusMessage.value = "❌ Lỗi: Không thể tạo file"
                    return@launch
                }

                if (saveAsTemplate) {
                    val dbLabel = label.ifEmpty { "TK $accountId" }
                    repository.insertAccount(
                        GuestAccount(
                            accountId = accountId,
                            uid = uid,
                            pass = pass,
                            label = dbLabel
                        )
                    )
                    logAction("LƯU MẪU", "Tự động sao lưu tài khoản vĩnh viển: $dbLabel")
                }

                _statusMessage.value = "✅ Đã tạo file guest '$fileName' thành công!"
                refreshFiles()
            } catch (e: java.io.IOException) {
                _statusMessage.value = "❌ Lỗi hệ thống tập tin: ${e.localizedMessage}"
                logAction("LỖI", "Không thể ghi file guest ($accountId): ${e.message}")
            } catch (e: SecurityException) {
                _statusMessage.value = "❌ Lỗi bảo mật/quyền truy cập: ${e.localizedMessage}"
                logAction("LỖI", "Thiếu quyền ghi file guest ($accountId): ${e.message}")
            } catch (e: Exception) {
                _statusMessage.value = "❌ Lỗi không xác định: ${e.message}"
                logAction("LỖI", "Lỗi khác khi ghi file guest ($accountId): ${e.message}")
            }
        }
    }

    fun swapOneOnOne(selectedFileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val basePathStr = _basePath.value.ifEmpty { return@launch }
                val root = DocumentFile.fromTreeUri(application, Uri.parse(basePathStr))
                ensureSubDirectories(root)
                val actDir = root?.findFile("ACTIVITY")
                val indoDir = root?.findFile("ACC")
                if (actDir == null || indoDir == null) return@launch

                val selectedFileInAct = actDir.findFile(selectedFileName)
                if (selectedFileInAct == null || !selectedFileInAct.exists()) {
                    _statusMessage.value = "❌ File lựa chọn không tồn tại trong ACTIVITY!"
                    return@launch
                }

                // Check ACC folder listing *before* moving
                val indoDirFiles = indoDir.listFiles().filter { it.isFile() }
                
                // 1. Move from ACTIVITY to ACC
                var dstInIndo = indoDir.findFile(selectedFileName)
                if (dstInIndo == null) dstInIndo = indoDir.createFile("application/octet-stream", selectedFileName)
                if (dstInIndo != null) {
                    application.contentResolver.openInputStream(selectedFileInAct.uri)?.use { input ->
                        application.contentResolver.openOutputStream(dstInIndo.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    selectedFileInAct.delete()
                }

                val infoMsg: String
                if (indoDirFiles.isNotEmpty()) {
                    // 2. Select first file in ACC folder
                    val fileFromIndo = indoDirFiles.first()
                    val fileNameFromIndo = fileFromIndo.name ?: "Unknown"
                    var dstInAct = actDir.findFile(fileNameFromIndo)
                    if (dstInAct == null) dstInAct = actDir.createFile("application/octet-stream", fileNameFromIndo)
                    
                    if (dstInAct != null) {
                        application.contentResolver.openInputStream(fileFromIndo.uri)?.use { input ->
                            application.contentResolver.openOutputStream(dstInAct.uri)?.use { output ->
                                input.copyTo(output)
                            }
                        }
                        fileFromIndo.delete()
                    }
                    
                    infoMsg = "🔄 Đã chuyển '$selectedFileName' -> ACC và hoán đổi '${fileNameFromIndo}' -> ACTIVITY"
                    logAction("HOÁN ĐỔI 1-1", "Từ: $selectedFileName ↔ Về: ${fileNameFromIndo}")
                } else {
                    infoMsg = "✅ Thư mục ACC trống! Đã di chuyển '$selectedFileName' sang ACC một chiều"
                    logAction("DI CHUYỂN", "Đã di chuyển một chiều: $selectedFileName")
                }

                _statusMessage.value = infoMsg
                refreshFiles()
            } catch (e: java.io.FileNotFoundException) {
                _statusMessage.value = "❌ Không tìm thấy tập tin để hoán đổi: ${e.localizedMessage}"
                logAction("LỖI HOÁN ĐỔI", "Thiếu file: ${e.message}")
            } catch (e: java.io.IOException) {
                _statusMessage.value = "❌ Lỗi đọc/ghi khi hoán đổi: ${e.localizedMessage}"
                logAction("LỖI HOÁN ĐỔI", "Lỗi I/O: ${e.message}")
            } catch (e: SecurityException) {
                _statusMessage.value = "❌ Thiếu quyền truy cập thư mục: ${e.localizedMessage}"
                logAction("LỖI HOÁN ĐỔI", "Lỗi bảo mật: ${e.message}")
            } catch (e: Exception) {
                _statusMessage.value = "❌ Lỗi hoán đổi không xác định: ${e.message}"
                logAction("LỖI HOÁN ĐỔI", "${e.message}")
            }
        }
    }

    fun quickDeleteFile(isActivity: Boolean, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val basePathStr = _basePath.value.ifEmpty { return@launch }
                val root = DocumentFile.fromTreeUri(application, Uri.parse(basePathStr))
                val subFolder = if (isActivity) "ACTIVITY" else "ACC"
                val folder = root?.findFile(subFolder)
                val file = folder?.findFile(fileName)
                
                if (file != null && file.exists() && file.delete()) {
                    _statusMessage.value = "🗑️ Đã xóa file $fileName thành công!"
                    logAction("XÓA FILE", "Đã xóa '$fileName' trong $subFolder")
                    refreshFiles()
                    if (_inspectedFile.value?.name == fileName) {
                        _inspectedFile.value = null
                    }
                } else {
                    _statusMessage.value = "❌ Không tìm thấy file cần xóa!"
                }
            } catch (e: java.io.FileNotFoundException) {
                _statusMessage.value = "❌ Không tìm thấy file cần xóa: ${e.localizedMessage}"
            } catch (e: SecurityException) {
                _statusMessage.value = "❌ Lỗi hệ thống/quyền riêng tư khi xóa file: ${e.localizedMessage}"
            } catch (e: Exception) {
                _statusMessage.value = "❌ Lỗi khi xóa file: ${e.message}"
            }
        }
    }

    fun inspectFileContent(fileInfo: FileInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = parseGuestFile(fileInfo.path)
                _inspectedFile.value = fileInfo.copy(
                    uid = parsed?.first ?: "Không parse được UID",
                    pass = parsed?.second ?: "Không parse được mật khẩu"
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error in inspectFileContent", e)
            }
        }
    }

    fun clearInspectedFile() {
        _inspectedFile.value = null
    }

    fun saveManualTemplate(accountId: String, uid: String, pass: String, label: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (accountId.isEmpty() || uid.isEmpty() || pass.isEmpty() || label.isEmpty()) {
                    _statusMessage.value = "❌ Vui lòng nhập đầy đủ thông tin mẫu!"
                    return@launch
                }
                repository.insertAccount(
                    GuestAccount(
                        accountId = accountId,
                        uid = uid,
                        pass = pass,
                        label = label
                    )
                )
                _statusMessage.value = "💾 Đã lưu mẫu tài khoản: $label"
                logAction("LƯU MẪU", "Tự tạo mẫu thủ công: $label")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to save manual template", e)
                _statusMessage.value = "❌ Lỗi khi lưu vào CSDL: ${e.message}"
            }
        }
    }

    fun deleteTemplate(account: GuestAccount) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteAccount(account)
                _statusMessage.value = "🗑️ Đã xóa mẫu tài khoản: ${account.label}"
                logAction("XÓA MẪU", "Đã xóa tài khoản đã lưu: ${account.label}")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to delete template", e)
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearLogs()
                _statusMessage.value = "🧹 Đã dọn dẹp toàn bộ lịch sử!"
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to clear logs", e)
            }
        }
    }

    private fun logAction(action: String, desc: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertLog(
                    ActionLog(
                        actionType = action,
                        description = desc
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Failed to insert log action", e)
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val repository: GuestRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
