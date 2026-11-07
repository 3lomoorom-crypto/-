package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.components.LostModeLockScreen
import com.example.ui.screens.BellScreen
import com.example.ui.screens.LockScreenControl
import com.example.ui.screens.MapScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardNavyCard
import com.example.ui.theme.GuardNavyDark
import com.example.ui.theme.GuardNavySurface
import com.example.ui.theme.GuardSky
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FindGuardViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FindGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: FindGuardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val currentDevice = viewModel.getCurrentDevice()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Runtime Permission Request for Location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    LaunchedEffect(Unit) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fineLocation != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Show Toast messages via Snackbar
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    // If current viewing device is locked by remote control -> Show full-screen Lost Mode Screen
    if (currentDevice.isLocked) {
        LostModeLockScreen(
            device = currentDevice,
            pinInput = state.pinInput,
            pinError = state.pinError,
            onDigitClick = { viewModel.appendPinDigit(it) },
            onDeleteClick = { viewModel.deletePinDigit() }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Logo",
                                tint = GuardSky,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FindGuard",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    actions = {
                        // Current Device Perspective Tag (Clickable to switch)
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GuardNavyCard)
                                .clickable {
                                    val nextDev = if (state.viewingDeviceId == state.deviceA.id) state.deviceB.id else state.deviceA.id
                                    viewModel.switchViewingDevice(nextDev)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("toggle_device_top_bar")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (state.viewingDeviceId == state.deviceA.id) "📱 เครื่อง A" else "📱 เครื่อง B",
                                    color = GuardCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch",
                                    tint = GuardSky,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GuardNavyDark,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GuardNavySurface,
                    contentColor = Color.White
                ) {
                    val tabs = listOf(
                        Triple(0, stringResource(R.string.nav_map), Icons.Default.Map),
                        Triple(1, stringResource(R.string.nav_devices), Icons.Default.QrCode),
                        Triple(2, stringResource(R.string.nav_bell), Icons.Default.NotificationsActive),
                        Triple(3, stringResource(R.string.nav_lock), Icons.Default.Lock)
                    )

                    tabs.forEach { (index, title, icon) ->
                        NavigationBarItem(
                            selected = state.currentTab == index,
                            onClick = { viewModel.selectTab(index) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title
                                )
                            },
                            label = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GuardNavyDark,
                                selectedTextColor = GuardSky,
                                indicatorColor = GuardSky,
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("nav_item_$index")
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = GuardNavyDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (state.currentTab) {
                    0 -> MapScreen(state = state, viewModel = viewModel)
                    1 -> PairingScreen(state = state, viewModel = viewModel)
                    2 -> BellScreen(state = state, viewModel = viewModel)
                    3 -> LockScreenControl(state = state, viewModel = viewModel)
                }
            }
        }
    }
}
