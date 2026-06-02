package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.GuestRepository
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
                .edit().putString("tree_uri", uri.toString()).apply()
            Toast.makeText(this, "✅ Đã cấp quyền truy cập thư mục thành công!", Toast.LENGTH_SHORT).show()
            permissionGrantedState = true
            viewModel?.updateCustomPath(uri.toString())
        } else {
            Toast.makeText(this, "⚠️ Bạn cần cấp quyền để ứng dụng hoạt động!", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasStoragePermission(): Boolean {
        val uriStr = getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
            .getString("tree_uri", null)
        if (uriStr != null) {
            // Check if we still have permission
            val persistedUriPermissions = contentResolver.persistedUriPermissions
            for (p in persistedUriPermissions) {
                if (p.uri.toString() == uriStr) {
                    return true
                }
            }
        }
        return false
    }

    private fun requestStoragePermission() {
        requestPickerLauncher.launch(null)
    }

    private var viewModel: MainViewModel? = null
    private var permissionGrantedState by androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        
        // Initialize Room Database layers
        val database = AppDatabase.getDatabase(this)
        val repository = GuestRepository(database.guestDao())
        
        // Initiate ViewModel
        val vm: MainViewModel by viewModels {
            MainViewModelFactory(application, repository)
        }
        viewModel = vm

        val uriStr = getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
            .getString("tree_uri", null)
        if (uriStr != null) {
            vm.updateCustomPath(uriStr)
        }

        permissionGrantedState = hasStoragePermission()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = com.example.ui.SurfaceDark // Make Scaffold have the matching charcoal background
                ) { innerPadding ->
                    MainScreen(
                        viewModel = vm,
                        modifier = Modifier.padding(innerPadding),
                        hasPermission = { permissionGrantedState },
                        onRequestPermission = { requestStoragePermission() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGrantedState = hasStoragePermission()
    }

}
