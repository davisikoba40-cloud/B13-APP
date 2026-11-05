package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GrayLight
import com.example.ui.theme.GrayMuted
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainTab
import com.example.viewmodel.MeshChatViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MeshChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.let {
            val targetId = it.getStringExtra("EXTRA_TARGET_ID")
            if (targetId != null) {
                if (targetId.startsWith("GROUP-")) {
                    val group = viewModel.groups.value.find { g -> g.id == targetId }
                    if (group != null) {
                        viewModel.openGroupChat(group)
                    }
                } else if (targetId != "BROADCAST") {
                    val peer = viewModel.allPeers.value.find { p -> p.id == targetId }
                    if (peer != null) {
                        viewModel.openDirectChat(peer)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MeshChatViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeChatPeer by viewModel.activeChatPeer.collectAsState()
    val activeChatGroup by viewModel.activeChatGroup.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()
    val isBtEnabled by viewModel.meshService.isBluetoothEnabled.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onContinue = {
                        viewModel.navigateTo(AppScreen.PERMISSIONS_GUARD)
                    }
                )
            }

            AppScreen.PERMISSIONS_GUARD -> {
                PermissionsScreen(
                    isBluetoothEnabled = isBtEnabled,
                    onAllGrantedOrContinue = {
                        viewModel.navigateTo(AppScreen.MAIN_TABS)
                    }
                )
            }

            AppScreen.MAIN_TABS -> {
                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    containerColor = PureBlack,
                    bottomBar = {
                        TacticalBottomNav(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            MainTab.CHANNELS -> {
                                ChatScreen(
                                    viewModel = viewModel,
                                    peer = null,
                                    group = null,
                                    onBack = null
                                )
                            }
                            MainTab.RADAR -> {
                                ScannerScreen(
                                    viewModel = viewModel,
                                    onStartDirectChat = { peer ->
                                        viewModel.openDirectChat(peer)
                                    }
                                )
                            }
                            MainTab.CONTACTS -> {
                                ContactsScreen(
                                    viewModel = viewModel,
                                    onStartDirectChat = { peer ->
                                        viewModel.openDirectChat(peer)
                                    }
                                )
                            }
                            MainTab.PROFILE -> {
                                ProfileScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }

            AppScreen.DIRECT_CHAT -> {
                BackHandler {
                    viewModel.closeDirectChat()
                }
                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    containerColor = PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ChatScreen(
                            viewModel = viewModel,
                            peer = activeChatPeer,
                            group = null,
                            onBack = { viewModel.closeDirectChat() }
                        )
                    }
                }
            }

            AppScreen.GROUP_CHAT -> {
                BackHandler {
                    viewModel.closeGroupChat()
                }
                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    containerColor = PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ChatScreen(
                            viewModel = viewModel,
                            peer = null,
                            group = activeChatGroup,
                            onBack = { viewModel.closeGroupChat() }
                        )
                    }
                }
            }
        }

        // Global Tactical Notice Banner
        AnimatedVisibility(
            visible = statusNotice != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            statusNotice?.let { notice ->
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PureBlack),
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = notice.uppercase(),
                        color = PureBlack,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TacticalBottomNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = PureBlack,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder)
    ) {
        val tabs = listOf(
            Triple(MainTab.CHANNELS, "CHANNELS", Icons.Default.ChatBubble),
            Triple(MainTab.RADAR, "RADAR", Icons.Default.Radar),
            Triple(MainTab.CONTACTS, "CONTACTS", Icons.Default.Group),
            Triple(MainTab.PROFILE, "PROFILE", Icons.Default.Person)
        )

        tabs.forEach { (tab, label, icon) ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        fontSize = 9.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PureBlack,
                    selectedTextColor = PureWhite,
                    indicatorColor = PureWhite,
                    unselectedIconColor = GrayLight,
                    unselectedTextColor = GrayMuted
                ),
                modifier = Modifier.testTag("tab_${label.lowercase()}")
            )
        }
    }
}
