package com.zuri.cartrack

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
//import androidx.compose.ui.viewinterop.AndroidView
//import com.naver.maps.geometry.LatLng
//import com.naver.maps.map.MapView
//import com.naver.maps.map.CameraUpdate
//import com.naver.maps.map.overlay.PolylineOverlay
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
//import com.naver.maps.geometry.LatLngBounds
//import com.naver.maps.map.overlay.Marker
//import com.zuri.cartrack.ui.theme.BlackBg1
//import com.zuri.cartrack.ui.theme.BlackBg2
//import com.zuri.cartrack.ui.theme.BlackBg3
import com.zuri.cartrack.ui.theme.CardDark
import com.zuri.cartrack.ui.theme.NeonPurple
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
//import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )

        setContent {
            val context = LocalContext.current
            val activity = context as Activity

            var tracking by remember {
                mutableStateOf(intent.getBooleanExtra("auto_start", false))
            }
            var finished by remember { mutableStateOf(false) }
            var showHistory by remember { mutableStateOf(false) }
            var showRanking by remember { mutableStateOf(false) }
            var selectedRecord by remember { mutableStateOf<TripRecord?>(null) }
            var showExitDialog by remember { mutableStateOf(false) }
            var showBluetoothDialog by remember { mutableStateOf(false) }
            var bluetoothName by remember {
                mutableStateOf(
                    getSharedPreferences("cartrack_prefs", MODE_PRIVATE)
                        .getString("car_bluetooth_name", "")
                        ?: ""
                )
            }

            LaunchedEffect(Unit) {
                if (intent.getBooleanExtra("auto_start", false)) {
                    ContextCompat.startForegroundService(
                        this@MainActivity,
                        Intent(this@MainActivity, TrackingService::class.java)
                    )
                }
            }

            val isHome = selectedRecord == null && !showHistory && !showRanking && !finished && !tracking

            BackHandler(enabled = selectedRecord != null) {
                selectedRecord = null
                showHistory = true
            }

            BackHandler(enabled = selectedRecord == null && showRanking) {
                showRanking = false
            }

            BackHandler(enabled = selectedRecord == null && showHistory) {
                showHistory = false
            }

            BackHandler(enabled = selectedRecord == null && finished) {
                finished = false
            }

            BackHandler(enabled = isHome) {
                showExitDialog = true
            }

            MaterialTheme {
                when {
                    selectedRecord != null -> TripDetailScreen(
                        record = selectedRecord!!,
                        onBack = {
                            selectedRecord = null
                            showHistory = true
                        }
                    )

                    showRanking -> RankingScreen(
                        records = TripStorage.load(this@MainActivity),
                        onBack = { showRanking = false }
                    )

                    showHistory -> HistoryScreen(
                        records = TripStorage.load(this@MainActivity),
                        onBack = { showHistory = false },
                        onSelect = { selectedRecord = it }
                    )

                    finished -> ResultScreen(
                        onRestart = { finished = false }
                    )

                    else -> TrackingScreen(
                        tracking = tracking,
                        onStart = {
                            tracking = true
                            ContextCompat.startForegroundService(
                                this@MainActivity,
                                Intent(this@MainActivity, TrackingService::class.java)
                            )
                        },
                        onStop = {
                            tracking = false
                            finished = true
                            stopService(Intent(this@MainActivity, TrackingService::class.java))
                            TripStorage.save(this@MainActivity)
                        },
                        onHistory = { showHistory = true },
                        onRanking = { showRanking = true },
                        onSetBluetooth = { showBluetoothDialog = true }
                    )
                }

                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("앱 종료") },
                        text = { Text("종료하시겠습니까?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showExitDialog = false
                                activity.finish()
                            }) {
                                Text("종료")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }

                if (showBluetoothDialog) {
                    AlertDialog(
                        onDismissRequest = { showBluetoothDialog = false },
                        title = { Text("차량 블루투스 등록") },
                        text = {
                            OutlinedTextField(
                                value = bluetoothName,
                                onValueChange = { bluetoothName = it },
                                label = { Text("블루투스 이름") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                getSharedPreferences("cartrack_prefs", MODE_PRIVATE)
                                    .edit()
                                    .putString("car_bluetooth_name", bluetoothName.trim())
                                    .apply()

                                showBluetoothDialog = false
                            }) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBluetoothDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackingScreen(
    tracking: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onHistory: () -> Unit,
    onRanking: () -> Unit,
    onSetBluetooth: () -> Unit
) {
    val currentSpeed = TrackingState.currentSpeed.floatValue
    val totalDistance = TrackingState.totalDistance.floatValue
    val maxSpeed = TrackingState.maxSpeed.floatValue
    val averageSpeed = TrackingState.averageSpeed.floatValue
    val elapsedSeconds = TrackingState.elapsedSeconds.longValue

    AppBackground {
        Spacer(Modifier.height(32.dp))
        if (tracking) {
            DrivingLiveScreen(
                onStop = onStop
            )
        } else {
            ReadyHomeScreen(
                onStart = onStart,
                onHistory = onHistory,
                onRanking = onRanking,
                onSetBluetooth = onSetBluetooth
            )
        }
    }
}

@Composable
fun ResultScreen(onRestart: () -> Unit) {
    val maxSpeed = TrackingState.maxSpeed.floatValue
    val avgSpeed = TrackingState.averageSpeed.floatValue
    val distance = TrackingState.totalDistance.floatValue
    val time = TrackingState.elapsedSeconds.longValue

    AppBackground {
        Text("주행 결과", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Text("%.1f km/h".format(maxSpeed), color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
        Text("최고 속도", color = Color(0xFFB0B7C3), fontSize = 18.sp)

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("총 거리", "%.2f km".format(distance / 1000f), Modifier.weight(1f))
            StatCard("평균 속도", "%.1f km/h".format(avgSpeed), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        StatCard("주행 시간", formatTime(time), Modifier.fillMaxWidth())

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                TrackingState.currentSpeed.floatValue = 0f
                TrackingState.totalDistance.floatValue = 0f
                TrackingState.maxSpeed.floatValue = 0f
                TrackingState.averageSpeed.floatValue = 0f
                TrackingState.elapsedSeconds.longValue = 0L
                TrackingState.points.clear()
                onRestart()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurple
            )
        ) {
            Text("다시 시작", fontSize = 18.sp)
        }
    }
}

@Composable
fun AppBackground(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF050711),
                        Color(0xFF0B1020),
                        Color(0xFF151032),
                        Color(0xFF070A12)
                    )
                )
            )
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = Color(0xFF9AA4B2), fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
fun HistoryScreen(
    records: List<TripRecord>,
    onBack: () -> Unit,
    onSelect: (TripRecord) -> Unit
) {
    val context = LocalContext.current
    var deleteTarget by remember { mutableStateOf<TripRecord?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    val list = remember(refresh) {
        TripStorage.load(context)
    }

    AppBackground {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "주행 기록",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { showClearDialog = true }
            ) {
                Text(
                    "전체 삭제",
                    color = Color.Red,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        if (list.isEmpty()) {
            Text("저장된 기록이 없습니다", color = Color(0xFFB0B7C3), fontSize = 18.sp)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list) { record ->
                    HistoryCard(
                        record = record,
                        onClick = { onSelect(record) },
                        onLongClick = { deleteTarget = record }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Text("뒤로가기", fontSize = 18.sp)
        }

        if (deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("기록 삭제") },
                text = { Text("삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            TripStorage.deleteRecord(context, deleteTarget!!)
                            deleteTarget = null
                            refresh++
                        }
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            deleteTarget = null
                        }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("전체 삭제") },
                text = { Text("모든 기록을 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            TripStorage.clearAll(context)
                            showClearDialog = false
                            refresh++
                        }
                    ) {
                        Text("삭제")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                        }
                    ) {
                        Text("취소")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryCard(
    record: TripRecord,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardDark
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(record.date, color = Color(0xFF9AA4B2), fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("최고 %.1f km/h".format(record.maxSpeed), color = Color.White, fontSize = 18.sp)
                Text("평균 %.1f km/h".format(record.averageSpeed), color = Color.White, fontSize = 18.sp)
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%.2f km".format(record.distanceMeters / 1000f), color = Color(0xFFB0B7C3), fontSize = 16.sp)
                Text(formatTime(record.durationSeconds), color = Color(0xFFB0B7C3), fontSize = 16.sp)
            }
        }
    }
}
@Composable
fun RankingScreen(
    records: List<TripRecord>,
    onBack: () -> Unit
) {
    val topSpeed = records.sortedByDescending { it.maxSpeed }.take(3)
    val topDistance = records.sortedByDescending { it.distanceMeters }.take(3)
    val topAverage = records.sortedByDescending { it.averageSpeed }.take(3)

    AppBackground {
        Text("주행 랭킹", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(24.dp))

        if (records.isEmpty()) {
            Text("랭킹 데이터가 없습니다", color = Color(0xFFB0B7C3), fontSize = 18.sp)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    RankingSection(
                        title = "최고속도 TOP 3",
                        records = topSpeed,
                        valueText = { "%.1f km/h".format(it.maxSpeed) }
                    )
                }

                item {
                    RankingSection(
                        title = "최장거리 TOP 3",
                        records = topDistance,
                        valueText = { "%.2f km".format(it.distanceMeters / 1000f) }
                    )
                }

                item {
                    RankingSection(
                        title = "최고 평균속도 TOP 3",
                        records = topAverage,
                        valueText = { "%.1f km/h".format(it.averageSpeed) }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Text("뒤로가기", fontSize = 18.sp)
        }
    }
}

@Composable
fun TripDetailScreen(record: TripRecord, onBack: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val view = LocalView.current
    val activity = view.context as Activity
    val scope = rememberCoroutineScope()
    var hideButtonsForCapture by remember { mutableStateOf(false) }
    var backgroundUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        backgroundUri = uri
    }

    val backgroundBitmap = remember(backgroundUri) {
        backgroundUri?.let { uri ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(activity.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
                    .copy(Bitmap.Config.ARGB_8888, true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        backgroundBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x55000000))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))

            Text(
                "%.1f km 🚀".format(record.distanceMeters / 1000f),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            TripRankStatPanel(record)

            Spacer(Modifier.height(20.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) { page ->
                if (page == 0) {
                    SpeedChartPanel(record.points)
                } else {
                    RouteMapPanel(record.points)
                }
            }

            Spacer(Modifier.weight(1f))

            if (!hideButtonsForCapture) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("배경")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                hideButtonsForCapture = true
                                delay(120)
                                shareCurrentScreen(activity, view)
                                delay(80)
                                hideButtonsForCapture = false
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF59E6D9))
                    ) {
                        Text("공유", color = Color.Black)
                    }

                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("뒤로")
                    }
                }
            }
        }
    }
}

