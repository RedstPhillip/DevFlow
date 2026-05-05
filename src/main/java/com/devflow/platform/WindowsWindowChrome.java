package com.devflow.platform;

import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.reflect.Method;
import java.util.List;

public final class WindowsWindowChrome {
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    private static final int GWL_WNDPROC = -4;
    private static final int GWL_STYLE = -16;
    private static final int WM_NCHITTEST = 0x0084;
    private static final int WM_NCLBUTTONDOWN = 0x00A1;
    private static final int WM_NCDESTROY = 0x0082;
    private static final int DWMWA_BORDER_COLOR = 34;
    private static final int DWMWA_CAPTION_COLOR = 35;
    private static final int DWMWA_TEXT_COLOR = 36;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWCP_ROUND = 2;
    private static final int DWMWA_COLOR_NONE = 0xFFFFFFFE;

    private static final long WS_SYSMENU = 0x00080000L;
    private static final long WS_THICKFRAME = 0x00040000L;
    private static final long WS_MINIMIZEBOX = 0x00020000L;
    private static final long WS_MAXIMIZEBOX = 0x00010000L;

    private static final int SWP_NOSIZE = 0x0001;
    private static final int SWP_NOMOVE = 0x0002;
    private static final int SWP_NOZORDER = 0x0004;
    private static final int SWP_NOACTIVATE = 0x0010;
    private static final int SWP_FRAMECHANGED = 0x0020;

    private static final int HTCLIENT = 1;
    private static final int HTCAPTION = 2;
    private static final int HTLEFT = 10;
    private static final int HTRIGHT = 11;
    private static final int HTTOP = 12;
    private static final int HTTOPLEFT = 13;
    private static final int HTTOPRIGHT = 14;
    private static final int HTBOTTOM = 15;
    private static final int HTBOTTOMLEFT = 16;
    private static final int HTBOTTOMRIGHT = 17;
    private static final int CAPTION_DARK = colorRef(0x0e, 0x10, 0x18);
    private static final int TEXT_DARK = colorRef(0xec, 0xed, 0xf2);

    private final Stage stage;
    private final Region titleBar;
    private final Button minButton;
    private final Button maxButton;
    private final Button closeButton;
    private final Runnable fallbackDragInstaller;
    private final WindowProc wndProc = this::handleMessage;
    private final ChangeListener<Object> refreshListener = (obs, oldValue, newValue) -> scheduleRefresh();
    private final EventHandler<WindowEvent> shownHandler = e -> {
        installNativeOrFallback();
        scheduleRefresh();
    };
    private final EventHandler<MouseEvent> nativeDragHandler = this::handleNativeDragPress;

    private HWND hwnd;
    private Pointer oldWndProc;
    private Pointer oldStyle;
    private boolean installed;
    private boolean fallbackInstalled;
    private boolean refreshQueued;
    private HitZones zones = HitZones.empty();

    private WindowsWindowChrome(
            Stage stage,
            Region titleBar,
            Button minButton,
            Button maxButton,
            Button closeButton,
            Runnable fallbackDragInstaller
    ) {
        this.stage = stage;
        this.titleBar = titleBar;
        this.minButton = minButton;
        this.maxButton = maxButton;
        this.closeButton = closeButton;
        this.fallbackDragInstaller = fallbackDragInstaller;
    }

    public static WindowsWindowChrome install(
            Stage stage,
            Region titleBar,
            Button minButton,
            Button maxButton,
            Button closeButton,
            Runnable fallbackDragInstaller
    ) {
        if (!WINDOWS) {
            fallbackDragInstaller.run();
            return null;
        }

        WindowsWindowChrome chrome = new WindowsWindowChrome(
                stage,
                titleBar,
                minButton,
                maxButton,
                closeButton,
                fallbackDragInstaller
        );
        chrome.attach();
        return chrome;
    }

