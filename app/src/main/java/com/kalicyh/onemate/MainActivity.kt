package com.kalicyh.onemate

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.kalicyh.onemate.ui.component.FloatingBottomBar
import com.kalicyh.onemate.ui.component.FloatingBottomBarItem
import io.github.libxposed.service.XposedService
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.ThemeController as MiuixThemeController
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private const val THEME_PREFS = "settings"
private const val ROUTE_HOME = 0
private const val ROUTE_SETTINGS = 1
private const val ROUTE_THEME_SETTINGS = 2
private const val ROUTE_ABOUT = 3

class MainActivity : ComponentActivity(), App.ServiceStateListener {
    private var remotePrefs: SharedPreferences? = null
    private lateinit var themePrefs: SharedPreferences
    private var serviceState by mutableStateOf(ServiceUiState())
    private var themeSettings by mutableStateOf(ThemeSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePrefs = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        themeSettings = readThemeSettings(themePrefs)

        setContent {
            OneMateApp(
                serviceState = serviceState,
                themeSettings = themeSettings,
                onTextEditingEnabledChange = ::setTextEditingEnabled,
                onThemeChange = ::updateThemeSettings,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        App.addServiceStateListener(this)
    }

    override fun onStop() {
        App.removeServiceStateListener(this)
        super.onStop()
    }

    override fun onServiceStateChanged(service: XposedService?) {
        runOnUiThread { bindService(service) }
    }

    private fun bindService(service: XposedService?) {
        try {
            val remoteSupported = service != null &&
                    service.frameworkProperties and XposedService.PROP_CAP_REMOTE != 0L
            remotePrefs = if (remoteSupported) {
                service.getRemotePreferences(ToolbarConfig.PREF_GROUP)
            } else {
                null
            }
            serviceState = ServiceUiState(
                connected = service != null,
                remoteSupported = remoteSupported,
                inScope = service?.scope?.contains(ToolbarConfig.TARGET_PACKAGE) == true,
                framework = service?.let { "${it.frameworkName} API ${it.apiVersion}" }.orEmpty(),
                textEditingEnabled = ToolbarConfig.isTextEditingEnabled(remotePrefs),
            )
        } catch (e: RuntimeException) {
            remotePrefs = null
            serviceState = ServiceUiState(error = "LSPosed service: 连接异常\n${e.message.orEmpty()}")
        }
    }

    private fun setTextEditingEnabled(enabled: Boolean) {
        val prefs = remotePrefs
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = prefs.edit()
            .putBoolean(ToolbarConfig.KEY_FORCE_TEXT_EDITING, enabled)
            .commit()
        if (!saved) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        serviceState = serviceState.copy(textEditingEnabled = enabled)
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
    }

    private fun updateThemeSettings(settings: ThemeSettings) {
        themeSettings = settings.normalized()
        saveThemeSettings(themePrefs, themeSettings)
    }
}

@Composable
private fun OneMateApp(
    serviceState: ServiceUiState,
    themeSettings: ThemeSettings,
    onTextEditingEnabledChange: (Boolean) -> Unit,
    onThemeChange: (ThemeSettings) -> Unit,
) {
    val activity = LocalContext.current as? ComponentActivity
    val colorMode = ColorMode.fromValue(themeSettings.colorMode)
    val darkMode = colorMode.isDark || colorMode.isSystem && isSystemInDarkTheme()

    DisposableEffect(darkMode) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkMode },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkMode },
        )
        activity?.window?.isNavigationBarContrastEnforced = false
        onDispose { }
    }

    val systemDensity = LocalDensity.current
    val density = remember(systemDensity, themeSettings.pageScale) {
        Density(systemDensity.density * themeSettings.pageScale, systemDensity.fontScale)
    }

    CompositionLocalProvider(LocalDensity provides density) {
        OneMateTheme(themeSettings = themeSettings, darkMode = darkMode) {
            var route by remember { mutableIntStateOf(ROUTE_HOME) }
            val rootPage = if (route == ROUTE_HOME) 0 else 1
            val showBottomBar = route == ROUTE_HOME || route == ROUTE_SETTINGS
            val surfaceColor = MiuixTheme.colorScheme.surface
            val backdrop = rememberLayerBackdrop {
                drawRect(surfaceColor)
                drawContent()
            }

            Scaffold(
                topBar = {
                    when (route) {
                        ROUTE_THEME_SETTINGS -> SubPageTopBar("主题设置") { route = ROUTE_SETTINGS }
                        ROUTE_ABOUT -> SubPageTopBar("关于") { route = ROUTE_SETTINGS }
                        else -> Unit
                    }
                },
                popupHost = { },
                contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(innerPadding),
                ) {
                    Box(Modifier.layerBackdrop(backdrop)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight()
                                .scrollEndHaptic()
                                .overScrollVertical()
                                .padding(
                                    top = if (showBottomBar) {
                                        WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                                    } else {
                                        0.dp
                                    }
                                )
                                .padding(horizontal = 12.dp),
                            overscrollEffect = null,
                        ) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                when (route) {
                                    ROUTE_HOME -> HomePage(serviceState, onTextEditingEnabledChange)
                                    ROUTE_SETTINGS -> SettingsPage(
                                        onThemeSettingsClick = { route = ROUTE_THEME_SETTINGS },
                                        onAboutClick = { route = ROUTE_ABOUT },
                                    )
                                    ROUTE_THEME_SETTINGS -> ThemeSettingsPage(themeSettings, onThemeChange)
                                    ROUTE_ABOUT -> AboutPage()
                                    else -> HomePage(serviceState, onTextEditingEnabledChange)
                                }
                                Spacer(
                                    Modifier.height(
                                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                                if (showBottomBar) {
                                                    if (themeSettings.enableFloatingBottomBar) 96.dp else 80.dp
                                                } else {
                                                    12.dp
                                                }
                                    )
                                )
                            }
                        }
                    }
                    if (showBottomBar) {
                        OneMateBottomBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            page = rootPage,
                            floating = themeSettings.enableFloatingBottomBar,
                            glass = themeSettings.enableFloatingBottomBarBlur,
                            backdrop = backdrop,
                            onPageChange = { route = if (it == 0) ROUTE_HOME else ROUTE_SETTINGS },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubPageTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        color = MiuixTheme.colorScheme.surface,
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
    )
}