@Composable
fun MiniRoutePreview(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }

        val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 1.0
        val lngRange = (maxLng - minLng).takeIf { it != 0.0 } ?: 1.0

        val padding = 24f

        fun toOffset(point: TripPoint): Offset {
            val x = padding + ((point.longitude - minLng) / lngRange).toFloat() * (size.width - padding * 2)
            val y = padding + ((maxLat - point.latitude) / latRange).toFloat() * (size.height - padding * 2)
            return Offset(x, y)
        }

        for (i in 1 until points.size) {
            val speed = points[i].speedKmh
            val color = when {
                speed < 30f -> Color(0xFFFF4D5A)
                speed < 80f -> Color(0xFFFFB020)
                speed < 120f -> Color(0xFF43E27B)
                else -> Color(0xFF5BE7E7)
            }

            drawLine(
                color = color,
                start = toOffset(points[i - 1]),
                end = toOffset(points[i]),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(Color(0xFF43E27B), radius = 10f, center = toOffset(points.first()))
        drawCircle(Color(0xFFFF4D5A), radius = 10f, center = toOffset(points.last()))
    }
}

@Composable
fun SpeedChart(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val labelWidth = 58f
        val chartWidth = size.width - labelWidth
        val chartHeight = size.height
        val maxSpeed = points.maxOf { it.speedKmh }.coerceAtLeast(1f)
        val widthStep = chartWidth / (points.size - 1)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(154, 164, 178)
            textSize = 26f
            textAlign = Paint.Align.LEFT
        }

        for (i in 0..4) {
            val y = chartHeight * i / 4f
            drawLine(
                color = Color(0xFF323844),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.5f
            )

            val labelValue = maxSpeed - (maxSpeed * i / 4f)
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    "%.0f".format(labelValue),
                    chartWidth + 8f,
                    y + 9f,
                    textPaint
                )
            }
        }

        val linePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, point ->
            val x = widthStep * index
            val y = chartHeight - (point.speedKmh / maxSpeed) * chartHeight

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, chartHeight)
                fillPath.lineTo(x, y)
            } else {
                val prevX = widthStep * (index - 1)
                val prevY = chartHeight - (points[index - 1].speedKmh / maxSpeed) * chartHeight
                val midX = (prevX + x) / 2f

                linePath.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                linePath.quadraticBezierTo(x, y, x, y)

                fillPath.quadraticBezierTo(prevX, prevY, midX, (prevY + y) / 2f)
                fillPath.quadraticBezierTo(x, y, x, y)
            }
        }

        fillPath.lineTo(chartWidth, chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(
                    Color(0x8859E6D9),
                    Color(0x2259E6D9),
                    Color.Transparent
                )
            )
        )

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val x1 = widthStep * (i - 1)
            val y1 = chartHeight - (prev.speedKmh / maxSpeed) * chartHeight

            val x2 = widthStep * i
            val y2 = chartHeight - (curr.speedKmh / maxSpeed) * chartHeight

            drawLine(
                color = speedGraphColor(curr.speedKmh),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        val maxIndex = points.indexOfFirst { it.speedKmh == maxSpeed }
        val maxX = widthStep * maxIndex
        val maxY = chartHeight - (maxSpeed / maxSpeed) * chartHeight

        drawCircle(Color.White, radius = 9f, center = Offset(maxX, maxY))
        drawCircle(Color(0xFF59E6D9), radius = 5f, center = Offset(maxX, maxY))
    }
}

