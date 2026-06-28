package com.kalicyh.onemate

import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.health.connect.HealthConnectManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CallToAction
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import org.json.JSONObject
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
private const val RUNTIME_PREFS = "runtime"
private const val CONFIG_ARCHIVE_PREFS = "config_archive"
private const val CONFIG_ARCHIVE_SAVED = "saved"
private const val HEALTH_PERMISSION_REQUEST = 42
private const val ROUTE_HOME = 0
private const val ROUTE_HIDDEN_SETTINGS = 1
private const val ROUTE_AQ_RECORDS = 2
private const val ROUTE_SETTINGS = 3
private const val ROUTE_THEME_SETTINGS = 4
private const val ROUTE_ABOUT = 5

class MainActivity : ComponentActivity(), App.ServiceStateListener {
    private var remotePrefs: SharedPreferences? = null
    private lateinit var themePrefs: SharedPreferences
    private lateinit var runtimePrefs: SharedPreferences
    private lateinit var configArchivePrefs: SharedPreferences
    private lateinit var healthPrefs: SharedPreferences
    private var serviceState by mutableStateOf(ServiceUiState())
    private var themeSettings by mutableStateOf(ThemeSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        themePrefs = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        runtimePrefs = getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)
        configArchivePrefs = getSharedPreferences(CONFIG_ARCHIVE_PREFS, Context.MODE_PRIVATE)
        healthPrefs = getSharedPreferences(HealthConnectSync.PREFS, Context.MODE_PRIVATE)
        themeSettings = readThemeSettings(themePrefs)