@Composable
private fun OneMateBottomBar(
    modifier: Modifier = Modifier,
    page: Int,
    floating: Boolean,
    glass: Boolean,
    backdrop: Backdrop,
    onPageChange: (Int) -> Unit,
) {
    if (!floating) {
        NavigationBar(modifier = modifier, color = MiuixTheme.colorScheme.surface) {
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Home,
                label = "主页",
                selected = page == 0,
                onClick = { onPageChange(0) },
            )
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Settings,
                label = "设置",
                selected = page == 1,
                onClick = { onPageChange(1) },
            )
        }
        return
    }

    val navPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    FloatingBottomBar(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(bottom = navPadding + 12.dp),
        selectedIndex = page,
        onSelected = onPageChange,
        backdrop = backdrop,
        tabsCount = 2,
        isBlurEnabled = glass,
    ) {
        FloatingBottomBarItem(
            onClick = { onPageChange(0) },
            modifier = Modifier.defaultMinSize(minWidth = 76.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Home,
                contentDescription = "主页",
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "主页",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        FloatingBottomBarItem(
            onClick = { onPageChange(1) },
            modifier = Modifier.defaultMinSize(minWidth = 76.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "设置",
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "设置",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomePage(
    serviceState: ServiceUiState,
    onTextEditingEnabledChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WorkStatusCard(serviceState)

        Card(Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = "启用三星键盘 Text editing",
                summary = "把旧版文本编辑按钮补回三星键盘工具栏，并显示仿旧版文本编辑面板。",
                startAction = {
                    Icon(
                        Icons.Rounded.TextFields,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                checked = serviceState.textEditingEnabled,
                enabled = serviceState.remoteSupported,
                onCheckedChange = onTextEditingEnabledChange,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前实现", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Hook 目标：${ToolbarConfig.TARGET_PACKAGE}", fontSize = 14.sp)
                Text("功能 ID：${ToolbarConfig.TEXT_EDITING_ID}", fontSize = 14.sp)
                Text("更改开关后重启三星键盘即可生效。", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun WorkStatusCard(serviceState: ServiceUiState) {
    val working = serviceState.ready && serviceState.textEditingEnabled
    val headline = when {
        working -> "Text editing 配置已就绪"
        !serviceState.textEditingEnabled -> "Text editing 未启用"
        !serviceState.connected -> "LSPosed 未连接"
        !serviceState.remoteSupported -> "Remote preferences 不可用"
        !serviceState.inScope -> "三星键盘未加入作用域"
        else -> "需要重启三星键盘"
    }
    val tint = if (working) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(38.dp),
                    imageVector = if (working) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = tint,
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text("工作状态", fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    Text(headline, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
            }

            StatusLine("功能开关", if (serviceState.textEditingEnabled) "已开启" else "已关闭", serviceState.textEditingEnabled)
            StatusLine("LSPosed", if (serviceState.connected) serviceState.framework else "未连接", serviceState.connected)
            StatusLine("Remote preferences", if (serviceState.remoteSupported) "可用" else "不可用", serviceState.remoteSupported)
            StatusLine("Samsung Keyboard scope", if (serviceState.inScope) "已包含" else "未包含", serviceState.inScope)

            if (serviceState.error.isNotEmpty()) {
                Text(serviceState.error, fontSize = 13.sp, lineHeight = 18.sp, color = MiuixTheme.colorScheme.outline)
            } else {
                Text("开启或关闭后重启三星键盘生效。", fontSize = 13.sp, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    ok: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (ok) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SettingsPage(
    onThemeSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "主题设置",
                summary = "颜色、模糊、底栏、页面缩放",
                startAction = {
                    Icon(
                        Icons.Rounded.Palette,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                onClick = onThemeSettingsClick,
            )
            ArrowPreference(
                title = "关于",
                summary = "模块信息与当前实现",
                startAction = {
                    Icon(
                        Icons.Rounded.Info,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun AboutPage() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("OneMate", fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
                Text("Samsung Keyboard Text editing LSPosed 模块", fontSize = 14.sp)
                StatusLine("目标包名", ToolbarConfig.TARGET_PACKAGE, true)
                StatusLine("功能 ID", ToolbarConfig.TEXT_EDITING_ID, true)
                StatusLine("libxposed API", "102", true)
            }
        }
    }
}

@Composable
private fun ThemeSettingsPage(
    settings: ThemeSettings,
    onThemeChange: (ThemeSettings) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ThemePreviewCard(settings)

        TabRow(
            tabs = listOf("跟随系统", "浅色", "深色"),
            selectedTabIndex = settings.selectedThemeIndex,
            onTabSelected = { index ->
                onThemeChange(settings.copy(colorMode = if (settings.miuixMonet) index + 3 else index))
            },
            height = 48.dp,
        )

        Card(Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = "Monet 动态颜色",
                startAction = {
                    Icon(
                        Icons.Rounded.Wallpaper,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                checked = settings.miuixMonet,
                onCheckedChange = { enabled ->
                    val mode = ColorMode.fromValue(settings.colorMode)
                    onThemeChange(
                        settings.copy(
                            miuixMonet = enabled,
                            colorMode = if (enabled) mode.toMonetMode() else mode.toNonMonetMode(),
                        )
                    )
                },
            )

            AnimatedVisibility(settings.miuixMonet) {
                Column {
                    val colorValues = listOf(0) + keyColorOptions
                    OverlayDropdownPreference(
                        title = "关键色",
                        items = listOf("默认") + keyColorNames,
                        startAction = {
                            Icon(
                                Icons.Rounded.Colorize,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onBackground,
                            )
                        },
                        selectedIndex = colorValues.indexOf(settings.keyColor).takeIf { it >= 0 } ?: 0,
                        onSelectedIndexChange = { index -> onThemeChange(settings.copy(keyColor = colorValues[index])) },
                    )

                    AnimatedVisibility(settings.keyColor != 0) {
                        Column {
                            val styles = PaletteStyle.entries
                            OverlayDropdownPreference(
                                title = "色彩风格",
                                items = styles.map { it.name },
                                startAction = {
                                    Icon(
                                        Icons.Rounded.Style,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = styles.indexOfFirst { it.name == settings.colorStyle }.coerceAtLeast(0),
                                onSelectedIndexChange = { index -> onThemeChange(settings.copy(colorStyle = styles[index].name)) },
                            )

                            val specs = ColorSpec.SpecVersion.entries
                            OverlayDropdownPreference(
                                title = "色彩规范",
                                items = specs.map { it.name },
                                startAction = {
                                    Icon(
                                        Icons.Rounded.DesignServices,
                                        modifier = Modifier.padding(end = 6.dp),
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onBackground,
                                    )
                                },
                                selectedIndex = specs.indexOfFirst { it.name == settings.colorSpec }.coerceAtLeast(0),
                                onSelectedIndexChange = { index -> onThemeChange(settings.copy(colorSpec = specs[index].name)) },
                            )
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SwitchPreference(
                    title = "启用模糊",
                    summary = "用于跟随 SukiSU 的主题配置；当前页面保留设置项。",
                    startAction = {
                        Icon(
                            Icons.Rounded.BlurOn,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    checked = settings.enableBlur,
                    onCheckedChange = { onThemeChange(settings.copy(enableBlur = it)) },
                )
            }
            SwitchPreference(
                title = "悬浮底栏",
                summary = "把底部导航切换为浮动样式。",
                startAction = {
                    Icon(
                        Icons.Rounded.CallToAction,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                checked = settings.enableFloatingBottomBar,
                onCheckedChange = { onThemeChange(settings.copy(enableFloatingBottomBar = it)) },
            )
            AnimatedVisibility(settings.enableFloatingBottomBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SwitchPreference(
                    title = "玻璃效果",
                    summary = "保留 SukiSU 的底栏玻璃开关。",
                    startAction = {
                        Icon(
                            Icons.Rounded.WaterDrop,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    checked = settings.enableFloatingBottomBarBlur,
                    onCheckedChange = { onThemeChange(settings.copy(enableFloatingBottomBarBlur = it)) },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SwitchPreference(
                    title = "预测返回",
                    summary = "启用 Android 的预测返回动画。",
                    startAction = {
                        Icon(
                            Icons.AutoMirrored.Rounded.MenuOpen,
                            modifier = Modifier.padding(end = 6.dp),
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    },
                    checked = settings.enablePredictiveBack,
                    onCheckedChange = { onThemeChange(settings.copy(enablePredictiveBack = it)) },
                )
            }

            var expanded by rememberSaveable { mutableStateOf(false) }
            var sliderValue by remember(settings.pageScale) { mutableFloatStateOf(settings.pageScale) }
            ArrowPreference(
                title = "页面缩放",
                summary = "调整整个 App 的显示密度。",
                startAction = {
                    Icon(
                        Icons.Rounded.AspectRatio,
                        modifier = Modifier.padding(end = 6.dp),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onBackground,
                    )
                },
                endActions = {
                    Text("${(sliderValue * 100).toInt()}%", color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                },
                onClick = { expanded = !expanded },
                holdDownState = expanded,
                bottomAction = {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = { onThemeChange(settings.copy(pageScale = sliderValue)) },
                        valueRange = 0.8f..1.1f,
                        showKeyPoints = true,
                        keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                        magnetThreshold = 0.01f,
                        hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                    )
                },
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(settings: ThemeSettings) {
    val colorMode = ColorMode.fromValue(settings.colorMode)
    val isDark = colorMode.isDark || colorMode.isSystem && isSystemInDarkTheme()
    val seedColor = if (settings.keyColor == 0) MiuixTheme.colorScheme.primary else Color(settings.keyColor)
    val dynamicScheme = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        style = settings.paletteStyle,
        specVersion = settings.colorSpecVersion,
    )
    val background = if (settings.miuixMonet) dynamicScheme.background else MiuixTheme.colorScheme.surface
    val card = if (settings.miuixMonet) dynamicScheme.surfaceContainerHighest else MiuixTheme.colorScheme.surfaceVariant
    val accent = if (settings.miuixMonet) dynamicScheme.primary else MiuixTheme.colorScheme.primary
    val text = if (settings.miuixMonet) dynamicScheme.onSurface else MiuixTheme.colorScheme.onBackground

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .aspectRatio(0.52f)
                .clip(RoundedCornerShape(20.dp))
                .background(background)
                .border(1.dp, MiuixTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("OneMate", color = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.55f)),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(card),
                            )
                        }
                    }
                }
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (it == 0) 50.dp else 18.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(card),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(if (settings.enableFloatingBottomBar) 18.dp else 0.dp))
                        .background(card),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(2) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (it == 0) accent else text.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OneMateTheme(
    themeSettings: ThemeSettings,
    darkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val paletteStyle = runCatching {
        ThemePaletteStyle.valueOf(themeSettings.paletteStyle.name)
    }.getOrDefault(ThemePaletteStyle.TonalSpot)
    val colorSpec = if (themeSettings.colorSpecVersion == ColorSpec.SpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }
    val controller = MiuixThemeController(
        when (ColorMode.fromValue(themeSettings.colorMode)) {
            ColorMode.SYSTEM -> ColorSchemeMode.System
            ColorMode.LIGHT -> ColorSchemeMode.Light
            ColorMode.DARK -> ColorSchemeMode.Dark
            ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
            ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
            ColorMode.MONET_DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.MonetDark
        },
        keyColor = themeSettings.keyColor.takeIf { it != 0 }?.let(::Color),
        isDark = darkMode,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )

    MiuixTheme(controller = controller) {
        CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onBackground) {
            content()
        }
    }
}

private data class ServiceUiState(
    val connected: Boolean = false,
    val remoteSupported: Boolean = false,
    val inScope: Boolean = false,
    val framework: String = "",
    val textEditingEnabled: Boolean = false,
    val error: String = "",
) {
    val ready: Boolean
        get() = connected && remoteSupported && inScope && error.isEmpty()

    val statusText: String
        get() {
            if (error.isNotEmpty()) return error
            if (!connected) return "请确认模块已安装，并由支持 API 102 的 LSPosed/Vector 加载。"
            return buildString {
                append("Framework: ").append(framework)
                append("\nRemote preferences: ").append(if (remoteSupported) "可用" else "不可用")
                append("\nSamsung Keyboard scope: ").append(if (inScope) "已包含" else "未包含")
            }
        }
}

private data class ThemeSettings(
    val colorMode: Int = ColorMode.SYSTEM.value,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.Default.name,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val pageScale: Float = 1.0f,
) {
    val selectedThemeIndex: Int
        get() = (if (colorMode >= 3) colorMode - 3 else colorMode).coerceIn(0, 2)

    val paletteStyle: PaletteStyle
        get() = runCatching { PaletteStyle.valueOf(colorStyle) }.getOrDefault(PaletteStyle.TonalSpot)

    val colorSpecVersion: ColorSpec.SpecVersion
        get() = runCatching { ColorSpec.SpecVersion.valueOf(colorSpec) }.getOrDefault(ColorSpec.SpecVersion.Default)

    fun normalized(): ThemeSettings {
        val mode = ColorMode.fromValue(colorMode)
        val nextMode = if (miuixMonet && !mode.isMonet) {
            mode.toMonetMode()
        } else if (!miuixMonet && mode.isMonet) {
            mode.toNonMonetMode()
        } else {
            mode.value
        }
        return copy(colorMode = nextMode, pageScale = pageScale.coerceIn(0.8f, 1.1f))
    }
}

private enum class ColorMode(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2),
    MONET_SYSTEM(3),
    MONET_LIGHT(4),
    MONET_DARK(5),
    DARK_AMOLED(6);

    val isSystem: Boolean get() = value == 0 || value == 3
    val isDark: Boolean get() = value == 2 || value == 5 || value == 6
    val isMonet: Boolean get() = value >= 3

    fun toNonMonetMode(): Int = when (this) {
        MONET_SYSTEM -> SYSTEM.value
        MONET_LIGHT -> LIGHT.value
        MONET_DARK, DARK_AMOLED -> DARK.value
        else -> value
    }

    fun toMonetMode(): Int = when (this) {
        SYSTEM -> MONET_SYSTEM.value
        LIGHT -> MONET_LIGHT.value
        DARK -> MONET_DARK.value
        else -> value
    }

    companion object {
        fun fromValue(value: Int): ColorMode = entries.find { it.value == value } ?: SYSTEM
    }
}

private val keyColorOptions = listOf(
    0xFFF44336.toInt(),
    0xFFE91E63.toInt(),
    0xFF9C27B0.toInt(),
    0xFF673AB7.toInt(),
    0xFF3F51B5.toInt(),
    0xFF2196F3.toInt(),
    0xFF00BCD4.toInt(),
    0xFF009688.toInt(),
    0xFF4CAF50.toInt(),
    0xFFFFEB3B.toInt(),
    0xFFFFC107.toInt(),
    0xFFFF9800.toInt(),
    0xFF795548.toInt(),
    0xFF607D8B.toInt(),
    0xFFFF9A9E.toInt(),
)

private val keyColorNames = listOf(
    "红色",
    "粉色",
    "紫色",
    "深紫",
    "靛蓝",
    "蓝色",
    "青色",
    "蓝绿",
    "绿色",
    "黄色",
    "琥珀",
    "橙色",
    "棕色",
    "蓝灰",
    "樱花",
)

private fun readThemeSettings(prefs: SharedPreferences): ThemeSettings {
    return ThemeSettings(
        colorMode = prefs.getInt("color_mode", ColorMode.SYSTEM.value),
        miuixMonet = prefs.getBoolean("miuix_monet", false),
        keyColor = prefs.getInt("key_color", 0),
        colorStyle = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name,
        colorSpec = prefs.getString("color_spec", ColorSpec.SpecVersion.Default.name) ?: ColorSpec.SpecVersion.Default.name,
        enablePredictiveBack = prefs.getBoolean("enable_predictive_back", false),
        enableBlur = prefs.getBoolean("enable_blur", false),
        enableFloatingBottomBar = prefs.getBoolean("enable_floating_bottom_bar", false),
        enableFloatingBottomBarBlur = prefs.getBoolean("enable_floating_bottom_bar_blur", false),
        pageScale = prefs.getFloat("page_scale", 1.0f),
    ).normalized()
}

private fun saveThemeSettings(prefs: SharedPreferences, settings: ThemeSettings) {
    prefs.edit()
        .putInt("color_mode", settings.colorMode)
        .putBoolean("miuix_monet", settings.miuixMonet)
        .putInt("key_color", settings.keyColor)
        .putString("color_style", settings.colorStyle)
        .putString("color_spec", settings.colorSpec)
        .putBoolean("enable_predictive_back", settings.enablePredictiveBack)
        .putBoolean("enable_blur", settings.enableBlur)
        .putBoolean("enable_floating_bottom_bar", settings.enableFloatingBottomBar)
        .putBoolean("enable_floating_bottom_bar_blur", settings.enableFloatingBottomBarBlur)
        .putFloat("page_scale", settings.pageScale)
        .apply()
}