    public void dispose() {
        titleBar.boundsInLocalProperty().removeListener(refreshListener);
        titleBar.localToSceneTransformProperty().removeListener(refreshListener);
        minButton.boundsInLocalProperty().removeListener(refreshListener);
        maxButton.boundsInLocalProperty().removeListener(refreshListener);
        closeButton.boundsInLocalProperty().removeListener(refreshListener);
        stage.xProperty().removeListener(refreshListener);
        stage.yProperty().removeListener(refreshListener);
        stage.widthProperty().removeListener(refreshListener);
        stage.heightProperty().removeListener(refreshListener);
        stage.maximizedProperty().removeListener(refreshListener);
        stage.removeEventHandler(WindowEvent.WINDOW_SHOWN, shownHandler);
        titleBar.removeEventFilter(MouseEvent.MOUSE_PRESSED, nativeDragHandler);

        if (installed && hwnd != null && oldWndProc != null) {
            try {
                User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, oldWndProc);
                if (oldStyle != null) {
                    User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_STYLE, oldStyle);
                    refreshNativeFrame(hwnd);
                }
            } catch (RuntimeException ex) {
                System.err.println("Failed to restore Windows window proc: " + ex.getMessage());
            }
        }
        installed = false;
        hwnd = null;
        oldWndProc = null;
        oldStyle = null;
    }

    private void attach() {
        titleBar.boundsInLocalProperty().addListener(refreshListener);
        titleBar.localToSceneTransformProperty().addListener(refreshListener);
        minButton.boundsInLocalProperty().addListener(refreshListener);
        maxButton.boundsInLocalProperty().addListener(refreshListener);
        closeButton.boundsInLocalProperty().addListener(refreshListener);
        stage.xProperty().addListener(refreshListener);
        stage.yProperty().addListener(refreshListener);
        stage.widthProperty().addListener(refreshListener);
        stage.heightProperty().addListener(refreshListener);
        stage.maximizedProperty().addListener(refreshListener);

        stage.addEventHandler(WindowEvent.WINDOW_SHOWN, shownHandler);
        titleBar.addEventFilter(MouseEvent.MOUSE_PRESSED, nativeDragHandler);
        Platform.runLater(() -> {
            installNativeOrFallback();
            scheduleRefresh();
        });
    }

    private void handleNativeDragPress(MouseEvent event) {
        if (!isNativeInstalled()
                || event.getButton() != MouseButton.PRIMARY
                || isWindowButtonTarget(event.getTarget())) {
            return;
        }

        if (event.getClickCount() == 2) {
            stage.setMaximized(!stage.isMaximized());
            event.consume();
            return;
        }

        User32.INSTANCE.ReleaseCapture();
        User32.INSTANCE.SendMessage(hwnd, WM_NCLBUTTONDOWN, Pointer.createConstant(HTCAPTION), Pointer.NULL);
        event.consume();
    }

    private boolean isWindowButtonTarget(Object target) {
        if (!(target instanceof Node node)) return false;
        while (node != null) {
            if (node == minButton || node == maxButton || node == closeButton) return true;
            node = node.getParent();
        }
        return false;
    }

    private void installNativeOrFallback() {
        if (installed || fallbackInstalled || !stage.isShowing()) {
            return;
        }

        try {
            long handle = findNativeWindowHandle(stage);
            if (handle == 0) {
                Platform.runLater(this::installNativeOrFallback);
                return;
            }

            hwnd = new HWND(handle);
            oldStyle = enableNativeFrameStyles(hwnd);
            applyDwmFrameColors(hwnd);
            oldWndProc = User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, wndProc);
            installed = oldWndProc != null;
            if (!installed) {
                installFallback();
            } else {
                scheduleRefresh();
            }
        } catch (RuntimeException | ReflectiveOperationException | LinkageError ex) {
            System.err.println("Native Windows chrome unavailable, falling back to JavaFX drag: " + ex.getMessage());
            installFallback();
        }
    }

    private void installFallback() {
        if (!fallbackInstalled) {
            fallbackInstalled = true;
            fallbackDragInstaller.run();
        }
    }

    private Pointer enableNativeFrameStyles(HWND hwnd) {
        Pointer currentStylePointer = User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_STYLE);
        long currentStyle = Pointer.nativeValue(currentStylePointer);
        long nativeFrameStyle = currentStyle
                | WS_SYSMENU
                | WS_THICKFRAME
                | WS_MINIMIZEBOX
                | WS_MAXIMIZEBOX;

        if (nativeFrameStyle != currentStyle) {
            User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_STYLE, Pointer.createConstant(nativeFrameStyle));
            refreshNativeFrame(hwnd);
        }
        return currentStylePointer;
    }

    private void refreshNativeFrame(HWND hwnd) {
        User32.INSTANCE.SetWindowPos(
                hwnd,
                null,
                0,
                0,
                0,
                0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED
        );
    }

    private void applyDwmFrameColors(HWND hwnd) {
        setDwmInt(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, DWMWCP_ROUND);
        setDwmInt(hwnd, DWMWA_BORDER_COLOR, DWMWA_COLOR_NONE);
        setDwmInt(hwnd, DWMWA_CAPTION_COLOR, CAPTION_DARK);
        setDwmInt(hwnd, DWMWA_TEXT_COLOR, TEXT_DARK);
    }

    private void setDwmInt(HWND hwnd, int attribute, int value) {
        try (Memory memory = new Memory(Integer.BYTES)) {
            memory.setInt(0, value);
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, attribute, memory, Integer.BYTES);
        } catch (RuntimeException | LinkageError ex) {
            System.err.println("DWM frame styling unavailable: " + ex.getMessage());
        }
    }

    private void scheduleRefresh() {
        if (refreshQueued) return;
        refreshQueued = true;
        Platform.runLater(() -> {
            refreshQueued = false;
            refreshHitZones();
        });
    }

    private void refreshHitZones() {
        if (!installed || hwnd == null || stage.getScene() == null) return;

        WinRect windowRect = new WinRect();
        if (!User32.INSTANCE.GetWindowRect(hwnd, windowRect)) return;
        WinPoint clientOrigin = new WinPoint();
        if (!User32.INSTANCE.ClientToScreen(hwnd, clientOrigin)) return;

        double scale = getDpiScale(hwnd);
        zones = new HitZones(
                new Rect(windowRect.left, windowRect.top, windowRect.right, windowRect.bottom),
                nodeRect(titleBar, clientOrigin, scale),
                nodeRect(minButton, clientOrigin, scale),
                nodeRect(maxButton, clientOrigin, scale),
                nodeRect(closeButton, clientOrigin, scale),
                (long) Math.ceil(8 * scale),
                stage.isResizable() && !stage.isMaximized()
        );
    }

    private Rect nodeRect(Node node, WinPoint clientOrigin, double scale) {
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        return new Rect(
                Math.round(clientOrigin.x + bounds.getMinX() * scale),
                Math.round(clientOrigin.y + bounds.getMinY() * scale),
                Math.round(clientOrigin.x + bounds.getMaxX() * scale),
                Math.round(clientOrigin.y + bounds.getMaxY() * scale)
        );
    }

    private long handleMessage(HWND callbackHwnd, int message, Pointer wParam, Pointer lParam) {
        if (message == WM_NCHITTEST) {
            long hit = hitTest(screenX(lParam), screenY(lParam));
            if (hit != HTCLIENT) {
                return hit;
            }
        } else if (message == WM_NCDESTROY) {
            long result = callOld(callbackHwnd, message, wParam, lParam);
            installed = false;
            hwnd = null;
            oldWndProc = null;
            return result;
        }

        return callOld(callbackHwnd, message, wParam, lParam);
    }

    private long hitTest(long x, long y) {
        HitZones current = zones;
        if (current.window.isEmpty()) return HTCLIENT;

        if (current.resizable) {
            long edge = current.resizeBorder;
            boolean left = x >= current.window.left && x < current.window.left + edge;
            boolean right = x <= current.window.right && x > current.window.right - edge;
            boolean top = y >= current.window.top && y < current.window.top + edge;
            boolean bottom = y <= current.window.bottom && y > current.window.bottom - edge;

            if (top && left) return HTTOPLEFT;
            if (top && right) return HTTOPRIGHT;
            if (bottom && left) return HTBOTTOMLEFT;
            if (bottom && right) return HTBOTTOMRIGHT;
            if (top) return HTTOP;
            if (bottom) return HTBOTTOM;
            if (left) return HTLEFT;
            if (right) return HTRIGHT;
        }

        boolean inCaption = current.titleBar.contains(x, y);
        if (!inCaption) return HTCLIENT;

        if (current.minButton.contains(x, y)
                || current.maxButton.contains(x, y)
                || current.closeButton.contains(x, y)) {
            return HTCLIENT;
        }
        return HTCAPTION;
    }

    private long callOld(HWND callbackHwnd, int message, Pointer wParam, Pointer lParam) {
        if (oldWndProc == null) return 0;
        return User32.INSTANCE.CallWindowProc(oldWndProc, callbackHwnd, message, wParam, lParam);
    }

    public boolean isNativeInstalled() {
        return installed && !fallbackInstalled;
    }

    private static long findNativeWindowHandle(Stage stage) throws ReflectiveOperationException {
        Class<?> windowClass = Class.forName("com.sun.glass.ui.Window");
        Method getWindows = windowClass.getDeclaredMethod("getWindowsClone");
        Method getNativeWindow = windowClass.getDeclaredMethod("getRawHandle");
        Method isVisible = windowClass.getDeclaredMethod("isVisible");
        Method getX = windowClass.getDeclaredMethod("getX");
        Method getY = windowClass.getDeclaredMethod("getY");
        Method getWidth = windowClass.getDeclaredMethod("getWidth");
        Method getHeight = windowClass.getDeclaredMethod("getHeight");

        @SuppressWarnings("unchecked")
        List<Object> windows = (List<Object>) getWindows.invoke(null);
        Object bestWindow = null;
        double bestScore = Double.MAX_VALUE;

        for (Object window : windows) {
            if (!Boolean.TRUE.equals(isVisible.invoke(window))) continue;
            long handle = ((Number) getNativeWindow.invoke(window)).longValue();
            if (handle == 0) continue;

            double score = Math.abs(((Number) getX.invoke(window)).doubleValue() - stage.getX())
                    + Math.abs(((Number) getY.invoke(window)).doubleValue() - stage.getY())
                    + Math.abs(((Number) getWidth.invoke(window)).doubleValue() - stage.getWidth())
                    + Math.abs(((Number) getHeight.invoke(window)).doubleValue() - stage.getHeight());
            if (score < bestScore) {
                bestScore = score;
                bestWindow = window;
            }
        }

        return bestWindow == null ? 0 : ((Number) getNativeWindow.invoke(bestWindow)).longValue();
    }

    private static double getDpiScale(HWND hwnd) {
        try {
            int dpi = User32.INSTANCE.GetDpiForWindow(hwnd);
            return dpi <= 0 ? 1.0 : dpi / 96.0;
        } catch (UnsatisfiedLinkError | RuntimeException ex) {
            return 1.0;
        }
    }

    private static int screenX(Pointer lParam) {
        long value = Pointer.nativeValue(lParam);
        return (short) (value & 0xffff);
    }

    private static int screenY(Pointer lParam) {
        long value = Pointer.nativeValue(lParam);
        return (short) ((value >> 16) & 0xffff);
    }

    private static int colorRef(int red, int green, int blue) {
        return (blue << 16) | (green << 8) | red;
    }

    private interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DwmSetWindowAttribute(HWND hwnd, int attribute, Pointer value, int size);
    }

    private interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

        Pointer SetWindowLongPtr(HWND hWnd, int nIndex, Pointer value);

        Pointer GetWindowLongPtr(HWND hWnd, int nIndex);

        boolean SetWindowPos(HWND hWnd, Pointer hWndInsertAfter, int x, int y, int cx, int cy, int flags);

        boolean ReleaseCapture();

        long SendMessage(HWND hWnd, int message, Pointer wParam, Pointer lParam);

        long CallWindowProc(Pointer previousWndProc, HWND hWnd, int message, Pointer wParam, Pointer lParam);

        boolean GetWindowRect(HWND hWnd, WinRect rect);

        boolean ClientToScreen(HWND hWnd, WinPoint point);

        int GetDpiForWindow(HWND hWnd);
    }

    private interface WindowProc extends StdCallLibrary.StdCallCallback {
        long callback(HWND hWnd, int message, Pointer wParam, Pointer lParam);
    }

    public static final class HWND extends PointerType {
        public HWND() {
        }

        public HWND(long pointer) {
            setPointer(Pointer.createConstant(pointer));
        }
    }

    @Structure.FieldOrder({"left", "top", "right", "bottom"})
    public static final class WinRect extends Structure {
        public int left;
        public int top;
        public int right;
        public int bottom;
    }

    @Structure.FieldOrder({"x", "y"})
    public static final class WinPoint extends Structure {
        public int x;
        public int y;
    }

    private record HitZones(
            Rect window,
            Rect titleBar,
            Rect minButton,
            Rect maxButton,
            Rect closeButton,
            long resizeBorder,
            boolean resizable
    ) {
        static HitZones empty() {
            Rect empty = Rect.empty();
            return new HitZones(empty, empty, empty, empty, empty, 0, false);
        }
    }

    private record Rect(long left, long top, long right, long bottom) {
        static Rect empty() {
            return new Rect(0, 0, 0, 0);
        }

        boolean isEmpty() {
            return right <= left || bottom <= top;
        }

        boolean contains(long x, long y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }
}
