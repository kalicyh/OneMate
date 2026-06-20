package com.kalicyh.onemate;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import io.github.libxposed.service.XposedService;

public final class MainActivity extends Activity implements App.ServiceStateListener {
    private TextView statusView;
    private Switch enabledSwitch;
    private Switch textEditingSwitch;
    private EditText extraIdsView;
    private Button saveButton;
    private SharedPreferences prefs;
    private boolean loadingPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("OneMate");
        setContentView(buildContentView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        App.addServiceStateListener(this);
    }

    @Override
    protected void onStop() {
        App.removeServiceStateListener(this);
        super.onStop();
    }

    @Override
    public void onServiceStateChanged(XposedService service) {
        runOnUiThread(() -> bindService(service));
    }

    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(18));
        scrollView.addView(root);

        statusView = text("", 14);
        root.addView(statusView);

        enabledSwitch = new Switch(this);
        enabledSwitch.setText("启用三星键盘 toolbar hook");
        enabledSwitch.setTextSize(16);
        root.addView(enabledSwitch);

        textEditingSwitch = new Switch(this);
        textEditingSwitch.setText("启用原生 text_editing 按钮");
        textEditingSwitch.setTextSize(16);
        root.addView(textEditingSwitch);

        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!loadingPrefs) {
                savePrefs();
            }
        });
        textEditingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!loadingPrefs) {
                savePrefs();
            }
        });

        root.addView(text("额外强制显示的 Bee ID", 16));
        extraIdsView = new EditText(this);
        extraIdsView.setMinLines(3);
        extraIdsView.setSingleLine(false);
        extraIdsView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        extraIdsView.setHint("一行一个或用逗号分隔，例如：\nedit_toolbar\nkbd_handwriting");
        root.addView(extraIdsView);

        saveButton = new Button(this);
        saveButton.setText("保存");
        saveButton.setOnClickListener(v -> savePrefs());
        root.addView(saveButton);

        root.addView(text("5.9.30+ 已删除旧版原生实现；模块会补原生按钮并显示仿旧版面板。", 13));
        return scrollView;
    }

    private void bindService(XposedService service) {
        prefs = null;
        boolean remoteSupported = false;
        String status;
        try {
            remoteSupported = service != null
                    && (service.getFrameworkProperties() & XposedService.PROP_CAP_REMOTE) != 0;
            if (remoteSupported) {
                prefs = service.getRemotePreferences(ToolbarConfig.PREF_GROUP);
            }
            status = buildStatus(service, remoteSupported);
        } catch (RuntimeException e) {
            status = "LSPosed service: 连接异常\n" + e.getMessage();
        }
        enabledSwitch.setEnabled(remoteSupported);
        textEditingSwitch.setEnabled(remoteSupported);
        extraIdsView.setEnabled(remoteSupported);
        saveButton.setEnabled(remoteSupported);
        loadPrefs();
        statusView.setText(status);
    }

    private String buildStatus(XposedService service, boolean remoteSupported) {
        if (service == null) {
            return "LSPosed service: 未连接\n请确认模块已安装并由支持 API 102 的 LSPosed/Vector 加载。";
        }
        boolean inScope = service.getScope().contains(ToolbarConfig.TARGET_PACKAGE);
        return "LSPosed service: 已连接"
                + "\nFramework: " + service.getFrameworkName() + " API " + service.getApiVersion()
                + "\nRemote preferences: " + (remoteSupported ? "可用" : "不可用")
                + "\nSamsung Keyboard scope: " + (inScope ? "已包含" : "未包含");
    }

    private void loadPrefs() {
        loadingPrefs = true;
        try {
            enabledSwitch.setChecked(ToolbarConfig.isEnabled(prefs));
            textEditingSwitch.setChecked(ToolbarConfig.isTextEditingEnabled(prefs));
            extraIdsView.setText(prefs == null ? "" : prefs.getString(ToolbarConfig.KEY_EXTRA_IDS, ""));
        } finally {
            loadingPrefs = false;
        }
    }

    private void savePrefs() {
        if (prefs == null) {
            Toast.makeText(this, "LSPosed remote preferences 不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        String extraIds = normalizeIds(ToolbarConfig.parseIds(extraIdsView.getText().toString()));
        boolean saved = prefs.edit()
                .putBoolean(ToolbarConfig.KEY_ENABLED, enabledSwitch.isChecked())
                .putBoolean(ToolbarConfig.KEY_FORCE_TEXT_EDITING, textEditingSwitch.isChecked())
                .putString(ToolbarConfig.KEY_EXTRA_IDS, extraIds)
                .commit();
        if (!saved) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            return;
        }
        extraIdsView.setText(extraIds);
        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
    }

    private static String normalizeIds(Set<String> ids) {
        StringBuilder builder = new StringBuilder();
        for (String id : ids) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(id);
        }
        return builder.toString();
    }

    private TextView text(String value, int sp) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