fun speedGraphColor(speedKmh: Float): Color {
    return when {
        speedKmh < 30f -> Color(0xFFFF4D5A)
        speedKmh < 80f -> Color(0xFFFFB020)
        speedKmh < 120f -> Color(0xFF43E27B)
        else -> Color(0xFF5BE7E7)
    }
}

/*@Composable
fun NaverRouteMap(
    points: List<TripPoint>,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                onCreate(null)

                getMapAsync { naverMap ->
                    naverMap.uiSettings.isZoomControlEnabled = false
                    naverMap.uiSettings.isCompassEnabled = false
                    naverMap.uiSettings.isScaleBarEnabled = false
                    naverMap.uiSettings.isLocationButtonEnabled = false

                    if (points.size >= 2) {
                        val coords = points.map { LatLng(it.latitude, it.longitude) }

                        PolylineOverlay().apply {
                            this.coords = coords
                            width = 10
                            color = android.graphics.Color.rgb(89, 230, 217)
                            map = naverMap
                        }

                        Marker().apply {
                            position = coords.first()
                            captionText = "Start"
                            map = naverMap
                        }

                        Marker().apply {
                            position = coords.last()
                            captionText = "End"
                            map = naverMap
                        }

                        val bounds = LatLngBounds.Builder().apply {
                            coords.forEach { include(it) }
                        }.build()

                        naverMap.moveCamera(
                            CameraUpdate.fitBounds(bounds, 90)
                        )
                    }
                }
            }
        }
    )
}*/

