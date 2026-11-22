package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.rememberRgbVisualState
import com.example.ui.screens.ConnectionScreen
import com.example.ui.screens.ControllerScreen
import com.example.ui.screens.LayoutsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class SounceNavTab(val label: String, val icon: ImageVector) {
    CONTROLLER("Controller", Icons.Default.Gamepad),
    LAYOUTS("Layouts", Icons.Default.Keyboard),
    CONNECT("Connect", Icons.Default.BluetoothConnected),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun SounceApp(
    viewModel: ControllerViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snapshot by viewModel.connectionSnapshot.collectAsStateWithLifecycle()
    val allLayouts by viewModel.allLayouts.collectAsStateWithLifecycle()
    val activeLayout by viewModel.activeLayout.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(SounceNavTab.CONTROLLER) }
    val rgbState = rememberRgbVisualState(settings.rgbEffect, Color(settings.rgbStaticColorArgb))

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                SounceTopHudBar(
                    headline = snapshot.statusHeadline,
                    subtext = "${settings.selectedMode.displayName} • ${snapshot.activeEngine.label}",
                    isReady = snapshot.isReadyToTransmit,
                    accent = rgbState.primaryAccent,
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        SounceNavTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        SounceNavTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        SounceNavTab.CONTROLLER -> ControllerScreen(
                            viewModel = viewModel,
                            settings = settings,
                            snapshot = snapshot,
                            activeLayout = activeLayout,
                            onNavigateToLayouts = { currentTab = SounceNavTab.LAYOUTS }
                        )
                        SounceNavTab.LAYOUTS -> LayoutsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            allLayouts = allLayouts,
                            activeLayout = activeLayout
                        )
                        SounceNavTab.CONNECT -> ConnectionScreen(
                            viewModel = viewModel,
                            settings = settings,
                            snapshot = snapshot
                        )
                        SounceNavTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onNavigateToLayouts = { currentTab = SounceNavTab.LAYOUTS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SounceTopHudBar(
    headline: String,
    subtext: String,
    isReady: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianElevated)
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isReady) StatusConnected else StatusWarning)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "SOUNCE GAMING CONTROLLER",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1
                )
                Text(
                    text = "$headline • $subtext",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Active Gaming Profile",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