        setContent {
            OneMateApp(
                serviceState = serviceState,
                themeSettings = themeSettings,
                onTextEditingEnabledChange = ::setTextEditingEnabled,
                onToolbarBadgesDisabledChange = ::setToolbarBadgesDisabled,
                onHiddenSettingEnabledChange = ::setHiddenSettingEnabled,
                onRefreshState = ::refreshServiceState,
                onRestartKeyboard = ::restartSamsungKeyboard,
                onHealthSyncEnabledChange = ::setHealthSyncEnabled,
                onRequestHealthPermissions = ::requestHealthPermissions,
                onSaveConfigArchive = ::saveConfigArchive,
                onRestoreConfigArchive = ::restoreConfigArchive,
                onThemeChange = ::updateThemeSettings,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        App.addServiceStateListener(this)
    }

    override fun onResume() {
        super.onResume()
        refreshServiceState()
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
            ensureAqBodyToken(remotePrefs)
            serviceState = ServiceUiState(
                connected = service != null,
                remoteSupported = remoteSupported,
                inScope = service?.scope?.contains(ToolbarConfig.TARGET_PACKAGE) == true,
                framework = service?.let { "${it.frameworkName} API ${it.apiVersion}" }.orEmpty(),
                textEditingEnabled = ToolbarConfig.isTextEditingEnabled(remotePrefs),
                toolbarBadgesDisabled = ToolbarConfig.areToolbarBadgesDisabled(remotePrefs),
                enabledHiddenSettings = readEffectiveEnabledHiddenSettings(remotePrefs, runtimePrefs),
                runtimeEnabledHiddenSettings = readRuntimeEnabledHiddenSettings(runtimePrefs),
                healthSyncEnabled = healthPrefs.getBoolean(HealthConnectSync.KEY_ENABLED, false),
                healthConnectGranted = HealthConnectSync.hasPermissions(this),
                lastHealthStatus = healthPrefs.getString(HealthConnectSync.KEY_LAST_STATUS, "").orEmpty(),
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

    private fun setToolbarBadgesDisabled(disabled: Boolean) {
        val prefs = remotePrefs
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = prefs.edit()
            .putBoolean(ToolbarConfig.KEY_DISABLE_TOOLBAR_BADGES, disabled)
            .commit()
        if (!saved) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        serviceState = serviceState.copy(toolbarBadgesDisabled = disabled)
        Toast.makeText(this, "已保存，重启三星键盘后生效", Toast.LENGTH_SHORT).show()
    }

    private fun setHiddenSettingEnabled(key: String, enabled: Boolean) {
        val prefs = remotePrefs
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show()
            return
        }
        val saved = prefs.edit()
            .putBoolean(ToolbarConfig.hiddenSettingPrefKey(key), enabled)
            .commit()
        if (!saved) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        serviceState = serviceState.copy(
            enabledHiddenSettings = readEffectiveEnabledHiddenSettings(prefs, runtimePrefs),
        )
        Toast.makeText(this, "已保存，重启三星键盘后生效", Toast.LENGTH_SHORT).show()
    }

    private fun refreshServiceState() {
        val prefs = remotePrefs
        serviceState = serviceState.copy(
            toolbarBadgesDisabled = ToolbarConfig.areToolbarBadgesDisabled(prefs),
            enabledHiddenSettings = readEffectiveEnabledHiddenSettings(prefs, runtimePrefs),
            runtimeEnabledHiddenSettings = readRuntimeEnabledHiddenSettings(runtimePrefs),
            healthSyncEnabled = healthPrefs.getBoolean(HealthConnectSync.KEY_ENABLED, false),
            healthConnectGranted = HealthConnectSync.hasPermissions(this),
            lastHealthStatus = healthPrefs.getString(HealthConnectSync.KEY_LAST_STATUS, "").orEmpty(),
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == HEALTH_PERMISSION_REQUEST) {
            refreshServiceState()
        }
    }

    private fun setHealthSyncEnabled(enabled: Boolean) {
        ensureAqBodyToken(remotePrefs)
        healthPrefs.edit().putBoolean(HealthConnectSync.KEY_ENABLED, enabled).apply()
        serviceState = serviceState.copy(healthSyncEnabled = enabled)
        if (enabled && !HealthConnectSync.hasPermissions(this)) {
            requestHealthPermissions()
        } else {
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureAqBodyToken(remotePrefs: SharedPreferences?) {
        if (remotePrefs == null) return
        val existing = healthPrefs.getString(ToolbarConfig.KEY_AQ_BODY_TOKEN, "").orEmpty()
        val token = existing.ifBlank { UUID.randomUUID().toString() }
        healthPrefs.edit().putString(ToolbarConfig.KEY_AQ_BODY_TOKEN, token).apply()
        if (remotePrefs.getString(ToolbarConfig.KEY_AQ_BODY_TOKEN, "") != token) {
            remotePrefs.edit().putString(ToolbarConfig.KEY_AQ_BODY_TOKEN, token).commit()
        }
    }

    private fun requestHealthPermissions() {
        runCatching {
            requestPermissions(HealthConnectSync.permissions, HEALTH_PERMISSION_REQUEST)
        }.onFailure {
            runCatching {
                startActivity(
                    Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS)
                        .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                )
            }.onFailure { error ->
                Toast.makeText(
                    this,
                    "无法打开 Health Connect 授权：${error.message.orEmpty()}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun restartSamsungKeyboard() {
        Thread {
            val command = "am force-stop ${ToolbarConfig.TARGET_PACKAGE}"
            val ok = listOf(
                arrayOf("su", "-c", command),
                arrayOf("/system/bin/su", "-c", command),
                arrayOf("/system/xbin/su", "-c", command),
                arrayOf("/vendor/bin/su", "-c", command),
            ).any(::runRootCommand)
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) "已强制停止三星键盘，点击测试输入框会重新拉起" else "重启失败：SukiSU 未给 OneMate 授权或 su 不可用",
                    Toast.LENGTH_SHORT,
                )
                    .show()
                refreshServiceState()
            }
        }.start()
    }

    private fun runRootCommand(command: Array<String>): Boolean = runCatching {
        val process = Runtime.getRuntime().exec(command)
        process.waitFor() == 0
    }.getOrDefault(false)

    private fun saveConfigArchive() {
        val prefs = remotePrefs
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show()
            return
        }
        val editor = configArchivePrefs.edit()
        configArchiveKeys.forEach { key -> editor.putBoolean(key, prefs.getBoolean(key, false)) }
        editor.putBoolean(CONFIG_ARCHIVE_SAVED, true)
        if (!editor.commit()) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val ok = runRootShell(samsungKeyboardBackupCommand())
            runOnUiThread {
                Toast.makeText(
                    this,
                    if (ok) "已保存当前配置" else "模块配置已保存，三星键盘配置备份失败：请确认 root 授权并先打开过三星键盘",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.start()
    }

    private fun restoreConfigArchive() {
        val prefs = remotePrefs
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show()
            return
        }
        if (!configArchivePrefs.getBoolean(CONFIG_ARCHIVE_SAVED, false)) {
            Toast.makeText(this, "还没有保存过配置", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val keyboardOk = runRootShell(samsungKeyboardRestoreCommand())
            val editor = prefs.edit()
            configArchiveKeys.forEach { key -> editor.putBoolean(key, configArchivePrefs.getBoolean(key, false)) }
            val moduleOk = editor.commit()
            runOnUiThread {
                refreshServiceState()
                Toast.makeText(
                    this,
                    when {
                        keyboardOk && moduleOk -> "已恢复当前配置，重启三星键盘后生效"
                        moduleOk -> "模块配置已恢复，三星键盘配置恢复失败：请确认 root 授权并已保存过键盘配置"
                        else -> "恢复失败"
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }.start()
    }

    private fun runRootShell(command: String): Boolean = listOf(
        arrayOf("su", "-c", command),
        arrayOf("/system/bin/su", "-c", command),
        arrayOf("/system/xbin/su", "-c", command),
        arrayOf("/vendor/bin/su", "-c", command),
    ).any(::runRootCommand)

    private fun samsungKeyboardArchivePath(name: String): String =
        File(File(filesDir, "samsung_keyboard_config"), "$name.tar").absolutePath

    private fun samsungKeyboardBackupCommand(): String {
        val archiveDir = File(filesDir, "samsung_keyboard_config").absolutePath
        return """
            set -e
            rm -rf "$archiveDir"
            mkdir -p "$archiveDir"
            for pair in ce:/data/user/0/${ToolbarConfig.TARGET_PACKAGE} de:/data/user_de/0/${ToolbarConfig.TARGET_PACKAGE}; do
              name="${'$'}{pair%%:*}"
              src="${'$'}{pair#*:}"
              [ -d "${'$'}src" ] || continue
              items=""
              for d in shared_prefs databases no_backup files; do
                [ -e "${'$'}src/${'$'}d" ] && items="${'$'}items ${'$'}d"
              done
              [ -n "${'$'}items" ] && tar -C "${'$'}src" -cf "$archiveDir/${'$'}name.tar" ${'$'}items
            done
            [ -f "$archiveDir/ce.tar" ] || [ -f "$archiveDir/de.tar" ]
        """.trimIndent()
    }

    private fun samsungKeyboardRestoreCommand(): String {
        val ceArchive = samsungKeyboardArchivePath("ce")
        val deArchive = samsungKeyboardArchivePath("de")
        return """
            set -e
            [ -f "$ceArchive" ] || [ -f "$deArchive" ]
            am force-stop ${ToolbarConfig.TARGET_PACKAGE} || true
            for pair in ce:$ceArchive:/data/user/0/${ToolbarConfig.TARGET_PACKAGE} de:$deArchive:/data/user_de/0/${ToolbarConfig.TARGET_PACKAGE}; do
              name="${'$'}{pair%%:*}"
              rest="${'$'}{pair#*:}"
              archive="${'$'}{rest%%:*}"
              dst="${'$'}{rest#*:}"
              [ -f "${'$'}archive" ] || continue
              [ -d "${'$'}dst" ] || continue
              tar -C "${'$'}dst" -xf "${'$'}archive"
              uid=${'$'}(stat -c %u "${'$'}dst" 2>/dev/null || ls -ldn "${'$'}dst" | awk '{print ${'$'}3}')
              gid=${'$'}(stat -c %g "${'$'}dst" 2>/dev/null || ls -ldn "${'$'}dst" | awk '{print ${'$'}4}')
              chown -R "${'$'}uid:${'$'}gid" "${'$'}dst"/shared_prefs "${'$'}dst"/databases "${'$'}dst"/no_backup "${'$'}dst"/files 2>/dev/null || true
              restorecon -R "${'$'}dst"/shared_prefs "${'$'}dst"/databases "${'$'}dst"/no_backup "${'$'}dst"/files 2>/dev/null || true
            done
            am force-stop ${ToolbarConfig.TARGET_PACKAGE} || true
        """.trimIndent()
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
    onToolbarBadgesDisabledChange: (Boolean) -> Unit,
    onHiddenSettingEnabledChange: (String, Boolean) -> Unit,
    onRefreshState: () -> Unit,
    onRestartKeyboard: () -> Unit,
    onHealthSyncEnabledChange: (Boolean) -> Unit,
    onRequestHealthPermissions: () -> Unit,
    onSaveConfigArchive: () -> Unit,
    onRestoreConfigArchive: () -> Unit,
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
            val rootPage = when (route) {
                ROUTE_HOME -> 0
                ROUTE_HIDDEN_SETTINGS -> 1
                ROUTE_AQ_RECORDS -> 2
                else -> 3
            }
            val showBottomBar = route == ROUTE_HOME ||
                    route == ROUTE_HIDDEN_SETTINGS ||
                    route == ROUTE_AQ_RECORDS ||
                    route == ROUTE_SETTINGS
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
                                    ROUTE_HOME -> HomePage(
                                        serviceState,
                                        onTextEditingEnabledChange,
                                        onToolbarBadgesDisabledChange,
                                        onRestartKeyboard,
                                        onHealthSyncEnabledChange,
                                        onRequestHealthPermissions,
                                    )
                                    ROUTE_HIDDEN_SETTINGS -> HiddenSettingsPage(
                                        serviceState = serviceState,
                                        onHiddenSettingEnabledChange = onHiddenSettingEnabledChange,
                                        onRefreshState = onRefreshState,
                                    )
                                    ROUTE_AQ_RECORDS -> AqRecordsPage()
                                    ROUTE_SETTINGS -> SettingsPage(
                                        onSaveConfigArchive = onSaveConfigArchive,
                                        onRestoreConfigArchive = onRestoreConfigArchive,
                                        onThemeSettingsClick = { route = ROUTE_THEME_SETTINGS },
                                        onAboutClick = { route = ROUTE_ABOUT },
                                    )
                                    ROUTE_THEME_SETTINGS -> ThemeSettingsPage(themeSettings, onThemeChange)
                                    ROUTE_ABOUT -> AboutPage()
                                    else -> HomePage(
                                        serviceState,
                                        onTextEditingEnabledChange,
                                        onToolbarBadgesDisabledChange,
                                        onRestartKeyboard,
                                        onHealthSyncEnabledChange,
                                        onRequestHealthPermissions,
                                    )
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
                            onPageChange = {
                                route = when (it) {
                                    0 -> ROUTE_HOME
                                    1 -> ROUTE_HIDDEN_SETTINGS
                                    2 -> ROUTE_AQ_RECORDS
                                    else -> ROUTE_SETTINGS
                                }
                            },
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
                icon = Icons.Rounded.DesignServices,
                label = "隐藏项",
                selected = page == 1,
                onClick = { onPageChange(1) },
            )
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Info,
                label = "AQ记录",
                selected = page == 2,
                onClick = { onPageChange(2) },
            )
            NavigationBarItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Settings,
                label = "设置",
                selected = page == 3,
                onClick = { onPageChange(3) },
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
        tabsCount = 4,
        isBlurEnabled = glass,
    ) {
        FloatingBottomBarItem(
            onClick = { onPageChange(0) },
            modifier = Modifier.defaultMinSize(minWidth = 68.dp),
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
            modifier = Modifier.defaultMinSize(minWidth = 68.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.DesignServices,
                contentDescription = "隐藏项",
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "隐藏项",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        FloatingBottomBarItem(
            onClick = { onPageChange(2) },
            modifier = Modifier.defaultMinSize(minWidth = 68.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = "AQ记录",
                tint = MiuixTheme.colorScheme.onSurface,
            )
            Text(
                text = "AQ记录",
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        FloatingBottomBarItem(
            onClick = { onPageChange(3) },
            modifier = Modifier.defaultMinSize(minWidth = 68.dp),
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
    onToolbarBadgesDisabledChange: (Boolean) -> Unit,
    onRestartKeyboard: () -> Unit,
    onHealthSyncEnabledChange: (Boolean) -> Unit,
    onRequestHealthPermissions: () -> Unit,
) {
    var testText by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WorkStatusCard(serviceState)

        Card(Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = "启用三星键盘 Text editing",
                summary = "把旧版文本编辑按钮补回工具栏，并显示三星键盘隐藏实验设置。",
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
            SwitchPreference(
                title = "禁用工具栏红点",
                summary = "拦截工具栏 BeeItem 的新功能徽标，包含表情里的 AI 生成图片红点。",
                checked = serviceState.toolbarBadgesDisabled,
                enabled = serviceState.remoteSupported,
                onCheckedChange = onToolbarBadgesDisabledChange,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "强制重启三星键盘",
                summary = "使用 root 执行 am force-stop；随后点击下面输入框重新拉起键盘。",
                onClick = onRestartKeyboard,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = "同步 AQ 身材数据到 Health Connect",
                summary = "打开 AQ 身材管理页时抓取体脂秤 RPC 结果，并写入体重、体脂、体水分、骨量、瘦体重、身高和基础代谢。",
                checked = serviceState.healthSyncEnabled,
                onCheckedChange = onHealthSyncEnabledChange,
            )
            ArrowPreference(
                title = "Health Connect 授权",
                summary = "状态：${if (serviceState.healthConnectGranted) "已授权" else "未授权"}\n${serviceState.lastHealthStatus.ifBlank { "尚未捕获 AQ 身材数据" }}",
                onClick = onRequestHealthPermissions,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("测试输入", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                BasicTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    textStyle = TextStyle(
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MiuixTheme.colorScheme.outline.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox = { innerTextField ->
                        Box(Modifier.fillMaxWidth()) {
                            if (testText.isEmpty()) {
                                Text(
                                    "点击这里测试键盘",
                                    fontSize = 18.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("当前实现", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("Hook 目标：${ToolbarConfig.TARGET_PACKAGE}", fontSize = 14.sp)
                Text("功能 ID：${ToolbarConfig.TEXT_EDITING_ID}", fontSize = 14.sp)
                Text("更改开关后重启三星键盘，重新打开键盘设置即可生效。", fontSize = 14.sp)
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
    val cardColor = if (working) Color(0xFF3F7E54) else MiuixTheme.colorScheme.surfaceVariant
    val contentColor = if (working) Color.White else MiuixTheme.colorScheme.onSurface
    val subColor = if (working) Color.White.copy(alpha = 0.78f) else MiuixTheme.colorScheme.onSurfaceVariantSummary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(158.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(cardColor)
            .padding(20.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(128.dp)
                .align(Alignment.BottomEnd),
            imageVector = if (working) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (working) Color(0xFF69F06D).copy(alpha = 0.8f) else MiuixTheme.colorScheme.outline.copy(alpha = 0.35f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("工作状态", fontSize = 14.sp, color = subColor)
            Text(headline, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = contentColor)
            Text(
                if (serviceState.connected) serviceState.framework else "LSPosed 未连接",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
            Text(
                "Remote: ${if (serviceState.remoteSupported) "可用" else "不可用"} / Scope: ${if (serviceState.inScope) "已包含" else "未包含"}",
                fontSize = 13.sp,
                color = subColor,
            )
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
private fun AqRecordsPage() {
    val context = LocalContext.current
    var reload by remember { mutableIntStateOf(0) }
    val records = remember(reload) { readAqRecords(context) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "刷新记录",
                summary = if (records.isEmpty()) {
                    "还没有捕获到 AQ 身材管理数据；打开 AQ 身材管理页后再回来刷新。"
                } else {
                    "共 ${records.size} 条，按测量时间倒序排列。"
                },
                onClick = { reload++ },
            )
        }
        if (records.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "暂无记录",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            records.forEach { record ->
                AqRecordItem(record = record)
            }
        }
    }
}

@Composable
private fun AqRecordItem(record: AqDisplayRecord) {
    val context = LocalContext.current
    var expanded by rememberSaveable(record.recordTime) { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(record.timeText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    record.summary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = if (expanded) {
                    Icons.Rounded.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight
                },
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "详细 JSON",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(onClick = { copyText(context, "AQ 身材记录", record.json) }) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "复制",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                }
                Text(
                    text = record.json,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

private fun readAqRecords(context: Context): List<AqDisplayRecord> = runCatching {
    val file = File(context.filesDir, HealthConnectSync.LATEST_FILE)
    if (!file.isFile) return emptyList()
    val day = JSONObject(file.readText()).getJSONObject("data").getJSONObject("day")
    val records = day.getJSONArray("records")
    buildList {
        for (i in 0 until records.length()) {
            val record = records.getJSONObject(i)
            val recordTime = record.optLong("recordTime", 0L)
            add(
                AqDisplayRecord(
                    recordTime = recordTime,
                    timeText = formatAqRecordTime(recordTime),
                    summary = aqRecordSummary(record),
                    json = record.toString(2),
                )
            )
        }
    }.sortedByDescending { it.recordTime }
}.getOrDefault(emptyList())

private fun aqRecordSummary(record: JSONObject): String {
    val parts = listOfNotNull(
        record.optString("weight").takeIf { it.isNotBlank() }?.let { "体重 ${it}kg" },
        record.optMetric("bmi")?.let { "BMI $it" },
        record.optMetric("fatPercent")?.let { "体脂 $it%" },
        record.optMetric("muscleMass")?.let { "肌肉 ${it}kg" },
    )
    return parts.joinToString(" / ").ifBlank { record.optString("source", "AQ 身材记录") }
}

private fun JSONObject.optMetric(key: String): String? =
    optJSONObject(key)?.optString("value")?.takeIf { it.isNotBlank() }

private fun formatAqRecordTime(recordTime: Long): String {
    if (recordTime <= 0L) return "未知时间"
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(recordTime))
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

private data class AqDisplayRecord(
    val recordTime: Long,
    val timeText: String,
    val summary: String,
    val json: String,
)

@Composable
private fun HiddenSettingsPage(
    serviceState: ServiceUiState,
    onHiddenSettingEnabledChange: (String, Boolean) -> Unit,
    onRefreshState: () -> Unit,
) {
    val forceableOptions = hiddenSettingOptions
        .filterNot { serviceState.runtimeEnabledHiddenSettings.contains(it.key) }
    val runtimeEnabledOptions = hiddenSettingOptions
        .filter { serviceState.runtimeEnabledHiddenSettings.contains(it.key) }
    val forceEnabledCount = forceableOptions.count { serviceState.enabledHiddenSettings.contains(it.key) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HiddenSettingSection(
            title = "可强开",
            summary = "共 ${forceableOptions.size} 项，${forceEnabledCount} 项已强开",
            options = forceableOptions,
            initiallyExpanded = true,
            serviceState = serviceState,
            onHiddenSettingEnabledChange = onHiddenSettingEnabledChange,
        )

        HiddenSettingSection(
            title = "无需 hook",
            summary = "共 ${runtimeEnabledOptions.size} 项，当前运行时已正常开启",
            options = runtimeEnabledOptions,
            initiallyExpanded = false,
            serviceState = serviceState,
            onHiddenSettingEnabledChange = onHiddenSettingEnabledChange,
        )

        Card(Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "刷新运行时状态",
                summary = "先打开一次三星键盘设置页，模块会记录每个 key 在当前环境下是否已正常开放。",
                onClick = onRefreshState,
            )
        }
    }
}

@Composable
private fun HiddenSettingSection(
    title: String,
    summary: String,
    options: List<HiddenSettingOption>,
    initiallyExpanded: Boolean,
    serviceState: ServiceUiState,
    onHiddenSettingEnabledChange: (String, Boolean) -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    summary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Icon(
                imageVector = if (expanded) {
                    Icons.Rounded.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight
                },
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                if (options.isEmpty()) {
                    Text(
                        "暂无项目",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                } else {
                    options.forEach { option ->
                        HiddenSettingSwitch(
                            option = option,
                            serviceState = serviceState,
                            onHiddenSettingEnabledChange = onHiddenSettingEnabledChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenSettingSwitch(
    option: HiddenSettingOption,
    serviceState: ServiceUiState,
    onHiddenSettingEnabledChange: (String, Boolean) -> Unit,
) {
    val runtimeEnabled = serviceState.runtimeEnabledHiddenSettings.contains(option.key)
    SwitchPreference(
        title = option.title,
        summary = "${option.key}\n条件：${option.condition}\n状态：${if (runtimeEnabled) "当前运行时已正常开启，无需 hook" else "当前运行时未开放，可用模块强开"}",
        checked = runtimeEnabled || serviceState.enabledHiddenSettings.contains(option.key),
        enabled = serviceState.remoteSupported && !runtimeEnabled,
        onCheckedChange = { onHiddenSettingEnabledChange(option.key, it) },
    )
}

@Composable
private fun SettingsPage(
    onSaveConfigArchive: () -> Unit,
    onRestoreConfigArchive: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            ArrowPreference(
                title = "保存当前配置",
                summary = "保存三星键盘设置、模糊音等内部配置，以及 OneMate 模块开关。",
                onClick = onSaveConfigArchive,
            )
            ArrowPreference(
                title = "恢复已保存配置",
                summary = "清空三星键盘数据后，可一键写回键盘配置和模块开关。",
                onClick = onRestoreConfigArchive,
            )
        }
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
    val toolbarBadgesDisabled: Boolean = false,
    val enabledHiddenSettings: Set<String> = emptySet(),
    val runtimeEnabledHiddenSettings: Set<String> = emptySet(),
    val healthSyncEnabled: Boolean = false,
    val healthConnectGranted: Boolean = false,
    val lastHealthStatus: String = "",
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

private data class HiddenSettingOption(
    val key: String,
    val title: String,
    val condition: String,
)

private fun readEnabledHiddenSettings(prefs: SharedPreferences?): Set<String> {
    if (prefs == null) return emptySet()
    return hiddenSettingOptions
        .map { it.key }
        .filter { prefs.getBoolean(ToolbarConfig.hiddenSettingPrefKey(it), false) }
        .toSet()
}

private fun readEffectiveEnabledHiddenSettings(
    remotePrefs: SharedPreferences?,
    runtimePrefs: SharedPreferences,
): Set<String> = readEnabledHiddenSettings(remotePrefs) - readRuntimeEnabledHiddenSettings(runtimePrefs)

private fun readRuntimeEnabledHiddenSettings(prefs: SharedPreferences?): Set<String> {
    if (prefs == null) return emptySet()
    return hiddenSettingOptions
        .map { it.key }
        .filter { prefs.getBoolean(ToolbarConfig.runtimeEnabledPrefKey(it), false) }
        .toSet()
}

private val configArchiveKeys: List<String>
    get() = buildList {
        add(ToolbarConfig.KEY_FORCE_TEXT_EDITING)
        add(ToolbarConfig.KEY_DISABLE_TOOLBAR_BADGES)
        hiddenSettingOptions.forEach { add(ToolbarConfig.hiddenSettingPrefKey(it.key)) }
    }

private val hiddenSettingOptions = listOf(
    HiddenSettingOption(
        "SETTINGS_SHOW_SMS_OTP",
        "显示要输入的短信验证码",
        "依赖短信 OTP/系统智能提取服务，可能受地区和隐私策略限制。",
    ),
    HiddenSettingOption(
        "SETTINGS_SPECIFIC_ASSIST",
        "聊天室推荐权重",
        "韩文内部调试项，依赖聊天推荐/候选词服务。",
    ),
    HiddenSettingOption(
        "SETTINGS_TOUCH_EVENT_RECORD",
        "Touch event test",
        "开发触控采样测试项，需要同步显示 Touch event category。",
    ),
    HiddenSettingOption(
        "SETTINGS_WRITING_ASSIST",
        "Writing Assist",
        "依赖 Galaxy AI、三星账号、地区策略和模型开关。",
    ),
    HiddenSettingOption(
        "SETTINGS_DRAWING_ASSIST",
        "Drawing Assist",
        "依赖 Galaxy AI、设备能力和地区策略。",
    ),
    HiddenSettingOption(
        "SETTINGS_WRITING_ASSIST_TRANSLATION",
        "Writing Assist Translation",
        "依赖写作助手和翻译服务可用性。",
    ),
    HiddenSettingOption(
        "SETTINGS_VOICE_INPUT",
        "语音输入",
        "依赖语音输入服务、地区策略和默认输入法状态。",
    ),
    HiddenSettingOption(
        "SETTINGS_DEFAULT_HWR_ON",
        "手写",
        "依赖手写语言包和当前输入语言。",
    ),
    HiddenSettingOption(
        "settings_direct_writing",
        "S Pen 直写",
        "依赖 S Pen/支持机型，部分 Knox 或安全模式下会隐藏。",
    ),
    HiddenSettingOption(
        "SETTINGS_SAVE_SCREENSHOTS_TO_CLIPBOARD",
        "截图保存到剪贴板",
        "依赖剪贴板、截图能力和系统策略。",
    ),
    HiddenSettingOption(
        "SETTINGS_PHYSICAL_KEYBOARD_TOOLBAR",
        "实体键盘工具栏",
        "依赖外接键盘或硬件键盘模式。",
    ),
    HiddenSettingOption(
        "SETTINGS_SHOW_BUTTON_TO_HIDE_KEYBOARD_RELATIVE_LINK",
        "隐藏键盘按钮相关链接",
        "Knox/Secure Folder 或部分导航模式下会隐藏。",
    ),
    HiddenSettingOption(
        "japanese_input_options",
        "日语输入选项",
        "需要已添加日语输入语言。",
    ),
    HiddenSettingOption(
        "enhanced_prediction",
        "中文增强预测",
        "依赖中文输入、地区和预测引擎。",
    ),
    HiddenSettingOption(
        "selected_language_download_cue",
        "语言包下载提示",
        "依赖语言包下载/更新服务状态。",
    ),
)

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
