package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardNavyCard
import com.example.ui.theme.GuardNavyDark
import com.example.ui.theme.GuardSky
import com.example.viewmodel.FindGuardViewModel
import com.example.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BellScreen(
    state: UiState,
    viewModel: FindGuardViewModel,
    modifier: Modifier = Modifier
) {
    val currentDev = viewModel.getCurrentDevice()
    val remoteDev = viewModel.getRemoteDevice()
    val isPaired = currentDev.isPaired

    // Bell swing animation when active
    val infiniteTransition = rememberInfiniteTransition(label = "bellSwing")
    val bellRotation by infiniteTransition.animateFloat(
        initialValue = if (state.isRingingNow) -18f else 0f,
        targetValue = if (state.isRingingNow) 18f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bellRotation"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bell Header & Target Device Indicator
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GuardNavyCard),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ระบบส่งเสียงระฆังเรียกเครื่อง",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isPaired) {
                            "เป้าหมาย: ${remoteDev.name} (ดังแม้ตั้งโหมดเงียบ)"
                        } else {
                            "ส่งเสียงทดสอบบนเครื่องนี้ (ยังไม่ได้จับคู่อีกเครื่อง)"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isPaired) GuardSky else Color(0xFF94A3B8)
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bell Visual with concentric sound waves
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        if (state.isRingingNow) {
                            // Outer sound wave
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .scale(waveScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            )
                            // Inner sound wave
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(waveScale * 0.9f)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                            )
                        }

                        // Golden Bell Core
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(100.dp)
                                .rotate(if (state.isRingingNow) bellRotation else 0f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFDE68A),
                                            Color(0xFFF59E0B),
                                            Color(0xFFB45309)
                                        )
                                    )
                                )
                                .border(3.dp, Color(0xFFFEF3C7), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (state.isRingingNow) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Bell",
                                tint = Color(0xFF78350F),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    if (state.isRingingNow) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🔔 กำลังดังครั้งที่ ${state.currentRingingCount}...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Action Buttons: 1 chime vs 2 chimes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F3A)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "เลือกจำนวนครั้งที่ต้องการให้ระฆังส่งเสียง:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Button: Ring 1 Time
                    Button(
                        onClick = { viewModel.triggerBell(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("ring_bell_1_time_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🔔 กดส่งเสียงระฆัง 1 ครั้ง",
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Button: Ring 2 Times
                    Button(
                        onClick = { viewModel.triggerBell(2) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEAB308)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("ring_bell_2_times_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFF78350F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "🔔🔔 กดส่งเสียงระฆัง 2 ครั้ง",
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "* ระบบส่งเสียงจะทำงานด้วยระดับเสียงสูงสุดและสั่นเตือนเครื่อง เพื่อให้หาเครื่องเจอได้ทันทีแม้ตกอยู่ใต้เบาะหรือซอกกระเป๋า",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bell Ringing History Log
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GuardNavyCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "ประวัติ",
                            tint = GuardCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ประวัติการส่งเสียงล่าสุด",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (state.bellHistory.isEmpty()) {
                        Text(
                            text = "ยังไม่มีประวัติการส่งเสียง กดปุ่มด้านบนเพื่อทดสอบส่งเสียง",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.bellHistory.take(5).forEach { alert ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (alert.ringCount == 1) "🔔" else "🔔🔔", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "ส่งเสียง ${alert.ringCount} ครั้ง -> ${alert.targetDeviceName}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "จาก: ${alert.fromDeviceName}",
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = dateFormat.format(Date(alert.timestamp)),
                                        color = GuardCyan,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
