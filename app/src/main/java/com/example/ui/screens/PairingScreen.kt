package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Device
import com.example.ui.components.QrCodeView
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardDanger
import com.example.ui.theme.GuardNavyCard
import com.example.ui.theme.GuardNavyDark
import com.example.ui.theme.GuardSky
import com.example.ui.theme.GuardSuccess
import com.example.viewmodel.FindGuardViewModel
import com.example.viewmodel.UiState

@Composable
fun PairingScreen(
    state: UiState,
    viewModel: FindGuardViewModel,
    modifier: Modifier = Modifier
) {
    val currentDev = viewModel.getCurrentDevice()
    val remoteDev = viewModel.getRemoteDevice()
    val isPaired = currentDev.isPaired

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Perspective Switcher Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GuardNavyCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "สลับอุปกรณ์",
                                tint = GuardCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "สลับมุมมองอุปกรณ์ (Dual Device Simulator)",
                                style = MaterialTheme.typography.labelMedium.copy(color = GuardCyan),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.switchViewingDevice(state.deviceA.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.viewingDeviceId == state.deviceA.id) GuardSky else Color(0xFF1E293B),
                                contentColor = if (state.viewingDeviceId == state.deviceA.id) GuardNavyDark else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_device_a_button")
                        ) {
                            Text("📱 เครื่อง A (Pixel)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.switchViewingDevice(state.deviceB.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.viewingDeviceId == state.deviceB.id) GuardSky else Color(0xFF1E293B),
                                contentColor = if (state.viewingDeviceId == state.deviceB.id) GuardNavyDark else Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_device_b_button")
                        ) {
                            Text("📱 เครื่อง B (Galaxy)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Device Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F3A)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isPaired) GuardSuccess else Color(0xFF475569)
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isPaired) GuardSuccess.copy(alpha = 0.2f) else Color(0xFF334155)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPaired) Icons.Default.Link else Icons.Default.LinkOff,
                                    contentDescription = "Status",
                                    tint = if (isPaired) GuardSuccess else Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentDev.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "รหัสอุปกรณ์: ${currentDev.id}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = GuardCyan)
                                )
                            }
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPaired) GuardSuccess.copy(alpha = 0.2f) else Color(0xFF334155))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPaired) "เชื่อมต่อแล้ว" else "ยังไม่เชื่อมต่อ",
                                color = if (isPaired) GuardSuccess else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isPaired) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = "Paired with",
                                tint = GuardSky,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "เชื่อมโยงกับ: ${remoteDev.name}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "แบตเตอรี่: ${remoteDev.battery}% | สถานะ: ${remoteDev.lastSeen}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { viewModel.unpairDevices() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardDanger),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unpair_button")
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ยกเลิกการเชื่อมต่ออุปกรณ์")
                        }
                    }
                }
            }
        }

        // Pairing Action Buttons (QR Code & Scan)
        if (!isPaired) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GuardNavyCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ขั้นตอนการเชื่อมต่อกันและกัน",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "1. ให้อุปกรณ์เครื่องหนึ่งกด 'สร้าง QR Code'\n2. อีกเครื่องกด 'สแกน QR Code'\n3. ทั้ง 2 เครื่องจะมีหน้าต่างกดยืนยัน 'ตกลง' พร้อมกัน",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCBD5E1),
                                lineHeight = 22.sp
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.setShowQrDialog(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = GuardSky),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_qr_button")
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "QR",
                                    tint = GuardNavyDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("สร้าง QR Code", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.initiatePairing() },
                                colors = ButtonDefaults.buttonColors(containerColor = GuardCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("scan_qr_button")
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan",
                                    tint = GuardNavyDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("สแกน / ส่งคำขอ", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Real Cloud Pair Code Section for Two Separate Physical Phones
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌐 ช่องสัญญาณคลาวด์สำหรับ 2 เครื่องจริง:",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "● ออนไลน์สด",
                                    color = GuardSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = state.pairCodeInput.ifEmpty { state.activePairCode },
                                    onValueChange = { viewModel.updatePairCodeInput(it) },
                                    label = { Text("รหัสจับคู่ (Pair Code)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("cloud_pair_code_input")
                                )
                                Button(
                                    onClick = {
                                        val code = state.pairCodeInput.ifEmpty { state.activePairCode }
                                        viewModel.initiatePairing(code)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GuardSky),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("apply_pair_code_button")
                                ) {
                                    Text("เชื่อมต่อ", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Code Display Dialog
    if (state.showQrDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowQrDialog(false) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setShowQrDialog(false)
                        viewModel.initiatePairing()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GuardSky)
                ) {
                    Text("จำลองอีกเครื่องมาสแกนทันที", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowQrDialog(false) }) {
                    Text("ปิด", color = Color(0xFF94A3B8))
                }
            },
            title = {
                Text(
                    text = "QR Code สำหรับเชื่อมต่อ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "นำอุปกรณ์อีกเครื่องมาสแกน QR Code นี้",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    QrCodeView(data = "FINDGUARD:${currentDev.id}:${currentDev.name}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "รหัสจับคู่: ${currentDev.id}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = GuardCyan,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            },
            containerColor = Color(0xFF131F3A)
        )
    }

    // Mutual Pairing Consent Dialog (ขึ้นที่ทั้ง 2 เครื่อง)
    state.activePairRequest?.let { req ->
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = GuardSky,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ต้องการเชื่อมต่ออุปกรณ์ไหม?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "ระบบความปลอดภัย: ทั้ง 2 เครื่องต้องกดยืนยัน 'ตกลง' ร่วมกัน จึงจะสามารถดูแผนที่ ส่งเสียง และล็อคเครื่องระยะไกลได้",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )

                    // Device A Response Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📱 เครื่อง A (${state.deviceA.name})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (state.deviceAAccepted) {
                                        true -> "✅ ตกลงแล้ว"
                                        false -> "❌ ไม่ตกลง"
                                        null -> "⏳ กำลังรอตัดสินใจ"
                                    },
                                    fontSize = 12.sp,
                                    color = when (state.deviceAAccepted) {
                                        true -> GuardSuccess
                                        false -> GuardDanger
                                        null -> Color(0xFFFBBF24)
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.respondPairingDeviceA(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GuardSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("accept_device_a")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ตกลง", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.respondPairingDeviceA(false) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardDanger),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("decline_device_a")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ไม่ตกลง", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Device B Response Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📱 เครื่อง B (${state.deviceB.name})",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (state.deviceBAccepted) {
                                        true -> "✅ ตกลงแล้ว"
                                        false -> "❌ ไม่ตกลง"
                                        null -> "⏳ กำลังรอตัดสินใจ"
                                    },
                                    fontSize = 12.sp,
                                    color = when (state.deviceBAccepted) {
                                        true -> GuardSuccess
                                        false -> GuardDanger
                                        null -> Color(0xFFFBBF24)
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.respondPairingDeviceB(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GuardSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("accept_device_b")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ตกลง", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.respondPairingDeviceB(false) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GuardDanger),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("decline_device_b")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ไม่ตกลง", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF131F3A)
        )
    }
}
