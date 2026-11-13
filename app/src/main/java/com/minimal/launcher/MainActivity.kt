package com.minimal.launcher

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppItem(val label: String, val packageName: String, val icon: Bitmap)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen(loadApps()) }
    }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        var list = pm.queryIntentActivities(intent, 0)
        if (list.isEmpty()) {
            val alt = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
            list = pm.queryIntentActivities(alt, 0)
        }
        return list
            .filter { it.activityInfo.packageName != packageName }
            .map {
                AppItem(
                    it.loadLabel(pm).toString(),
                    it.activityInfo.packageName,
                    toBitmap(it.loadIcon(pm))
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun toBitmap(d: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, 256, 256)
        d.draw(c)
        return bmp
    }

    private fun launch(pkg: String) {
        packageManager.getLeanbackLaunchIntentForPackage(pkg)?.let {
            startActivity(it); return
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    @Composable
    fun LauncherScreen(apps: List<AppItem>) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF0B0B10), Color(0xFF1B1B26)))
            )
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp)) {
                TopBar()
                Spacer(Modifier.weight(1f))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp)
                ) {
                    items(apps) { app ->
                        AppTile(app.label, app.icon) { launch(app.packageName) }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun TopBar() {
        var now by remember { mutableStateOf(Date()) }
        LaunchedEffect(Unit) {
            while (true) { now = Date(); delay(1000) }
        }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        val date = SimpleDateFormat("d MMMM, EEEE", Locale.getDefault()).format(now)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column {
                BasicText(time, style = TextStyle(Color.White, 56.sp, FontWeight.Light))
                BasicText(date, style = TextStyle(Color(0xFF9A9AAE), 18.sp))
            }
            Spacer(Modifier.weight(1f))
            SettingsButton { openSettings() }
        }
    }

    @Composable
    fun SettingsButton(onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            if (focused) 1.12f else 1f,
            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "s"
        )
        Box(
            Modifier
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(if (focused) Color(0xFF3A3A4C) else Color(0xFF24242F))
                .onFocusChanged { focused = it.isFocused }
                .clickable { onClick() }
                .focusable()
                .padding(horizontal = 28.dp, vertical = 14.dp)
        ) {
            BasicText("Ayarlar", style = TextStyle(Color.White, 18.sp))
        }
    }

    @Composable
    fun AppTile(label: String, icon: Bitmap, onClick: () -> Unit) {
        var focused by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            if (focused) 1.18f else 1f,
            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow), label = "t"
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(150.dp)
        ) {
            Box(
                Modifier
                    .size(130.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF24242F))
                    .then(
                        if (focused) Modifier.border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
                        else Modifier
                    )
                    .onFocusChanged { focused = it.isFocused }
                    .clickable { onClick() }
                    .focusable()
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(icon.asImageBitmap(), label, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(18.dp))
            BasicText(
                label,
                style = TextStyle(
                    if (focused) Color.White else Color(0xFF8A8A9E),
                    16.sp
                ),
                maxLines = 1
            )
        }
    }
}
