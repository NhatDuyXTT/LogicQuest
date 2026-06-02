package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionLog
import com.example.data.GuestAccount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Beautiful color constants for a premium game style (charcoal dark backgrounds and neon fire-garena orange accents)
val SurfaceDark = Color(0xFF0F1016)
val CardBackground = Color(0xFF181A24)
val GarenaOrange = Color(0xFF2196F3) // Blue color as requested
val GarenaAmber = Color(0xFFFFC107)
val AccentCyan = Color(0xFF00E5FF)
val CodeGray = Color(0xFF262938)
val InfoBlue = Color(0xFF1E88E5)

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    hasPermission: () -> Boolean = { true },
    onRequestPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val basePath by viewModel.basePath.collectAsState()
    val isSandboxMode by viewModel.isSandboxMode.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var permissionGranted by remember { mutableStateOf(hasPermission()) }

    // Dynamic permission update when switching views or folders
    LaunchedEffect(isSandboxMode, activeTab) {
        permissionGranted = hasPermission()
    }

    // Handle incoming toasted status alerts
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    // Stably remember execution callbacks on MainScreen level to avoid garbage collection/jank on tab switches
    val currentOnInspect = remember(viewModel) { { file: FileInfo -> viewModel.inspectFileContent(file) } }
    val currentOnCloseInspect = remember(viewModel) { { viewModel.clearInspectedFile() } }
    val currentOnExecuteSwap = remember(viewModel) { { name: String -> viewModel.swapOneOnOne(name) } }
    val currentOnDeleteFile = remember(viewModel) { { isAct: Boolean, name: String -> viewModel.quickDeleteFile(isAct, name) } }
    val currentOnRefresh = remember(viewModel) { { viewModel.refreshFiles() } }

    val currentOnSubmit = remember(viewModel) {
        {
            viewModel.createGuestAccount(
                viewModel.accountId, 
                viewModel.accountUid, 
                viewModel.accountPass, 
                viewModel.saveAsTemplate, 
                viewModel.templateLabel
            )
            // Clean fields post submit
            viewModel.accountId = ""
            viewModel.accountUid = ""
            viewModel.accountPass = ""
            viewModel.templateLabel = ""
        }
    }

    val currentOnLoadTemplate = remember(viewModel) {
        { account: GuestAccount ->
            viewModel.accountId = account.accountId
            viewModel.accountUid = account.uid
            viewModel.accountPass = account.pass
            viewModel.templateLabel = account.label
        }
    }

    val currentOnDeleteTemplate = remember(viewModel) { { account: GuestAccount -> viewModel.deleteTemplate(account) } }
    val currentOnClearLogs = remember(viewModel) { { viewModel.clearAllLogs() } }
    val currentOnLoadTemplateAndSwitch = remember(viewModel) {
        { account: GuestAccount ->
            viewModel.accountId = account.accountId
            viewModel.accountUid = account.uid
            viewModel.accountPass = account.pass
            viewModel.templateLabel = account.label
            activeTab = 1 // Switch to Creator Form automatically
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
        ) {
            // Elegant top Game Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF231411), SurfaceDark)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(GarenaOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "G-SWAP UTILITY",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "BỘ QUẢN LÝ GUEST ACCOUNT FREE FIRE",
                                color = GarenaAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!permissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF321915)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ CHƯA CÓ QUYỀN TRUY CẬP THƯ MỤC",
                                    color = Color(0xFFFFCDD2),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        onRequestPermission()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GarenaOrange),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .testTag("btn_request_storage_permission")
                                ) {
                                    Text("CẤP QUYỀN TRÊN ĐIỆN THOẠI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for organized single-view sections
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = SurfaceDark,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = GarenaOrange
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("HOÁN ĐỔI 1-1", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = GarenaOrange,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("TẠO GUEST DAT", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = GarenaOrange,
                    unselectedContentColor = Color.Gray
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("MẪU LƯU & LOGS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    selectedContentColor = GarenaOrange,
                    unselectedContentColor = Color.Gray
                )
            }

            // Selected Section Panels
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SurfaceDark)
            ) {
                when (activeTab) {
                    0 -> {
                        SwapControlPanel(
                            viewModel = viewModel,
                            onInspectFile = currentOnInspect,
                            onCloseInspect = currentOnCloseInspect,
                            onExecuteSwap = currentOnExecuteSwap,
                            onDeleteFile = currentOnDeleteFile,
                            onRefresh = currentOnRefresh,
                            clipboardManager = clipboardManager,
                            context = context
                        )
                    }

                    1 -> {
                        AccountCreatorCreatorForm(
                            viewModel = viewModel,
                            onSubmit = currentOnSubmit,
                            onLoadTemplate = currentOnLoadTemplate
                        )
                    }

                    2 -> {
                        HistoryAndSavedTemplatesSection(
                            viewModel = viewModel,
                            onDeleteTemplate = currentOnDeleteTemplate,
                            onClearLogs = currentOnClearLogs,
                            onLoadTemplate = currentOnLoadTemplateAndSwitch
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwapControlPanel(
    viewModel: MainViewModel,
    onInspectFile: (FileInfo) -> Unit,
    onCloseInspect: () -> Unit,
    onExecuteSwap: (String) -> Unit,
    onDeleteFile: (Boolean, String) -> Unit,
    onRefresh: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val activityFiles by viewModel.activityFiles.collectAsState()
    val indoFiles by viewModel.indoFiles.collectAsState()
    val inspectedFile by viewModel.inspectedFile.collectAsState()
    val isSandbox by viewModel.isSandboxMode.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Folder labels summary bar with direct full reload button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📁 DANH SÁCH FILE GUEST DAT HIỆN TẠI",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF20222F), CircleShape)
                    .testTag("btn_refresh")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    tint = GarenaAmber,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Inspected file credentials detailed preview drawer
        AnimatedVisibility(
            visible = inspectedFile != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            inspectedFile?.let { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF232635)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Chi tiết",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "NỘI DUNG FILE GUEST DAT",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onCloseInspect,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Đóng",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // File title
                        Text(
                            text = file.name,
                            color = GarenaAmber,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (file.uid != null && file.pass != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CodeGray, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("com.garena.msdk.guest_uid", color = Color.Gray, fontSize = 10.sp)
                                        Text(file.uid, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(file.uid))
                                            Toast.makeText(context, "Copied UID!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Copy UID",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFF32364C), modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("com.garena.msdk.guest_password", color = Color.Gray, fontSize = 10.sp)
                                        Text(file.pass, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(file.pass))
                                            Toast.makeText(context, "Copied Password!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Copy Pass",
                                            tint = AccentCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "⚠️ File DAT này trống hoặc không đúng cấu trúc Garena!",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Two Split Sections for ACTIVITY and ACC
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Left Half: ACTIVITY Folder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(end = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GarenaOrange.copy(alpha = 0.15f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .border(1.dp, GarenaOrange.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "⚡ ĐƯỜNG DẪN ACC (GỐC)",
                            color = GarenaOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activityFiles.size} files hiện diện",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CardBackground)
                        .border(
                            1.dp,
                            Color(0xFF2C3040),
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                ) {
                    if (activityFiles.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Thư mục\nACTIVITY trống",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = activityFiles,
                                key = { it.path }
                            ) { file ->
                                FileListItem(
                                    fileInfo = file,
                                    canSwap = true,
                                    isActivityBase = true,
                                    onInspect = onInspectFile,
                                    onSwap = onExecuteSwap,
                                    onDelete = onDeleteFile,
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Half: ACC Folder
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GarenaAmber.copy(alpha = 0.15f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .border(1.dp, GarenaAmber.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "📁 ĐƯỜNG DẪN CẦN CHUYỂN",
                            color = GarenaAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${indoFiles.size} files dự phòng",
                            color = Color.Gray,
                            fontSize = 8.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CardBackground)
                        .border(
                            1.dp,
                            Color(0xFF2C3040),
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        )
                ) {
                    if (indoFiles.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Thư mục\nACC trống",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = indoFiles,
                                key = { it.path }
                            ) { file ->
                                FileListItem(
                                    fileInfo = file,
                                    canSwap = false,
                                    isActivityBase = false,
                                    onInspect = onInspectFile,
                                    onSwap = onExecuteSwap,
                                    onDelete = onDeleteFile,
                                    formatDate = { viewModel.formatDate(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Visual File Card representing a single Guest Dat file inside direct folders
@Composable
fun FileListItem(
    fileInfo: FileInfo,
    canSwap: Boolean,
    isActivityBase: Boolean,
    onInspect: (FileInfo) -> Unit,
    onSwap: (String) -> Unit,
    onDelete: (Boolean, String) -> Unit,
    formatDate: (Long) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("file_card_${fileInfo.name}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF20222F)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF292C3D))
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            // File Name & Trash bin Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = fileInfo.name
                        .replace("guest100067.dat", "")
                    Text(
                        text = if (displayName.isNotEmpty()) displayName else "Dạng Gốc",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = fileInfo.name,
                        color = Color.Gray,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = { onDelete(isActivityBase, fileInfo.name) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${fileInfo.size} B • ${formatDate(fileInfo.lastModified)}",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )

                // Swap trigger button (Available in Activity only)
                if (canSwap) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GarenaOrange)
                            .clickable { onSwap(fileInfo.name) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SWAP",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Hoán đổi",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Micro detail button shortcut
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161824), RoundedCornerShape(4.dp))
                    .clickable { onInspect(fileInfo) }
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔍 Xem UID Mật Khẩu",
                    color = AccentCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Creator panel containing input text fields and Fast-fill helpers for effortless testing
@Composable
fun AccountCreatorCreatorForm(
    viewModel: MainViewModel,
    onSubmit: () -> Unit,
    onLoadTemplate: (GuestAccount) -> Unit
) {
    val guestAccounts by viewModel.guestAccounts.collectAsState()
    val scrollState = rememberScrollState()

    val accountId = viewModel.accountId
    val accountUid = viewModel.accountUid
    val accountPass = viewModel.accountPass
    val saveAsTemplate = viewModel.saveAsTemplate
    val templateLabel = viewModel.templateLabel

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Form Title
        Text(
            text = "✍️ NHẬP THÔNG TIN TÀI KHOẢN GUEST MỚI",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Account ID Field
        OutlinedTextField(
            value = accountId,
            onValueChange = { viewModel.accountId = it },
            label = { Text("ACCOUNT ID (Tên File Phân Biệt, vd: AccViet01)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GarenaOrange,
                unfocusedBorderColor = Color(0xFF2C3040),
                focusedLabelColor = GarenaOrange,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("input_account_id"),
            singleLine = true
        )

        // Guest UID Field
        OutlinedTextField(
            value = accountUid,
            onValueChange = { viewModel.accountUid = it },
            label = { Text("UID CỦA TÀI KHOẢN GUEST (9-12 Số)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GarenaOrange,
                unfocusedBorderColor = Color(0xFF2C3040),
                focusedLabelColor = GarenaOrange,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("input_uid"),
            singleLine = true
        )

        // Guest Password Field
        OutlinedTextField(
            value = accountPass,
            onValueChange = { viewModel.accountPass = it },
            label = { Text("PASS TÀI KHOẢN GUEST") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GarenaOrange,
                unfocusedBorderColor = Color(0xFF2C3040),
                focusedLabelColor = GarenaOrange,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("input_password"),
            singleLine = true
        )

        // Back up database templates settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161824), RoundedCornerShape(8.dp))
                .clickable { viewModel.saveAsTemplate = !saveAsTemplate }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = saveAsTemplate,
                onCheckedChange = { viewModel.saveAsTemplate = it },
                colors = CheckboxDefaults.colors(checkedColor = GarenaOrange)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "LƯU VÀO THƯ VIỆN DATABASE TRÊN APP",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sao lưu vĩnh viễn cấu hình này tránh mất pass.",
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(visible = saveAsTemplate) {
            OutlinedTextField(
                value = templateLabel,
                onValueChange = { viewModel.templateLabel = it },
                label = { Text("Nhãn Lưu Trữ (Mô tả, vd: TK Vip Rank Thách Đấu)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color(0xFF2C3040),
                    focusedLabelColor = AccentCyan,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )
        }

        // Submit Big Button
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btn_create_account"),
            colors = ButtonDefaults.buttonColors(containerColor = GarenaOrange),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tạo mới +",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("TẠO FILE DAT & LƯU VÀO ACTIVITY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Inline listing of recently stored templates in Room as fast load
        if (guestAccounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "⚡ CLICK LOAD MẪU ĐÃ LƯU TRƯỚC ĐÓ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                guestAccounts.take(3).forEach { account ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2C3040), RoundedCornerShape(8.dp))
                            .clickable { onLoadTemplate(account) }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = account.label,
                                color = AccentCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "UID: ${account.uid}",
                                color = Color.LightGray,
                                fontSize = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Saved templates database explorer + Operation Transaction list
@Composable
fun HistoryAndSavedTemplatesSection(
    viewModel: MainViewModel,
    onDeleteTemplate: (GuestAccount) -> Unit,
    onClearLogs: () -> Unit,
    onLoadTemplate: (GuestAccount) -> Unit
) {
    val guestAccounts by viewModel.guestAccounts.collectAsState()
    val actionLogs by viewModel.actionLogs.collectAsState()
    var subTabSelection by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Secondary Pill Switches
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (subTabSelection == 0) GarenaOrange else Color(0xFF1F2231))
                    .clickable { subTabSelection = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💼 Thư Viện Mẫu (${guestAccounts.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (subTabSelection == 1) GarenaOrange else Color(0xFF1F2231))
                    .clickable { subTabSelection = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📜 Lịch Sử Log (${actionLogs.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Sub views active content listings
        if (subTabSelection == 0) {
            if (guestAccounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Trống",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chưa có mẫu tài khoản nào được lưu.\nTích chọn 'LƯU VÀO DATABASE APP' khi tạo tài khoản!",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = guestAccounts,
                        key = { it.id }
                    ) { account ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2C3040))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "ID: ${account.accountId} | UID: ${account.uid}",
                                        color = AccentCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Pass: ${account.pass}",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onLoadTemplate(account) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(GarenaOrange.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Nạp mẫu",
                                            tint = GarenaOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { onDeleteTemplate(account) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF2E1A1A), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa mẫu",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Logs tab
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vết giao dịch hoạt động gần đây (gốc database)",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Dọn sạch",
                        color = Color(0xFFEF5350),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearLogs() }
                            .testTag("btn_clear_logs")
                    )
                }

                if (actionLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lịch sử hoạt động rỗng.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = actionLogs,
                            key = { log -> log.id }
                        ) { log ->
                            ActionLogItem(log = log)
                        }
                    }
                }
            }
        }
    }
}

// Single Action Log Item drawing
@Composable
fun ActionLogItem(log: ActionLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151D)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            val formattedTime = remember(log.timestamp) {
                try {
                    val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
                    sdf.format(Date(log.timestamp))
                } catch (e: Exception) {
                    "-"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.actionType,
                    color = when (log.actionType) {
                        "LỖI", "LỖI HOÁN ĐỔI" -> Color(0xFFEF5350)
                        "CẤU HÌNH" -> AccentCyan
                        "HOÁN ĐỔI 1-1" -> GarenaAmber
                        else -> GarenaOrange
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )

                Text(
                    text = formattedTime,
                    color = Color.Gray,
                    fontSize = 8.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = log.description,
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}