@Composable
fun DetailStatItem(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = icon,
            fontSize = 26.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = value,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = label,
            color = Color(0xFF8F98A8),
            fontSize = 13.sp
        )
    }
}

@Composable
fun TripRankStatPanel(record: TripRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        Column(Modifier.padding(vertical = 28.dp, horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth()) {
                TripRankStat("➤", "%.2f km".format(record.distanceMeters / 1000f), "Distance", Modifier.weight(1f))
                TripRankStat("◉", "%.1f km/h".format(record.averageSpeed), "Avg Speed", Modifier.weight(1f))
            }

            Spacer(Modifier.height(30.dp))

            Row(Modifier.fillMaxWidth()) {
                TripRankStat("◌", "%.1f km/h".format(record.maxSpeed), "Max Speed", Modifier.weight(1f))
                TripRankStat("ϟ", formatTime(record.durationSeconds), "Time", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TripRankStat(
    icon: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, color = Color(0xFF54D8D8), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8E8E94), fontSize = 14.sp)
    }
}

@Composable
fun SpeedChartPanel(points: List<TripPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Speed Over Time",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            SpeedChart(
                points = points,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun RouteMapPanel(points: List<TripPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        if (points.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "지도 데이터 부족",
                    color = Color(0xFF8E8E94),
                    fontSize = 16.sp
                )
            }
        } else {
            /*NaverRouteMap(
                points = points,
                modifier = Modifier.fillMaxSize()
            )*/
            MiniRoutePreview(
                points = points,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }
    }
}

