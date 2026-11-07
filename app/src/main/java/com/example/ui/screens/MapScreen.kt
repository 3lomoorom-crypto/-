package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GuardCyan
import com.example.ui.theme.GuardDanger
import com.example.ui.theme.GuardNavyCard
import com.example.ui.theme.GuardNavyDark
import com.example.ui.theme.GuardNavySurface
import com.example.ui.theme.GuardSky
import com.example.ui.theme.GuardSuccess
import com.example.viewmodel.FindGuardViewModel
import com.example.viewmodel.UiState

@Composable
fun MapScreen(
    state: UiState,
    viewModel: FindGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentDev = viewModel.getCurrentDevice()
    val remoteDev = viewModel.getRemoteDevice()
    val isPaired = currentDev.isPaired

    val distanceMeters = viewModel.calculateDistanceMeters(
        currentDev.latitude, currentDev.longitude,
        remoteDev.latitude, remoteDev.longitude
    )

    val distanceText = if (distanceMeters < 1000) {
        "$distanceMeters เมตร"
    } else {
        String.format("%.2f กม.", distanceMeters / 1000.0)
    }

    // Animated radar ripple for GPS tracking
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GuardNavyDark)
    ) {
        // Top GPS Info Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GuardNavySurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "GPS",
                    tint = GuardSky,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPaired) "เชื่อมต่อพิกัดกับ ${remoteDev.name}" else "แสดงพิกัดเครื่องปัจจุบัน",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPaired) GuardSuccess.copy(alpha = 0.2f) else Color(0xFF334155))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isPaired) "GPS สด" else "เดี่ยว",
                    color = if (isPaired) GuardSuccess else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Map View Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F172A))
        ) {
            // Stylized vector map canvas with road grid and pins
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Background grid lines (representing city map blocks)
                val gridColor = Color(0xFF1E293B)
                val streetColor = Color(0xFF283548)
                val mainRoadColor = Color(0xFF334155)

                // Secondary roads
                for (x in 0..w.toInt() step 60) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), 1.5f)
                }
                for (y in 0..h.toInt() step 60) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(w, y.toFloat()), 1.5f)
                }

                // Main avenues
                drawLine(streetColor, Offset(w * 0.2f, 0f), Offset(w * 0.2f, h), 6f)
                drawLine(streetColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), 6f)
                drawLine(mainRoadColor, Offset(0f, h * 0.45f), Offset(w, h * 0.45f), 8f)
                drawLine(mainRoadColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 6f)

                // Diagonal expressway
                val highwayPath = Path().apply {
                    moveTo(0f, h * 0.15f)
                    cubicTo(w * 0.35f, h * 0.3f, w * 0.65f, h * 0.55f, w, h * 0.85f)
                }
                drawPath(highwayPath, Color(0xFF38BDF8).copy(alpha = 0.25f), style = Stroke(width = 7f))

                // Points of interest:
                // Device A Position (Current)
                val devAPos = Offset(w * 0.38f, h * 0.65f)
                // Device B Position (Target/Lost Phone)
                val devBPos = Offset(w * 0.68f, h * 0.32f)

                if (isPaired) {
                    // Connection / Distance dash line
                    drawLine(
                        color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                        start = devAPos,
                        end = devBPos,
                        strokeWidth = 3f
                    )

                    // Target radar ripple
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = (1f - radarPulse) * 0.5f),
                        radius = 80f * radarPulse,
                        center = devBPos
                    )
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = (1f - radarPulse) * 0.8f),
                        radius = 45f * radarPulse,
                        center = devBPos
                    )

                    // Device B Target Pin (Red / Lost Phone)
                    drawCircle(Color(0xFFEF4444), radius = 16f, center = devBPos)
                    drawCircle(Color.White, radius = 6f, center = devBPos)
                }

                // Current Device Pin (Blue / Primary)
                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.3f), radius = 32f, center = devAPos)
                drawCircle(Color(0xFF2563EB), radius = 16f, center = devAPos)
                drawCircle(Color.White, radius = 6f, center = devAPos)
            }

            // Pin Labels Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 40.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("🔵 ${currentDev.name} (ที่นี่)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (isPaired) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 24.dp, top = 40.dp)
                        .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔴 ${remoteDev.name} ($distanceText)", color = Color(0xFFFDA4AF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Bottom Map Controls & Location Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GuardNavySurface),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isPaired) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "พิกัดอุปกรณ์: ${remoteDev.name}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = remoteDev.locationName,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1))
                            )
                        }

                        // Battery and distance badge
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = GuardSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${remoteDev.battery}%", color = GuardSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("ห่าง $distanceText", color = GuardSky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Precise Lat/Lng
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ละติจูด: ${remoteDev.latitude} | ลองจิจูด: ${remoteDev.longitude}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = remoteDev.lastSeen,
                            color = GuardCyan,
                            fontSize = 11.sp
                        )
                    }

                    // Map Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Real Google Maps Open Intent Button
                        Button(
                            onClick = {
                                viewModel.openInGoogleMaps(
                                    context = context,
                                    lat = remoteDev.latitude,
                                    lon = remoteDev.longitude,
                                    label = remoteDev.name
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardSky),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("open_google_maps_button")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = "Maps", tint = GuardNavyDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("เปิดใน Google Maps", color = GuardNavyDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // Quick Ring Bell Button from Map
                        OutlinedButton(
                            onClick = { viewModel.triggerBell(1) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("quick_bell_from_map")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = "Bell", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ส่งเสียง", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Unpaired hint
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ยังไม่ได้เชื่อมต่อกับอุปกรณ์อื่น",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ไปที่แท็บ 'อุปกรณ์' เพื่อเชื่อมต่อด้วย QR Code ทั้ง 2 เครื่อง จึงจะเห็นพิกัดของกันและกัน",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.selectTab(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardSky),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ไปหน้าเชื่อมต่ออุปกรณ์", color = GuardNavyDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
