package com.kalicyh.onemate;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.inputmethodservice.InputMethodService;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam;
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam;
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam;

public final class HoneyboardModule extends XposedModule {
    private static final String TAG = "OneMate";
    private static final String POLICY_CLASS = "Q6.c";
    private static final String POLICY_METHOD = "a";
    private static final String BEE_ITEM_CLASS =
            "com.samsung.android.honeyboard.beehive.data.BeeItem";
    private static final String BEE_INFO_BUILDER_CLASS = "N6.i";
    private static final String BEE_INFO_CLASS = "N6.j";
    private static final String BEE_INTERFACE_CLASS = "N6.c";
    private static final String BEE_WORLD_CLASS =
            "com.samsung.android.honeyboard.beehive.viewmodel.J";
    private static final String BEE_TAG_CLASS =
            "com.samsung.android.honeyboard.common.beehive.BeeTag";
    private static final String PRESET_BEE_SET_CLASS = "Aa.a";
    private static final String HONEYBOARD_SERVICE_CLASS =
            "com.samsung.android.honeyboard.service.HoneyBoardService";
    private static final String BOARD_CONFIG_CLASS = "n7.e";
    private static final String BOARD_MANAGER_CLASS = "Ea.e";
    private static final String BOARD_CREATOR_INTERFACE_CLASS = "T6.D";
    private static final String BOARD_INTERFACE_CLASS = "T6.b";
    private static final String BOARD_REQUEST_INFO_CLASS = "T6.F";
    private static final String SETTINGS_FRAGMENT_CLASS =
            "com.samsung.android.honeyboard.settings.common.CommonSettingsFragmentCompat";
    private static final String R_CLASS = "com.samsung.android.honeyboard.R";
    private static final String TEXT_EDITING_ID = "text_editing";
    private static final String TEXT_EDITING_BOARD_ID = "onemate_text_editing_board";
    private static final int VISIBILITY_HIDDEN = 2;
    private static final int VISIBILITY_VISIBLE = 0;