@Composable
fun ReadyHomeScreen(
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onRanking: () -> Unit,
    onSetBluetooth: () -> Unit
) {
    Text("SPEED", color = Color(0xFF8BE9FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Text("TRACKER", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)

    Spacer(Modifier.height(36.dp))

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("READY", color = Color(0xFF59E6D9), fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
    }

    Spacer(Modifier.height(28.dp))

    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth().height(70.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
    ) {
        Text("주행 시작", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(14.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DarkOutlineButton("기록", onHistory, Modifier.weight(1f))
        DarkOutlineButton("랭킹", onRanking, Modifier.weight(1f))
    }

    Spacer(Modifier.height(12.dp))

    OutlinedButton(
        onClick = onSetBluetooth,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text("차량 블루투스 등록", fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DrivingLiveScreen(onStop: () -> Unit) {
    val currentSpeed = TrackingState.currentSpeed.floatValue
    val totalDistance = TrackingState.totalDistance.floatValue
    val maxSpeed = TrackingState.maxSpeed.floatValue
    val averageSpeed = TrackingState.averageSpeed.floatValue
    val elapsedSeconds = TrackingState.elapsedSeconds.longValue

    Text("LIVE DRIVE", color = Color(0xFF59E6D9), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(18.dp))

    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
        shape = RoundedCornerShape(38.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f".format(currentSpeed), color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Black)
                Text("km/h", color = Color(0xFF8E9AAF), fontSize = 20.sp)
            }
        }
    }

    Spacer(Modifier.height(18.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumStatCard("거리", "%.2f km".format(totalDistance / 1000f), "DIST", Modifier.weight(1f))
        PremiumStatCard("최고", "%.1f".format(maxSpeed), "MAX", Modifier.weight(1f))
    }

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PremiumStatCard("평균", "%.1f".format(averageSpeed), "AVG", Modifier.weight(1f))
        PremiumStatCard("시간", formatTime(elapsedSeconds), "TIME", Modifier.weight(1f))
    }

    Spacer(Modifier.height(28.dp))

    Button(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B5C))
    ) {
        Text("주행 종료", fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RankingSection(
    title: String,
    records: List<TripRecord>,
    valueText: (TripRecord) -> String
) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        records.forEachIndexed { index, record ->
            RankingRow(
                rank = index + 1,
                record = record,
                value = valueText(record)
            )

            if (index != records.lastIndex) {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun RankingRow(
    rank: Int,
    record: TripRecord,
    value: String
) {
    val medal = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                medal,
                fontSize = 28.sp,
                modifier = Modifier.width(44.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    record.date,
                    color = Color(0xFF9AA4B2),
                    fontSize = 14.sp
                )

                Text(
                    formatTime(record.durationSeconds),
                    color = Color(0xFF6F7A8A),
                    fontSize = 13.sp
                )
            }

            Text(
                value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun shareCurrentScreen(activity: Activity, view: View) {
    val bitmap = Bitmap.createBitmap(
        view.width,
        view.height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)
    view.draw(canvas)

    val file = File(activity.cacheDir, "trip_result.png")

    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = FileProvider.getUriForFile(
        activity,
        "${activity.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    activity.startActivity(Intent.createChooser(intent, "주행 결과 공유"))
}

@Composable
fun DarkOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PremiumStatCard(title: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(104.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661B1B1D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                color = Color(0xFF59E6D9),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                value,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(2.dp))

            Text(
                title,
                color = Color(0xFF8E9AAF),
                fontSize = 13.sp
            )
        }
    }
}