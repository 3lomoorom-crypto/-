package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardDanger
import com.example.ui.theme.GuardNavyCard
import com.example.ui.theme.GuardNavyDark
import com.example.ui.theme.GuardSky
import com.example.ui.theme.GuardSuccess
import com.example.viewmodel.FindGuardViewModel
import com.example.viewmodel.UiState

@Composable
fun LockScreenControl(
    state: UiState,
    viewModel: FindGuardViewModel,
    modifier: Modifier = Modifier
) {
    val currentDev = viewModel.getCurrentDevice()
    val remoteDev = viewModel.getRemoteDevice()
    val isPaired = currentDev.isPaired

    var customMessage by remember { mutableStateOf(remoteDev.lockMessage) }
    var contactPhone by remember { mutableStateOf(remoteDev.contactPhone) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (remoteDev.isLocked) Color(0xFF2A0D12) else GuardNavyCard
                ),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (remoteDev.isLocked) GuardDanger else Color(0xFF334155)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (remoteDev.isLocked) GuardDanger.copy(alpha = 0.2f)
                                        else GuardSuccess.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (remoteDev.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock State",
                                    tint = if (remoteDev.isLocked) GuardDanger else GuardSuccess,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "สถานะเครื่อง: ${remoteDev.name}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (remoteDev.isLocked) "🔴 กำลังถูกล็อคระยะไกล (Lost Mode)" else "🟢 สถานะปกติ (ปลดล็อคอยู่)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (remoteDev.isLocked) GuardDanger else GuardSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Remote Lock or Unlock Action Button
                    if (remoteDev.isLocked) {
                        Button(
                            onClick = { viewModel.unlockRemoteDevice() },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("remote_unlock_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = GuardNavyDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("สั่งปลดล็อคเครื่องระยะไกล", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.lockRemoteDevice(customMessage, contactPhone) },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardDanger),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("remote_lock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("สั่งล็อคเครื่อง ${remoteDev.name} ทันที", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test shortcut: switch view to the locked device
                    if (remoteDev.isLocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.switchViewingDevice(remoteDev.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("switch_to_locked_device_button")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = GuardSky, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("สลับไปดูหน้าจอล็อคบน ${remoteDev.name} (เพื่อทดสอบปลดล็อค)", color = GuardSky, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Customization of Lost Mode Screen Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F3A)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ตั้งค่าข้อความที่จะแสดงเมื่อเครื่องถูกล็อค",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Message text field
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { customMessage = it },
                        label = { Text("ข้อความฉุกเฉินบนหน้าจอล็อค") },
                        leadingIcon = { Icon(Icons.Default.Message, contentDescription = null, tint = GuardSky) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GuardSky,
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_message_input")
                    )

                    // Phone text field
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("เบอร์โทรฉุกเฉินให้ผู้พบเห็นโทรกลับ") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = GuardSky) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GuardSky,
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_phone_input")
                    )

                    // PIN Code Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pin,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "รหัส PIN ปลดล็อคบนหน้าจอ: ${remoteDev.unlockPin}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "ผู้ที่ถือเครื่องอยู่สามารถกรอกรหัส PIN นี้เพื่อปลดล็อคได้ทันที",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Security Principle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GuardNavyCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GuardCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "การปกป้องข้อมูลอุปกรณ์",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "เมื่อเปิดใช้โหมดสูญหาย หน้าจอจะถูกล็อคด้วยข้อความฉุกเฉิน บังคับเปิดตำแหน่ง GPS เพื่อส่งพิกัดให้อุปกรณ์คู่หู และมีปุ่มโทรออกฉุกเฉินหาเจ้าของได้โดยตรง",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