    private SharedPreferences prefs;
    private final Map<Object, Map<String, Object>> syntheticBees =
            new java.util.WeakHashMap<>();
    private final Map<Object, EditorOverlay> editorOverlays =
            new java.util.WeakHashMap<>();
    private final ThreadLocal<Boolean> addingSyntheticBee = new ThreadLocal<>();
    private InputMethodService currentService;
    private volatile Object boardConfig;
    private volatile Object textEditingBoardRequester;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        if ((getFrameworkProperties() & PROP_CAP_REMOTE) != 0) {
            prefs = getRemotePreferences(ToolbarConfig.PREF_GROUP);
        }
        log(Log.INFO, TAG, "loaded in " + param.getProcessName());
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!ToolbarConfig.TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        ClassLoader classLoader = param.getClassLoader();
        hookToolbarVisibility(classLoader);
        hookBeeTagLists(classLoader);
        hookBeeWorldRegistration(classLoader);
        hookBeeLookup(classLoader);
        hookBeeItemVisibility(classLoader);
        hookBoardConfig(classLoader);
        hookBoardRequester(classLoader);
        hookEditorOverlay(classLoader);
        hookHiddenSettings(classLoader);
    }

    @Override
    public boolean onHotReloading(HotReloadingParam param) {
        return true;
    }

    private void hookToolbarVisibility(ClassLoader classLoader) {
        try {
            Class<?> policyClass = Class.forName(POLICY_CLASS, false, classLoader);
            Method method = policyClass.getDeclaredMethod(POLICY_METHOD, String.class);
            method.setAccessible(true);
            hook(method)
                    .setId("toolbar-visibility")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Integer
                                && ((Integer) result).intValue() == VISIBILITY_HIDDEN
                                && shouldForceVisible(chain.getArg(0))) {
                            return VISIBILITY_VISIBLE;
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + POLICY_CLASS + "#" + POLICY_METHOD);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook Samsung Keyboard toolbar policy", t);
        }
    }

    private void hookBeeLookup(ClassLoader classLoader) {
        try {
            Class<?> beeWorldClass = Class.forName(BEE_WORLD_CLASS, false, classLoader);
            Method method = beeWorldClass.getDeclaredMethod("i", String.class);
            method.setAccessible(true);
            hook(method)
                    .setId("force-bee-lookup")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object beeId = chain.getArg(0);
                        if (result == null
                                && TEXT_EDITING_ID.equals(beeId)
                                && !Boolean.TRUE.equals(addingSyntheticBee.get())
                                && shouldForceVisible(beeId)) {
                            Object bee = getOrCreateSyntheticBee(
                                    chain.getThisObject(), classLoader, (String) beeId);
                            if (bee != null) {
                                log(Log.INFO, TAG, "created synthetic bee " + beeId);
                                return bee;
                            }
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + BEE_WORLD_CLASS + "#i");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "skip missing bee lookup hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook bee lookup", t);
        }
    }

    private void hookBeeWorldRegistration(ClassLoader classLoader) {
        try {
            Class<?> beeWorldClass = Class.forName(BEE_WORLD_CLASS, false, classLoader);
            Class<?> beeInterface = Class.forName(BEE_INTERFACE_CLASS, false, classLoader);
            int hooked = 0;
            for (Constructor<?> constructor : beeWorldClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 5) {
                    continue;
                }
                constructor.setAccessible(true);
                int index = hooked++;
                hook(constructor)
                        .setId("register-text-editing-bee-" + index)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            registerSyntheticBee(chain.getThisObject(), classLoader, beeInterface);
                            return result;
                        });
            }
            Method addBee = beeWorldClass.getDeclaredMethod("a", beeInterface);
            addBee.setAccessible(true);
            hook(addBee)
                    .setId("register-text-editing-after-add")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!Boolean.TRUE.equals(addingSyntheticBee.get())) {
                            registerSyntheticBee(chain.getThisObject(), classLoader, beeInterface);
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + BEE_WORLD_CLASS + " constructors=" + hooked);
        } catch (ClassNotFoundException e) {
            log(Log.DEBUG, TAG, "skip missing bee world registration hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook bee world registration", t);
        }
    }

    private void hookBeeItemVisibility(ClassLoader classLoader) {
        try {
            Class<?> beeItemClass = Class.forName(BEE_ITEM_CLASS, false, classLoader);
            Method method = beeItemClass.getDeclaredMethod("getBeeVisibility");
            method.setAccessible(true);
            hook(method)
                    .setId("force-bee-item-visible")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        String beeId = readStringMethod(chain.getThisObject(), "getBeeId");
                        if (shouldForceVisible(beeId)) {
                            return VISIBILITY_VISIBLE;
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + BEE_ITEM_CLASS + "#getBeeVisibility");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "skip missing bee item visibility hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook bee item visibility", t);
        }
    }

    private void hookBeeTagLists(ClassLoader classLoader) {
        hookBeeTagList(classLoader, PRESET_BEE_SET_CLASS, "preset");
        hookBeeTagList(classLoader, "wa.b", "user-lower");
        hookBeeTagList(classLoader, "Wa.b", "user-upper");
    }

    private void hookBeeTagList(ClassLoader classLoader, String className, String label) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            Method method = clazz.getDeclaredMethod("d");
            method.setAccessible(true);
            hook(method)
                    .setId("text-edit-bee-list-" + label)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!(result instanceof java.util.List)
                                || !shouldForceVisible(TEXT_EDITING_ID)) {
                            return result;
                        }
                        return listWithTextEditing((java.util.List<?>) result, classLoader);
                    });
            log(Log.INFO, TAG, "hooked " + className + "#d");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "skip missing bee tag list " + className);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook bee tag list " + className, t);
        }
    }

    private void hookEditorOverlay(ClassLoader classLoader) {
        try {
            Class<?> serviceClass = Class.forName(HONEYBOARD_SERVICE_CLASS, false, classLoader);

            Method onStartInputView =
                    serviceClass.getDeclaredMethod("onStartInputView", EditorInfo.class, boolean.class);
            onStartInputView.setAccessible(true);
            hook(onStartInputView)
                    .setId("text-edit-overlay-start")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        syncEditorOverlay(chain.getThisObject());
                        return result;
                    });

            Method onWindowShown = serviceClass.getDeclaredMethod("onWindowShown");
            onWindowShown.setAccessible(true);
            hook(onWindowShown)
                    .setId("text-edit-overlay-window-shown")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        syncEditorOverlay(chain.getThisObject());
                        return result;
                    });

            Method onFinishInputView = serviceClass.getDeclaredMethod("onFinishInputView", boolean.class);
            onFinishInputView.setAccessible(true);
            hook(onFinishInputView)
                    .setId("text-edit-overlay-finish")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        dismissEditorOverlay(chain.getThisObject());
                        return result;
                    });

            Method onWindowHidden = serviceClass.getDeclaredMethod("onWindowHidden");
            onWindowHidden.setAccessible(true);
            hook(onWindowHidden)
                    .setId("text-edit-overlay-window-hidden")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        dismissEditorOverlay(chain.getThisObject());
                        return result;
                    });

            Method onDestroy = serviceClass.getDeclaredMethod("onDestroy");
            onDestroy.setAccessible(true);
            hook(onDestroy)
                    .setId("text-edit-overlay-destroy")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        dismissEditorOverlay(chain.getThisObject());
                        return chain.proceed();
                    });

            log(Log.INFO, TAG, "hooked " + HONEYBOARD_SERVICE_CLASS + " text editor overlay");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "skip missing editor overlay hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook editor overlay", t);
        }
    }

    private void hookBoardConfig(ClassLoader classLoader) {
        try {
            Class<?> boardConfigClass = Class.forName(BOARD_CONFIG_CLASS, false, classLoader);
            int hooked = 0;
            for (Constructor<?> constructor : boardConfigClass.getDeclaredConstructors()) {
                constructor.setAccessible(true);
                int index = hooked++;
                hook(constructor)
                        .setId("capture-board-config-" + index)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            boardConfig = chain.getThisObject();
                            return result;
                        });
            }
            Method setHandwritingMode = boardConfigClass.getDeclaredMethod("r", boolean.class);
            setHandwritingMode.setAccessible(true);
            hook(setHandwritingMode)
                    .setId("sync-text-editing-with-handwriting")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(chain.getArg(0))) {
                            clearTextEditingState();
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + BOARD_CONFIG_CLASS + " constructors=" + hooked);
        } catch (ClassNotFoundException e) {
            log(Log.DEBUG, TAG, "skip missing board config hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook board config", t);
        }
    }

    private void hookBoardRequester(ClassLoader classLoader) {
        try {
            Class<?> boardManagerClass = Class.forName(BOARD_MANAGER_CLASS, false, classLoader);
            Class<?> boardCreatorClass =
                    Class.forName(BOARD_CREATOR_INTERFACE_CLASS, false, classLoader);
            int hooked = 0;
            for (Constructor<?> constructor : boardManagerClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() != 3) {
                    continue;
                }
                constructor.setAccessible(true);
                int index = hooked++;
                hook(constructor)
                        .setId("register-text-editing-board-" + index)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            Object result = chain.proceed();
                            Object context = chain.getArg(0);
                            if (context instanceof Context && shouldForceVisible(TEXT_EDITING_ID)) {
                                registerTextEditingBoard(
                                        chain.getThisObject(),
                                        (Context) context,
                                        classLoader,
                                        boardCreatorClass);
                            }
                            return result;
                        });
            }
            log(Log.INFO, TAG, "hooked " + BOARD_MANAGER_CLASS + " constructors=" + hooked);
        } catch (ClassNotFoundException e) {
            log(Log.DEBUG, TAG, "skip missing board requester hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook board requester", t);
        }
    }

    private void hookHiddenSettings(ClassLoader classLoader) {
        try {
            Class<?> settingsClass = Class.forName(SETTINGS_FRAGMENT_CLASS, false, classLoader);
            Method method = settingsClass.getDeclaredMethod("isPreferenceVisible", String.class);
            method.setAccessible(true);
            hook(method)
                    .setId("show-hidden-keyboard-settings")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object preferenceKey = chain.getArg(0);
                        Object result = chain.proceed();
                        boolean runtimeEnabled = result instanceof Boolean && (Boolean) result;
                        publishHiddenSettingRuntimeEnabled(chain.getThisObject(), preferenceKey, runtimeEnabled);
                        if (!runtimeEnabled && shouldForceSettingsPreferenceVisible(preferenceKey)) {
                            return true;
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "hooked " + SETTINGS_FRAGMENT_CLASS + "#isPreferenceVisible");
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log(Log.DEBUG, TAG, "skip missing keyboard settings hook");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to hook keyboard settings visibility", t);
        }
    }

    private boolean shouldForceVisible(Object beeId) {
        return beeId instanceof String
                && ToolbarConfig.isTextEditingEnabled(prefs)
                && TEXT_EDITING_ID.equals(beeId);
    }

    private boolean shouldForceSettingsPreferenceVisible(Object preferenceKey) {
        return ToolbarConfig.shouldForceSettingsPreference(prefs, preferenceKey);
    }

    private void publishHiddenSettingRuntimeEnabled(
            Object source, Object preferenceKey, boolean runtimeEnabled) {
        if (!(preferenceKey instanceof String)
                || !ToolbarConfig.isKnownHiddenSetting((String) preferenceKey)) {
            return;
        }
        Context context = contextFrom(source);
        if (context == null) {
            return;
        }
        Intent intent = new Intent(ToolbarConfig.ACTION_HIDDEN_SETTING_RUNTIME)
                .setClassName("com.kalicyh.onemate", "com.kalicyh.onemate.HiddenSettingsStateReceiver")
                .putExtra(ToolbarConfig.EXTRA_HIDDEN_SETTING_KEY, (String) preferenceKey)
                .putExtra(ToolbarConfig.EXTRA_HIDDEN_SETTING_RUNTIME_ENABLED, runtimeEnabled);
        context.sendBroadcast(intent);
    }

    private Context contextFrom(Object source) {
        if (source instanceof Context) {
            return (Context) source;
        }
        if (source == null) {
            return null;
        }
        Context context = invokeContextMethod(source, "getContext");
        if (context != null) {
            return context;
        }
        return invokeContextMethod(source, "getActivity");
    }

    private Context invokeContextMethod(Object source, String methodName) {
        try {
            Method method = source.getClass().getMethod(methodName);
            Object result = method.invoke(source);
            return result instanceof Context ? (Context) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void syncEditorOverlay(Object serviceObject) {
        if (!(serviceObject instanceof InputMethodService)) {
            return;
        }
        InputMethodService service = (InputMethodService) serviceObject;
        currentService = service;
        EditorOverlay overlay = getEditorOverlay(service);
        if (!shouldForceVisible(TEXT_EDITING_ID)) {
            overlay.dismissAll();
        }
    }

    private void dismissEditorOverlay(Object serviceObject) {
        if (serviceObject == currentService) {
            currentService = null;
        }
        EditorOverlay overlay = editorOverlays.get(serviceObject);
        if (overlay != null) {
            overlay.dismissAll();
        }
    }

    private void clearTextEditingState() {
        synchronized (syntheticBees) {
            for (Map<String, Object> bees : syntheticBees.values()) {
                Object bee = bees.get(TEXT_EDITING_ID);
                if (bee != null && Proxy.isProxyClass(bee.getClass())) {
                    InvocationHandler handler = Proxy.getInvocationHandler(bee);
                    if (handler instanceof SyntheticBeeHandler) {
                        ((SyntheticBeeHandler) handler).clearSelection();
                    }
                }
            }
        }
        synchronized (editorOverlays) {
            for (EditorOverlay overlay : editorOverlays.values()) {
                overlay.dismissAll();
            }
        }
    }

    private void setHandwritingMode(boolean enabled) {
        Object config = boardConfig;
        if (config == null) {
            return;
        }
        try {
            Method method = config.getClass().getDeclaredMethod("r", boolean.class);
            method.setAccessible(true);
            method.invoke(config, enabled);
        } catch (Throwable t) {
            log(Log.DEBUG, TAG, "failed to update handwriting mode", t);
        }
    }

    private void registerTextEditingBoard(
            Object boardManager, Context context, ClassLoader classLoader, Class<?> boardCreatorClass) {
        try {
            Object creator = Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{boardCreatorClass},
                    new TextEditingBoardCreatorHandler(context, classLoader));
            Method method = boardManager.getClass().getDeclaredMethod(
                    "y", String.class, boardCreatorClass, String.class);
            method.setAccessible(true);
            method.invoke(boardManager, TEXT_EDITING_BOARD_ID, creator, null);
            log(Log.INFO, TAG, "registered native text editing board");
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to register native text editing board", t);
        }
    }

    private Object createTextEditingBoard(
            Context context, ClassLoader classLoader, Object boardRequester) {
        try {
            Class<?> boardInterface = Class.forName(BOARD_INTERFACE_CLASS, false, classLoader);
            return Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[]{boardInterface},
                    new TextEditingBoardHandler(context, boardRequester));
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to create native text editing board", t);
            return null;
        }
    }

    private boolean requestTextEditingBoard() {
        Object requester = textEditingBoardRequester;
        if (requester == null) {
            log(Log.INFO, TAG, "skip native text editing board request: requester is null");
            return false;
        }
        try {
            ClassLoader classLoader = requester.getClass().getClassLoader();
            Class<?> requestInfoClass = Class.forName(
                    BOARD_REQUEST_INFO_CLASS, false, classLoader);
            Method method = requester.getClass().getMethod(
                    "D", String.class, requestInfoClass, boolean.class);
            method.invoke(requester, TEXT_EDITING_BOARD_ID, defaultBoardRequestInfo(requestInfoClass), false);
            log(Log.INFO, TAG, "requested native text editing board via "
                    + requester.getClass().getName());
            return true;
        } catch (Throwable t) {
            log(Log.INFO, TAG, "failed to request native text editing board", t);
            return false;
        }
    }

    private Object defaultBoardRequestInfo(Class<?> requestInfoClass)
            throws ReflectiveOperationException {
        for (Field field : requestInfoClass.getDeclaredFields()) {
            if (field.getType() == requestInfoClass
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                return field.get(null);
            }
        }
        throw new NoSuchFieldException("default " + requestInfoClass.getName());
    }

    private boolean hideTextEditingBoard(Object requester) {
        if (requester == null) {
            requester = textEditingBoardRequester;
        }
        if (requester == null) {
            return false;
        }
        try {
            Method method = requester.getClass().getMethod("q", String.class);
            method.invoke(requester, TEXT_EDITING_BOARD_ID);
            clearTextEditingState();
            return true;
        } catch (Throwable t) {
            log(Log.DEBUG, TAG, "failed to hide native text editing board", t);
            return false;
        }
    }

    private boolean isTextEditingBoardActive() {
        Object requester = textEditingBoardRequester;
        if (requester == null) {
            return false;
        }
        try {
            Method method = requester.getClass().getMethod("e", String.class);
            Object result = method.invoke(requester, TEXT_EDITING_BOARD_ID);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private View buildTextEditingBoardView(Context context, Object boardRequester) {
        InputMethodService service = currentService;
        if (service == null) {
            service = asInputMethodService(context);
        }
        if (service == null) {
            TextView fallback = new TextView(context);
            fallback.setText("Text editing");
            fallback.setGravity(Gravity.CENTER);
            PanelColors colors = panelColors(context);
            fallback.setTextColor(colors.text);
            fallback.setBackgroundColor(colors.background);
            return fallback;
        }
        return new EditorOverlay().buildPanel(service);
    }

    private void registerSyntheticBee(Object beeWorld, ClassLoader classLoader, Class<?> beeInterface) {
        if (beeWorld == null || !shouldForceVisible(TEXT_EDITING_ID)) {
            return;
        }
        if (hasActiveBee(beeWorld, TEXT_EDITING_ID)) {
            return;
        }
        Object bee = getOrCreateSyntheticBee(beeWorld, classLoader, TEXT_EDITING_ID);
        if (bee == null) {
            return;
        }
        try {
            Method addBee = beeWorld.getClass().getDeclaredMethod("a", beeInterface);
            addBee.setAccessible(true);
            addingSyntheticBee.set(Boolean.TRUE);
            try {
                addBee.invoke(beeWorld, bee);
            } finally {
                addingSyntheticBee.remove();
            }
            log(Log.INFO, TAG, "requested native registration for " + TEXT_EDITING_ID);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to register synthetic bee " + TEXT_EDITING_ID, t);
        }
    }

    private boolean hasActiveBee(Object beeWorld, String beeId) {
        addingSyntheticBee.set(Boolean.TRUE);
        try {
            Object bee = callOneArgQuietly(beeWorld, "i", String.class, beeId);
            return bee != null;
        } finally {
            addingSyntheticBee.remove();
        }
    }

    private Object callOneArgQuietly(
            Object target, String methodName, Class<?> argClass, Object arg) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, argClass);
            method.setAccessible(true);
            return method.invoke(target, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private java.util.List<?> listWithTextEditing(java.util.List<?> list, ClassLoader classLoader)
            throws ReflectiveOperationException {
        if (list.isEmpty()) {
            return list;
        }
        java.util.ArrayList<Object> copy = new java.util.ArrayList<>(list);
        removeBeeId(copy, TEXT_EDITING_ID);
        copy.add(0, newBeeTag(classLoader, TEXT_EDITING_ID));
        return copy;
    }

    private void removeBeeId(java.util.List<Object> list, String beeId) {
        for (int i = list.size() - 1; i >= 0; i--) {
            String id = readStringMethod(list.get(i), "getId");
            if (beeId.equals(id)) {
                list.remove(i);
            }
        }
    }

    private Object newBeeTag(ClassLoader classLoader, String beeId)
            throws ReflectiveOperationException {
        Class<?> beeTagClass = Class.forName(BEE_TAG_CLASS, false, classLoader);
        Constructor<?> constructor = beeTagClass.getDeclaredConstructor(String.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(beeId, false);
    }

    private EditorOverlay getEditorOverlay(InputMethodService service) {
        synchronized (editorOverlays) {
            EditorOverlay overlay = editorOverlays.get(service);
            if (overlay == null) {
                overlay = new EditorOverlay();
                editorOverlays.put(service, overlay);
            }
            return overlay;
        }
    }

    private String readStringMethod(Object item, String methodName) {
        if (item == null) {
            return null;
        }
        try {
            Method method = item.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(item);
            return value instanceof String ? (String) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object getOrCreateSyntheticBee(Object beeWorld, ClassLoader classLoader, String beeId) {
        if (beeWorld == null) {
            return null;
        }
        synchronized (syntheticBees) {
            Map<String, Object> bees = syntheticBees.get(beeWorld);
            if (bees == null) {
                bees = new HashMap<>();
                syntheticBees.put(beeWorld, bees);
            }
            Object existing = bees.get(beeId);
            if (existing != null) {
                return existing;
            }
            Object created = createSyntheticBee(beeWorld, classLoader, beeId);
            if (created != null) {
                bees.put(beeId, created);
            }
            return created;
        }
    }

    private Object createSyntheticBee(Object beeWorld, ClassLoader classLoader, String beeId) {
        try {
            Class<?> beeInterface = Class.forName(BEE_INTERFACE_CLASS, false, classLoader);
            Context context = honeyboardContext(beeWorld);
            Object beeInfo = newBeeInfo(context, classLoader);
            InvocationHandler handler = new SyntheticBeeHandler(context, beeId, beeInfo);
            return Proxy.newProxyInstance(classLoader, new Class<?>[]{beeInterface}, handler);
        } catch (Throwable t) {
            log(Log.ERROR, TAG, "failed to create synthetic bee " + beeId, t);
            return null;
        }
    }

    private Context honeyboardContext(Object beeWorld) throws ReflectiveOperationException {
        Object context = callNoArg(beeWorld, "l");
        if (!(context instanceof Context)) {
            throw new ReflectiveOperationException("missing Honeyboard context");
        }
        return (Context) context;
    }

    private Object newBeeInfo(Context context, ClassLoader classLoader) throws ReflectiveOperationException {
        int labelId = optionalResourceId(context, classLoader, "string", "text_editing");
        if (labelId == 0) {
            labelId = optionalResourceId(context, classLoader, "string", "cursor_control_text");
        }
        if (labelId == 0) {
            labelId = optionalResourceId(context, classLoader, "string", "edit_toolbar");
        }
        if (labelId == 0) {
            throw new ReflectiveOperationException("missing text editing resources");
        }

        Class<?> builderClass = Class.forName(BEE_INFO_BUILDER_CLASS, false, classLoader);
        Icon icon = moduleIcon(context);
        Object builder;
        if (icon != null) {
            Constructor<?> builderConstructor = builderClass.getDeclaredConstructor(
                    android.content.Context.class,
                    Icon.class,
                    int.class,
                    android.content.res.Resources.class,
                    String.class);
            builderConstructor.setAccessible(true);
            builder = builderConstructor.newInstance(
                    context, icon, labelId, context.getResources(), context.getPackageName());
        } else {
            int iconId = optionalResourceId(
                    context, classLoader, "drawable", "ic_samsung_keyobard_toolbar_edit");
            if (iconId == 0) {
                iconId = optionalResourceId(context, classLoader, "drawable", "cursor_control");
            }
            if (iconId == 0) {
                throw new ReflectiveOperationException("missing text editing icon");
            }
            Constructor<?> builderConstructor =
                    builderClass.getDeclaredConstructor(android.content.Context.class, int.class, int.class);
            builderConstructor.setAccessible(true);
            builder = builderConstructor.newInstance(context, iconId, labelId);
        }
        setStringField(builder, "f6524f", "Text editing");
        setIntField(builder, "f6525g", labelId);
        setStringField(builder, "f6526h", "Text editing");

        Class<?> infoClass = Class.forName(BEE_INFO_CLASS, false, classLoader);
        Constructor<?> infoConstructor = infoClass.getDeclaredConstructor(builderClass);
        infoConstructor.setAccessible(true);
        return infoConstructor.newInstance(builder);
    }

    private Icon moduleIcon(Context context) {
        try {
            Context moduleContext = context.createPackageContext(
                    getModuleApplicationInfo().packageName, Context.CONTEXT_IGNORE_SECURITY);
            int iconId = moduleContext.getResources().getIdentifier(
                    "ic_toolbar_text_edit_panel", "drawable", moduleContext.getPackageName());
            return iconId == 0 ? null : Icon.createWithResource(moduleContext, iconId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Drawable moduleDrawable(Context context, String name) {
        try {
            Context moduleContext = context.createPackageContext(
                    getModuleApplicationInfo().packageName, Context.CONTEXT_IGNORE_SECURITY);
            int drawableId = moduleContext.getResources().getIdentifier(
                    name, "drawable", moduleContext.getPackageName());
            Drawable drawable = drawableId == 0 ? null : moduleContext.getDrawable(drawableId);
            return drawable == null ? null : drawable.mutate();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int resourceId(ClassLoader classLoader, String type, String name)
            throws ReflectiveOperationException {
        Class<?> rClass = Class.forName(R_CLASS + "$" + type, false, classLoader);
        Field field = rClass.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(null);
    }

    private int optionalResourceId(ClassLoader classLoader, String type, String name) {
        try {
            return resourceId(classLoader, type, name);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void setStringField(Object target, String fieldName, String value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Throwable ignored) {
        }
    }

    private int optionalResourceId(
            Context context, ClassLoader classLoader, String type, String name) {
        int id = context.getResources().getIdentifier(name, type, context.getPackageName());
        return id != 0 ? id : optionalResourceId(classLoader, type, name);
    }

    private void setIntField(Object target, String fieldName, int value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setInt(target, value);
        } catch (Throwable ignored) {
        }
    }

    private Object callNoArg(Object target, String methodName) throws ReflectiveOperationException {
        Method method;
        try {
            method = target.getClass().getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            method = target.getClass().getMethod(methodName);
        }
        method.setAccessible(true);
        return method.invoke(target);
    }

    private final class TextEditingBoardCreatorHandler implements InvocationHandler {
        private final Context context;
        private final ClassLoader classLoader;

        TextEditingBoardCreatorHandler(Context context, ClassLoader classLoader) {
            this.context = context;
            this.classLoader = classLoader;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == (args == null ? null : args[0]));
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("toString".equals(name)) {
                return "OneMateTextEditingBoardCreator";
            }
            if ("f".equals(name)) {
                Object boardRequester = args == null || args.length == 0 ? null : args[0];
                textEditingBoardRequester = boardRequester;
                return createTextEditingBoard(context, classLoader, boardRequester);
            }
            return null;
        }
    }

    private final class TextEditingBoardHandler implements InvocationHandler {
        private final Context context;
        private final Object boardRequester;
        private boolean isBound;

        TextEditingBoardHandler(Context context, Object boardRequester) {
            this.context = context;
            this.boardRequester = boardRequester;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == (args == null ? null : args[0]));
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("toString".equals(name)) {
                return "OneMateTextEditingBoard";
            }
            if ("getBoardId".equals(name)) {
                return TEXT_EDITING_BOARD_ID;
            }
            if ("getBoardView".equals(name)) {
                return buildTextEditingBoardView(context, boardRequester);
            }
            if ("getCandidateView".equals(name)
                    || "getSmartCandidateView".equals(name)
                    || "getSpellView".equals(name)) {
                return null;
            }
            if ("isBound".equals(name)) {
                return Boolean.valueOf(isBound);
            }
            if ("onBind".equals(name)) {
                isBound = true;
                return null;
            }
            if ("onUnbind".equals(name)) {
                isBound = false;
                clearTextEditingState();
                return null;
            }
            if ("getDumpKey".equals(name)) {
                return "onemate_text_editing";
            }
            if ("getDumpName".equals(name)) {
                return "OneMate Text Editing";
            }
            if ("needRebindOnViewTypeChanged".equals(name)) {
                return Boolean.TRUE;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (returnType == Integer.TYPE) {
                return Integer.valueOf(0);
            }
            return null;
        }
    }

    private final class SyntheticBeeHandler implements InvocationHandler {
        private final Context context;
        private final String beeId;
        private final Object beeInfo;
        private Object callback;
        private boolean isNew;
        private boolean isSleep;
        private boolean isSelected;

        SyntheticBeeHandler(Context context, String beeId, Object beeInfo) {
            this.context = context;
            this.beeId = beeId;
            this.beeInfo = beeInfo;
        }

        void clearSelection() {
            isSelected = false;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == (args == null ? null : args[0]));
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("toString".equals(name)) {
                return "OneMateSyntheticBee(" + beeId + ")";
            }
            if ("getBeeId".equals(name)) {
                return beeId;
            }
            if ("getBeeInfo".equals(name)) {
                return beeInfo;
            }
            if ("getBeeVisibility".equals(name)) {
                return Integer.valueOf(VISIBILITY_VISIBLE);
            }
            if ("getCallback".equals(name)) {
                return callback;
            }
            if ("setCallback".equals(name)) {
                callback = args == null ? null : args[0];
                return null;
            }
            if ("getBeeFlags".equals(name)) {
                return Integer.valueOf(1);
            }
            if ("getPinBeePriority".equals(name)) {
                return Integer.valueOf(Integer.MAX_VALUE);
            }
            if ("getSearchSuggestionType".equals(name)) {
                return Integer.valueOf(0);
            }
            if ("isNew".equals(name)) {
                return Boolean.valueOf(isNew);
            }
            if ("setNew".equals(name)) {
                isNew = args != null && Boolean.TRUE.equals(args[0]);
                return null;
            }
            if ("isSleep".equals(name) || "getIsSleep".equals(name)) {
                return Boolean.valueOf(isSleep);
            }
            if ("setSleep".equals(name)) {
                isSleep = args != null && Boolean.TRUE.equals(args[0]);
                return null;
            }
            if ("isPpAccepted".equals(name) || "needLabel".equals(name)) {
                return Boolean.TRUE;
            }
            if ("isSelected".equals(name)) {
                return Boolean.valueOf(isSelected || isTextEditingBoardActive());
            }
            if ("finish".equals(name)) {
                isSelected = false;
                hideTextEditingBoard(null);
                InputMethodService service = asInputMethodService(context);
                if (service == null) {
                    service = currentService;
                }
                if (service != null) {
                    getEditorOverlay(service).dismissAll();
                }
                return null;
            }
            if ("execute".equals(name)) {
                isNew = false;
                boolean shown = false;
                boolean handled = false;
                setHandwritingMode(false);
                if (isTextEditingBoardActive()) {
                    hideTextEditingBoard(null);
                    isSelected = false;
                    handled = true;
                } else if (requestTextEditingBoard()) {
                    isSelected = true;
                    shown = true;
                    handled = true;
                }
                InputMethodService service = asInputMethodService(context);
                if (service == null) {
                    service = currentService;
                }
                if (!handled && service != null) {
                    EditorOverlay overlay = getEditorOverlay(service);
                    if (overlay.isShown()) {
                        overlay.dismissAll();
                        isSelected = false;
                    } else {
                        shown = overlay.showPanel(service);
                        isSelected = shown;
                    }
                }
                if (args != null && args.length == 1 && args[0] != null) {
                    callNoArg(args[0], "invoke");
                }
                log(Log.INFO, TAG, "synthetic bee executed " + beeId + ", panel=" + shown);
                return null;
            }
            Class<?> returnType = method.getReturnType();
            if (returnType == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (returnType == Integer.TYPE) {
                return Integer.valueOf(0);
            }
            return null;
        }
    }

    private InputMethodService asInputMethodService(Context context) {
        Context current = context;
        for (int i = 0; i < 6 && current != null; i++) {
            if (current instanceof InputMethodService) {
                return (InputMethodService) current;
            }
            if (current instanceof ContextWrapper) {
                current = ((ContextWrapper) current).getBaseContext();
            } else {
                break;
            }
        }
        return null;
    }

    private View decorView(InputMethodService service) {
        try {
            Window window = service.getWindow() == null ? null : service.getWindow().getWindow();
            return window == null ? null : window.getDecorView();
        } catch (Throwable t) {
            return null;
        }
    }

    private int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    private PanelColors panelColors(Context context) {
        int background = resolveColor(context, android.R.attr.colorBackground, Color.rgb(38, 36, 40));
        return panelColorsFromBackground(background);
    }

    private PanelColors panelColors(InputMethodService service) {
        int background = dominantBackgroundColor(
                decorView(service),
                resolveColor(service, android.R.attr.colorBackground, Color.rgb(38, 36, 40)));
        return panelColorsFromBackground(background);
    }

    private PanelColors panelColorsFromBackground(int background) {
        boolean dark = isDark(background);
        int text = dark ? Color.WHITE : Color.BLACK;
        int pad = blend(background, dark ? Color.BLACK : Color.WHITE, 0.18f);
        int button = blend(background, text, 0.24f);
        int selected = blend(button, text, 0.22f);
        return new PanelColors(background, pad, button, selected, text);
    }

    private int dominantBackgroundColor(View view, int fallback) {
        int[] bestArea = new int[]{0};
        int[] bestColor = new int[]{fallback};
        collectBackgroundColor(view, bestArea, bestColor);
        return bestColor[0];
    }

    private void collectBackgroundColor(View view, int[] bestArea, int[] bestColor) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        int color = viewBackgroundColor(view);
        if (Color.alpha(color) > 180) {
            int width = Math.max(view.getWidth(), view.getMeasuredWidth());
            int height = Math.max(view.getHeight(), view.getMeasuredHeight());
            int area = width * height;
            if (area > bestArea[0]) {
                bestArea[0] = area;
                bestColor[0] = color;
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectBackgroundColor(group.getChildAt(i), bestArea, bestColor);
            }
        }
    }

    private int viewBackgroundColor(View view) {
        android.graphics.drawable.Drawable background = view.getBackground();
        if (background instanceof android.graphics.drawable.ColorDrawable) {
            return ((android.graphics.drawable.ColorDrawable) background).getColor();
        }
        if (background instanceof GradientDrawable) {
            android.content.res.ColorStateList color = ((GradientDrawable) background).getColor();
            if (color != null) {
                return color.getDefaultColor();
            }
        }
        return Color.TRANSPARENT;
    }

    private int resolveColor(Context context, int attr, int fallback) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true)) {
            return fallback;
        }
        if (value.resourceId != 0) {
            try {
                return context.getColor(value.resourceId);
            } catch (Throwable ignored) {
                return fallback;
            }
        }
        if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return value.data;
        }
        return fallback;
    }

    private boolean isDark(int color) {
        return (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) < 128000;
    }

    private int blend(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        return Color.rgb(
                Math.round(Color.red(from) * inverse + Color.red(to) * ratio),
                Math.round(Color.green(from) * inverse + Color.green(to) * ratio),
                Math.round(Color.blue(from) * inverse + Color.blue(to) * ratio));
    }

    private static final class PanelColors {
        final int background;
        final int pad;
        final int button;
        final int selectedButton;
        final int text;

        PanelColors(int background, int pad, int button, int selectedButton, int text) {
            this.background = background;
            this.pad = pad;
            this.button = button;
            this.selectedButton = selectedButton;
            this.text = text;
        }
    }

    private static final class PercentFrame extends FrameLayout {
        PercentFrame(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = View.MeasureSpec.getSize(widthMeasureSpec);
            int height = View.MeasureSpec.getSize(heightMeasureSpec);
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                float[] box = (float[]) child.getTag();
                int childWidth = Math.max(1, Math.round(width * box[2]));
                int childHeight = Math.max(1, Math.round(height * box[3]));
                child.measure(
                        View.MeasureSpec.makeMeasureSpec(childWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(childHeight, View.MeasureSpec.EXACTLY));
            }
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                float[] box = (float[]) child.getTag();
                int childLeft = Math.round(width * box[0]);
                int childTop = Math.round(height * box[1]);
                child.layout(
                        childLeft,
                        childTop,
                        childLeft + child.getMeasuredWidth(),
                        childTop + child.getMeasuredHeight());
            }
        }
    }

    private final class EditorOverlay {
        private View panel;
        private boolean selectMode;
        private int selectionAnchor = -1;

        boolean isShown() {
            return panel != null && panel.getParent() != null;
        }

        boolean showPanel(InputMethodService service) {
            View anchor = decorView(service);
            if (anchor == null) {
                return false;
            }
            anchor.post(() -> showPanelNow(service, anchor));
            return true;
        }

        void dismissAll() {
            removePanel();
            selectMode = false;
            selectionAnchor = -1;
        }

        private void showPanelNow(InputMethodService service, View anchor) {
            if (!shouldForceVisible(TEXT_EDITING_ID)) {
                dismissAll();
                return;
            }
            if (panel != null && panel.getParent() != null) {
                removePanel();
                return;
            }
            ViewGroup host = panelHost(anchor);
            if (host == null) {
                return;
            }
            panel = buildPanel(service);
            try {
                if (host instanceof FrameLayout) {
                    host.addView(panel, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            panelHeight(service, host),
                            Gravity.BOTTOM));
                } else {
                    host.addView(panel, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            panelHeight(service, host)));
                }
                log(Log.INFO, TAG, "shown native text edit panel");
            } catch (Throwable t) {
                log(Log.ERROR, TAG, "failed to attach text edit panel", t);
                panel = null;
            }
        }

        private ViewGroup panelHost(View anchor) {
            View root = anchor.getRootView();
            if (root instanceof ViewGroup) {
                return (ViewGroup) root;
            }
            if (anchor instanceof ViewGroup) {
                return (ViewGroup) anchor;
            }
            ViewParent parent = anchor.getParent();
            return parent instanceof ViewGroup ? (ViewGroup) parent : null;
        }

        private int panelHeight(Context context, View host) {
            int hostHeight = host.getHeight();
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int min = dp(context, 260);
            if (hostHeight > min && hostHeight < screenHeight * 3 / 4) {
                return Math.max(min, hostHeight - dp(context, 56));
            }
            int preferred = dp(context, 320);
            int max = Math.max(min, screenHeight / 2);
            return Math.min(preferred, max);
        }

        private void removePanel() {
            if (panel == null) {
                return;
            }
            ViewParent parent = panel.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(panel);
            }
            panel = null;
        }

        private View buildPanel(InputMethodService service) {
            PanelColors colors = panelColors(service);
            LinearLayout root = new LinearLayout(service);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(dp(service, 10), dp(service, 6), dp(service, 10), dp(service, 4));
            root.setBackgroundColor(colors.background);

            root.addView(header(service, colors), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(service, 42)));

            root.addView(legacyControls(service, colors), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            return root;
        }

        private View header(InputMethodService service, PanelColors colors) {
            LinearLayout header = new LinearLayout(service);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setOrientation(LinearLayout.HORIZONTAL);

            ImageView icon = new ImageView(service);
            Drawable iconDrawable = moduleDrawable(service, "ic_toolbar_text_edit_panel");
            if (iconDrawable != null) {
                icon.setImageDrawable(iconDrawable);
                icon.setColorFilter(colors.text);
            }
            header.addView(icon, new LinearLayout.LayoutParams(dp(service, 24), dp(service, 24)));

            View divider = new View(service);
            divider.setBackgroundColor(blend(colors.background, colors.text, 0.45f));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    dp(service, 1), dp(service, 24));
            dividerParams.setMargins(dp(service, 12), 0, dp(service, 12), 0);
            header.addView(divider, dividerParams);

            TextView title = new TextView(service);
            int titleId = service.getResources().getIdentifier(
                    "text_editing", "string", service.getPackageName());
            title.setText(titleId == 0 ? "Text editing" : service.getString(titleId));
            title.setTextColor(colors.text);
            title.setTextSize(18);
            title.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            header.addView(title, titleParams);
            return header;
        }

        private View legacyControls(InputMethodService service, PanelColors colors) {
            PercentFrame controls = new PercentFrame(service);
            controls.setBackgroundColor(colors.background);

            View selectContainer = new View(service);
            selectContainer.setBackground(roundRect(colors.pad, dp(service, 8)));
            addPercent(controls, selectContainer, 0.0417f, 0f, 0.6361f, 0.824f);

            LinearLayout sideGroup = new LinearLayout(service);
            sideGroup.setOrientation(LinearLayout.VERTICAL);
            sideGroup.setBackground(roundRect(colors.button, dp(service, 8)));
            addSideRow(sideGroup, "Cut", () -> menuAction(service, android.R.id.cut), colors);
            addSideRow(sideGroup, "Copy", () -> menuAction(service, android.R.id.copy), colors);
            addSideRow(sideGroup, "Paste", () -> menuAction(service, android.R.id.paste), colors);
            addPercent(controls, sideGroup, 0.7083f, 0f, 0.2556f, 0.612f);

            addPercent(controls, iconButton(service, "ic_textedit_up",
                            () -> move(service, KeyEvent.KEYCODE_DPAD_UP), colors, false),
                    0.2389f, 0f, 0.2389f, 0.288f);
            addPercent(controls, iconButton(service, "ic_textedit_left",
                            () -> move(service, KeyEvent.KEYCODE_DPAD_LEFT), colors, false),
                    0.0417f, 0.288f, 0.1777f, 0.248f);
            addPercent(controls, iconButton(service, "ic_textedit_right",
                            () -> move(service, KeyEvent.KEYCODE_DPAD_RIGHT), colors, false),
                    0.5f, 0.288f, 0.1778f, 0.248f);
            addPercent(controls, iconButton(service, "ic_textedit_down",
                            () -> move(service, KeyEvent.KEYCODE_DPAD_DOWN), colors, false),
                    0.2389f, 0.536f, 0.2389f, 0.288f);

            Button select = panelButton(service, "Select", 16, colors);
            select.setOnClickListener(v -> {
                selectMode = !selectMode;
                selectionAnchor = selectMode ? selectionEnd(service) : -1;
                select.setBackground(roundRect(
                        selectMode ? colors.selectedButton : colors.button,
                        dp(service, 28)));
            });
            addPercent(controls, select, 0.2389f, 0.344f, 0.2389f, 0.144f);

            addPercent(controls, iconButton(service, "ic_textedit_enter",
                            () -> sendKey(service, KeyEvent.KEYCODE_ENTER, 0), colors, true),
                    0.7083f, 0.656f, 0.2556f, 0.168f);

            addPercent(controls, iconButton(service, "ic_textedit_home",
                            () -> move(service, KeyEvent.KEYCODE_MOVE_HOME), colors, false),
                    0.069f, 0.868f, 0.0667f, 0.096f);
            addPercent(controls, textActionButton(service, "Select all",
                            () -> menuAction(service, android.R.id.selectAll), colors, 15),
                    0.183f, 0.868f, 0.353f, 0.096f);
            addPercent(controls, iconButton(service, "ic_textedit_end",
                            () -> move(service, KeyEvent.KEYCODE_MOVE_END), colors, false),
                    0.5806f, 0.868f, 0.0667f, 0.096f);

            View divider = new View(service);
            divider.setBackgroundColor(blend(colors.background, colors.text, 0.35f));
            addPercent(controls, divider, 0.752f, 0.868f, 0.0028f, 0.096f);

            addPercent(controls, iconButton(service, "ic_textedit_backspace",
                            () -> sendKey(service, KeyEvent.KEYCODE_DEL, 0), colors, false),
                    0.833f, 0.868f, 0.13f, 0.096f);

            return controls;
        }

        private void addPercent(PercentFrame parent, View child, float x, float y, float width, float height) {
            child.setTag(new float[]{x, y, width, height});
            parent.addView(child);
        }

        private void addSideRow(LinearLayout parent, String label, Runnable action, PanelColors colors) {
            Button button = textActionButton(parent.getContext(), label, action, colors, 16);
            parent.addView(button, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        private Button textActionButton(
                Context context, String label, Runnable action, PanelColors colors, int textSizeSp) {
            Button button = panelButton(context, label, textSizeSp, colors);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setOnClickListener(v -> action.run());
            return button;
        }

        private ImageButton iconButton(
                Context context, String drawableName, Runnable action, PanelColors colors, boolean solid) {
            ImageButton button = new ImageButton(context);
            Drawable drawable = moduleDrawable(context, drawableName);
            if (drawable != null) {
                button.setImageDrawable(drawable);
                button.setColorFilter(colors.text);
            }
            button.setScaleType(ImageView.ScaleType.CENTER);
            button.setPadding(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
            button.setBackground(solid ? roundRect(colors.button, dp(context, 8)) : null);
            button.setFocusable(false);
            button.setFocusableInTouchMode(false);
            button.setOnClickListener(v -> action.run());
            return button;
        }

        private Button panelButton(Context context, String label, int textSizeSp, PanelColors colors) {
            Button button = new Button(context);
            button.setText(label);
            button.setTextSize(textSizeSp);
            button.setTextColor(colors.text);
            button.setAllCaps(false);
            button.setFocusable(false);
            button.setFocusableInTouchMode(false);
            button.setMinWidth(0);
            button.setMinHeight(0);
            button.setPadding(dp(context, 4), 0, dp(context, 4), 0);
            button.setBackground(roundRect(colors.button, dp(context, 10)));
            return button;
        }

        private GradientDrawable roundRect(int color, int radius) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(radius);
            return drawable;
        }

        private void move(InputMethodService service, int keyCode) {
            if (selectMode && extendSelection(service, keyCode)) {
                return;
            }
            sendKey(service, keyCode, selectMode ? KeyEvent.META_SHIFT_ON : 0);
        }

        private boolean extendSelection(InputMethodService service, int keyCode) {
            InputConnection inputConnection = service.getCurrentInputConnection();
            if (inputConnection == null) {
                return false;
            }
            ExtractedText text = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
            if (text == null || text.text == null) {
                return false;
            }
            int length = text.text.length();
            int cursor = clamp(text.selectionEnd, 0, length);
            if (selectionAnchor < 0) {
                selectionAnchor = cursor;
            }
            int next;
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                next = Math.max(0, cursor - 1);
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                next = Math.min(length, cursor + 1);
            } else if (keyCode == KeyEvent.KEYCODE_MOVE_HOME) {
                next = 0;
            } else if (keyCode == KeyEvent.KEYCODE_MOVE_END) {
                next = length;
            } else {
                // ponytail: vertical selection falls back to key events; exact line navigation needs app text layout.
                return false;
            }
            inputConnection.setSelection(selectionAnchor, next);
            inputConnection.finishComposingText();
            return true;
        }

        private int selectionEnd(InputMethodService service) {
            InputConnection inputConnection = service.getCurrentInputConnection();
            if (inputConnection == null) {
                return -1;
            }
            ExtractedText text = inputConnection.getExtractedText(new ExtractedTextRequest(), 0);
            if (text == null || text.text == null) {
                return -1;
            }
            return clamp(text.selectionEnd, 0, text.text.length());
        }

        private int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private void sendKey(InputMethodService service, int keyCode, int metaState) {
            InputConnection inputConnection = service.getCurrentInputConnection();
            if (inputConnection == null) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            inputConnection.sendKeyEvent(new KeyEvent(
                    now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState));
            inputConnection.sendKeyEvent(new KeyEvent(
                    now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState));
        }

        private void menuAction(InputMethodService service, int actionId) {
            InputConnection inputConnection = service.getCurrentInputConnection();
            if (inputConnection != null) {
                inputConnection.performContextMenuAction(actionId);
            }
        }

    }
}
