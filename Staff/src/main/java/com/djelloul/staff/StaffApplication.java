package com.djelloul.staff;

import ch.qos.logback.classic.LoggerContext;

import com.djelloul.core.AppUtils;
import com.djelloul.core.ImageUtils;
import com.djelloul.core.SwingUtils;
import com.djelloul.core.common.Constants;
import com.djelloul.core.io.CopyTextRequest;
import com.djelloul.core.io.KeyPressRequest;
import com.djelloul.core.io.KeyReleaseRequest;
import com.djelloul.core.io.KeyTypeRequest;
import com.djelloul.core.io.MouseMoveRequest;
import com.djelloul.core.io.MousePressRequest;
import com.djelloul.core.io.MouseReleaseRequest;
import com.djelloul.core.io.MouseWheelRequest;
import com.djelloul.core.io.ScreenRequest;
import com.djelloul.core.io.ScreenResponse;
import com.djelloul.core.io.ScreenTile;
import com.djelloul.core.io.SocketHandler;
import com.djelloul.core.monitor.DataMonitor;
import com.djelloul.core.monitor.MonitoringInputStream;
import com.djelloul.core.monitor.MonitoringOutputStream;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.awt.Desktop;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Staff application.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public class StaffApplication {
    @SuppressWarnings("unused")
    private static Logger LOGGER;
    public final static String NAME;
    public final static String TITLE;
    public final static String VERSION;
    public final static File WORK_DIR;
    private static StaffOptions options = null;
    private static StaffFrame frame = null;
    private static Session tunnel = null;
    private static ServerSocket serverSocket = null;
    private static Handler handler = null;

    static {
        NAME = Constants.name;
        TITLE = Constants.title;
        VERSION = Constants.version;

        // get work directory
        // use the AppData folder on Windows systems, if available
        String appDataPath = (SystemUtils.IS_OS_WINDOWS) ? SystemUtils.getEnvironmentVariable("APPDATA", null) :
                null;
        WORK_DIR = (StringUtils.isNotBlank(appDataPath)) ? new File(appDataPath, NAME) :
                new File(SystemUtils.getUserHome(), "." + NAME);
        if (!WORK_DIR.isDirectory() && !WORK_DIR.mkdirs()) {
            System.err.println("Can't create work directory at: " + WORK_DIR.getAbsolutePath());
            System.exit(1);
        }
        System.setProperty("app.dir", WORK_DIR.getAbsolutePath());

        // init logging
        LOGGER = LoggerFactory.getLogger(StaffApplication.class);

        // enable debugging for SSL connections
        //System.setProperty("javax.net.debug", "ssl");

        // disable disk based caching for ImageIO
        ImageIO.setUseCache(false);
    }

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {
        LOGGER.info(StringUtils.repeat("-", 60));
        LOGGER.info("Starting " + TITLE + "...");
        LOGGER.info(StringUtils.repeat("-", 60));
        LOGGER.info("system  : " + SystemUtils.OS_NAME + " (" + SystemUtils.OS_VERSION + ")");
        LOGGER.info("runtime : " + SystemUtils.JAVA_RUNTIME_NAME + " (" + SystemUtils.JAVA_RUNTIME_VERSION + ")");
        LOGGER.info("time    : " + new Date());
        LOGGER.info(StringUtils.repeat("-", 60));

        // configure keystore for SSL connections
//        final File keystoreFile = new File(WORK_DIR, "keystore.jks");
//        final File keystorePassFile = new File(WORK_DIR, "keystore.jks.txt");
//        String keystorePassword = null;
//        if (!Constants.customKeyStore) {
//            LOGGER.info("loading internal keystore...");
//
//            // copy internal keystore into the work directory
//            // in order to make it usable with system properties
//            try (InputStream input = resource("keystore.jks").openStream()) {
//                FileUtils.copyToFile(input, keystoreFile);
//            } catch (IOException ex) {
//                LOGGER.warn("Can't copy internal keystore to work directory!", ex);
//            }
//
//            // read password of the internal keystore
//            try (InputStream input = resource("keystore.jks.txt").openStream()) {
//                keystorePassword = StringUtils.trimToEmpty(IOUtils.toString(input, "UTF-8"));
//            } catch (IOException ex) {
//                LOGGER.warn("Can't read internal keystore password!", ex);
//            }
//        } else {
//            LOGGER.info("loading external keystore...");
//
//            // copy internal keystore into the work directory,
//            // if it is not available yet
//            if (!keystoreFile.isFile()) {
//                try (InputStream input = resource("keystore.jks").openStream()) {
//                    FileUtils.copyToFile(input, keystoreFile);
//                } catch (IOException ex) {
//                    LOGGER.warn("Can't copy internal keystore to work directory!", ex);
//                }
//            }
//
//            // copy password of the internal keystore into the work directory,
//            // if it is not available yet
//            if (!keystorePassFile.isFile()) {
//                try (InputStream input = resource("keystore.jks.txt").openStream()) {
//                    FileUtils.copyToFile(input, keystorePassFile);
//                } catch (IOException ex) {
//                    LOGGER.warn("Can't copy internal keystore password to work directory!", ex);
//                }
//            }
//
//            // read password of the external keystore
//            try (InputStream input = new FileInputStream(keystorePassFile)) {
//                keystorePassword = StringUtils.trimToEmpty(IOUtils.toString(input, "UTF-8"));
//            } catch (IOException ex) {
//                LOGGER.warn("Can't read external keystore password!", ex);
//            }
//        }
//        AppUtils.initKeystore(keystoreFile, StringUtils.trimToEmpty(keystorePassword));

        // load options
        options = new StaffOptions(new File(WORK_DIR, "staff.properties"));
        try {
            options.read();
        } catch (IOException ex) {
            LOGGER.warn("Can't read staff options!", ex);
        }

        // setup look and feel
        SwingUtils.installLookAndFeel();

        // set application name for Gnome / Ubuntu
        if (SystemUtils.IS_OS_LINUX)
            SwingUtils.setAwtAppClassName(TITLE);

        // setup desktop environment
        //noinspection Duplicates
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            System.out.println();
            // register about dialog
//            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
//                desktop.setAboutHandler(e -> new AboutDialog().createAndShow());
//            }
        }

        // start application
        frame = new Frame(options);
        SwingUtilities.invokeLater(() -> frame.createAndShow());

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown " + TITLE + "...");

            // write options
            try {
                options.write();
            } catch (IOException ex) {
                LOGGER.warn("Can't write staff options!", ex);
            }

            // shutdown logger
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (loggerFactory instanceof LoggerContext) {
                LoggerContext context = (LoggerContext) loggerFactory;
                context.stop();
            }
        }));
    }

    public static URL resource(String file) {
        return StaffApplication.class.getResource("resources/" + file);
    }

    @SuppressWarnings("Duplicates")
    public static URL resourceBranding() {
        final Locale l = Locale.getDefault();

        URL branding = resource("branding_" + l.getLanguage() + "-" + l.getCountry() + ".png");
        if (branding == null) branding = resource("branding_" + l.getLanguage() + ".png");
        if (branding == null) branding = resource("branding.png");

        return branding;
    }

    @SuppressWarnings("ConstantConditions")
    private static void start() {
    	final String host = frame.getHost();
        final Integer port = frame.getPort();
        final boolean ssl = frame.isSsl();
        
        final File sslKeystore = AppUtils.getKeystore();
        final String sslKeystorePassword = AppUtils.getKeystorePassword();
        final boolean ssh = frame.isSsh();
        final Integer sshPort = frame.getSshPort();
        final Integer sshRemotePort = frame.getSshRemotePort();
        final String sshHost = frame.getSshHost();
        final String sshUser = frame.getSshUser();
        final boolean sshKeyAuth = frame.isSshKeyAuth();
        final File sshKey = frame.getSshKey();

        // validation
        List<String> errors = new ArrayList<>();
        if (port == null || port < 1 || port > 65535)
            errors.add("No valid local port number was specified.");
        if (ssl) {
            if (sslKeystore == null)
                errors.add("No keystore configured.");
            else if (!sslKeystore.isFile())
                errors.add("No keystore found at:" + "\n" + sslKeystore.getAbsolutePath());
            if (StringUtils.isBlank(sslKeystorePassword))
                errors.add("No keystore password configured.");
        }
        if (ssh) {
            if (sshHost.isEmpty())
                errors.add("No valid SSH hostname was specified.");
            if (sshPort == null || sshPort < 1 || sshPort > 65535)
                errors.add("No valid SSH port number was specified.");
            if (sshRemotePort == null || sshRemotePort < 1 || sshRemotePort > 65535)
                errors.add("No valid remote port number was specified.");
            if (sshUser.isEmpty())
                errors.add("No valid SSH username was specified.");
            if (sshKeyAuth && (sshKey == null || !sshKey.isFile()))
                errors.add("No valid SSH key was selected.");
        }

        //noinspection Duplicates
        if (!errors.isEmpty()) {
            StringBuilder msg = new StringBuilder("Your settings are invalid.");
            msg.append("\n\n");
            for (String error : errors)
                msg.append("- ").append(error).append("\n");

            AppUtils.showError(frame, msg.toString(), "Error");
            return;
        }

        frame.setStarted(true);

        new Thread(() -> {

            if (ssh) {
                try {
                    LOGGER.info("Creating ssh tunnel "
                            + sshHost + ":" + sshRemotePort + " -> "
                            + "localhost:" + port + "...");

                    JSch jsch = new JSch();
                    jsch.setKnownHosts(new File(WORK_DIR, "known_hosts.txt").getAbsolutePath());
                    if (sshKeyAuth && sshKey.isFile()) jsch.addIdentity(sshKey.getAbsolutePath());

                    tunnel = jsch.getSession(sshUser, sshHost, sshPort);

                    // setup host key checking
                    if (Constants.sshHostKeyCheck)
                        tunnel.setConfig("StrictHostKeyChecking", "ask");
                    else
                        tunnel.setConfig("StrictHostKeyChecking", "no");

                    // setup packet compression
                    tunnel.setConfig("compression.s2c", "zlib,none");
                    tunnel.setConfig("compression.c2s", "zlib,none");
                    tunnel.setConfig("compression_level", "9");

                    // setup connection info
                    tunnel.setUserInfo(new UserInfo() {
                        private String passphrasePrompt = null;
                        private String passwordPrompt = null;

                        @Override
                        @SuppressWarnings("Duplicates")
                        public String getPassphrase() {
                            String message = StringEscapeUtils.escapeXml11("Enter your SSH passphrase.");
                            if (StringUtils.isNotBlank(passphrasePrompt))
                                message += "\n(" + StringEscapeUtils.escapeXml11(passphrasePrompt) + ")";

                            JTextPane text = new JTextPane();
                            text.setOpaque(false);
                            text.setEditable(false);
                            text.setText(message);

                            return AppUtils.askForPassword(frame, text, "SSH authentication", "Submit", "Cancel");
                        }

                        @Override
                        @SuppressWarnings("Duplicates")
                        public String getPassword() {
                            String message = StringEscapeUtils.escapeXml11("Enter your SSH password.");
                            if (StringUtils.isNotBlank(passwordPrompt))
                                message += "\n(" + StringEscapeUtils.escapeXml11(passwordPrompt) + ")";

                            JTextPane text = new JTextPane();
                            text.setOpaque(false);
                            text.setEditable(false);
                            text.setText(message);

                            return AppUtils.askForPassword(frame, text, "SSH authentication", "Submit", "Cancel");
                        }

                        @Override
                        public boolean promptPassphrase(String message) {
                            passphrasePrompt = StringUtils.trimToNull(message);
                            return sshKeyAuth;
                        }

                        @Override
                        public boolean promptPassword(String message) {
                            passwordPrompt = StringUtils.trimToNull(message);
                            return !sshKeyAuth;
                        }

                        @Override
                        public boolean promptYesNo(String message) {
                            String text = "Question about SSH connection:" + "\n\n" + message;
                            Boolean answer = AppUtils.askQuestion(frame, text, "Question");
                            return Boolean.TRUE.equals(answer);
                        }

                        @Override
                        public void showMessage(String message) {
                            String text = "Information about SSH connection:" + "\n\n" + message;
                            AppUtils.showInformation(frame, text, "Information");
                        }
                    });
                    tunnel.connect();
                    tunnel.setPortForwardingR(sshRemotePort, "localhost", port);
                } catch (JSchException ex) {
                    LOGGER.error("Can't open ssh tunnel!", ex);
                    stop(true);
                    String message = "Can't setup SSH tunnel!" + "\n" + ex.getLocalizedMessage();
                    AppUtils.showError(frame, message, "Error");
                    return;
                }
            }

            try {
                LOGGER.info("Creating server socket at port " + port + "...");
                //noinspection ConstantConditions
                serverSocket = (ssl) ?
                        SSLServerSocketFactory.getDefault().createServerSocket(port) :
                        ServerSocketFactory.getDefault().createServerSocket(port);
            } catch (IOException ex) {
                LOGGER.error("Can't open server socket at localPort " + port + "!", ex);
                stop(true);
                String message = "Can't setup server socket!" + "\n" + ex.getLocalizedMessage();
                AppUtils.showError(frame, message, "Error");
                return;
            }

            try {
            	if (!ssh) {
            		LOGGER.info("Waiting for connections at localhost:" + port + "...");
            		frame.setInfo("Listening at" + " localhost:" + port + "...");
            	} else {
            		LOGGER.info("Waiting for connections at " + sshHost + ":" + sshRemotePort + "...");
            		frame.setInfo("Listening at" + " " + sshHost + ":" + sshRemotePort + "...");
            	}
                //noinspection ConstantConditions
                Socket socket = (ssl) ?
                        SSLSocketFactory.getDefault().createSocket(host, port) :
                        SocketFactory.getDefault().createSocket(host, port);

                handler = new Handler(socket);
                handler.start();
                frame.setStatusConnected();
            } catch (IOException ex) {
                LOGGER.error("Connection to " + host + ":" + port + " failed!", ex);
                stop(true);
                String message = "Can't establish connection!" + "\n" + ex.getLocalizedMessage();
                AppUtils.showError(frame, message, "Error");
            }
            
            LOGGER.info("Sending first screen request...");
            handler.sendScreenRequest();
        }).start();
    }

    private static void stop(boolean stopHandler) {
        if (handler != null) {
            if (stopHandler) handler.stop();
            handler = null;
        }
        if (serverSocket != null) {
            try {
                if (!serverSocket.isClosed()) serverSocket.close();
            } catch (IOException ex) {
                LOGGER.error("Can't close server socket!", ex);
            }
            serverSocket = null;
        }
        if (tunnel != null) {
            if (tunnel.isConnected())
                tunnel.disconnect();
            tunnel = null;
        }

        frame.setScreenDisabled();
        frame.setStarted(false);
        frame.setInfo("Connection was closed.");
    }

    private static class Frame extends StaffFrame {
        private Timer mouseMotionTimer = null;
        private MouseEvent mouseMotionEvent = null;
        private Timer resizeTimer = null;
        private boolean windowsKeyDown = false;
        private List<Integer> pressedKeys = new ArrayList<>();

        private Frame(StaffOptions options) {
            super(options);
        }

        @Override
        protected void doCopyText(String text) {
            if (handler == null) return;
            handler.sendCopyText(text);
        }

        @Override
        @SuppressWarnings("Duplicates")
        protected synchronized void doHandleKeyPress(KeyEvent e) {
            if (handler == null) return;
            //LOGGER.debug("key pressed: " + e.paramString());

            // Get code of the pressed key.
            // Keypad arrows are translated to regular arrow keys.
            final int keyCode;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_KP_DOWN:
                    keyCode = KeyEvent.VK_DOWN;
                    break;
                case KeyEvent.VK_KP_LEFT:
                    keyCode = KeyEvent.VK_LEFT;
                    break;
                case KeyEvent.VK_KP_RIGHT:
                    keyCode = KeyEvent.VK_RIGHT;
                    break;
                case KeyEvent.VK_KP_UP:
                    keyCode = KeyEvent.VK_UP;
                    break;
                default:
                    keyCode = e.getKeyCode();
                    break;
            }

            // Never press undefined key codes.
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                return;
            }

            // Never send caps lock, num lock and scroll lock key.
            if (keyCode == KeyEvent.VK_CAPS_LOCK || keyCode == KeyEvent.VK_NUM_LOCK || keyCode == KeyEvent.VK_SCROLL_LOCK) {
                return;
            }

            // Detect, if a control key was pressed.
            final boolean isControlKey = e.isActionKey() ||
                    keyCode == KeyEvent.VK_BACK_SPACE ||
                    keyCode == KeyEvent.VK_DELETE ||
                    keyCode == KeyEvent.VK_ENTER ||
                    keyCode == KeyEvent.VK_SPACE ||
                    keyCode == KeyEvent.VK_TAB ||
                    keyCode == KeyEvent.VK_ESCAPE ||
                    keyCode == KeyEvent.VK_ALT ||
                    keyCode == KeyEvent.VK_ALT_GRAPH ||
                    keyCode == KeyEvent.VK_CONTROL ||
                    keyCode == KeyEvent.VK_SHIFT ||
                    keyCode == KeyEvent.VK_META;

            // Press control keys.
            if (isControlKey) {
                //LOGGER.debug("press key \"{}\" ({})", keyCode, KeyEvent.getKeyText(keyCode));
                handler.sendKeyPress(keyCode);
                e.consume();
            }

            // Press other keys, if they are pressed together with a modifier key.
            else if (e.isControlDown() || e.isMetaDown() || windowsKeyDown || (!SystemUtils.IS_OS_MAC && e.isAltDown())) {
                //LOGGER.debug("press key \"{}\" ({})", keyCode, KeyEvent.getKeyText(keyCode));
                handler.sendKeyPress(keyCode);
                if (!pressedKeys.contains(keyCode))
                    pressedKeys.add(keyCode);
                e.consume();
            }

            // Remember, that the Windows key was pressed.
            if (keyCode == KeyEvent.VK_WINDOWS) {
                synchronized (Frame.this) {
                    windowsKeyDown = true;
                }
            }
        }

        @Override
        @SuppressWarnings("Duplicates")
        protected synchronized void doHandleKeyRelease(KeyEvent e) {
            if (handler == null) return;
            //LOGGER.debug("key released: " + e.paramString());

            // Get code of the released key.
            // Keypad arrows are translated to regular arrow keys.
            final int keyCode;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_KP_DOWN:
                    keyCode = KeyEvent.VK_DOWN;
                    break;
                case KeyEvent.VK_KP_LEFT:
                    keyCode = KeyEvent.VK_LEFT;
                    break;
                case KeyEvent.VK_KP_RIGHT:
                    keyCode = KeyEvent.VK_RIGHT;
                    break;
                case KeyEvent.VK_KP_UP:
                    keyCode = KeyEvent.VK_UP;
                    break;
                default:
                    keyCode = e.getKeyCode();
                    break;
            }

            // Never press undefined key codes.
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                return;
            }

            // Never send caps lock, num lock and scroll lock key.
            if (keyCode == KeyEvent.VK_CAPS_LOCK || keyCode == KeyEvent.VK_NUM_LOCK || keyCode == KeyEvent.VK_SCROLL_LOCK) {
                return;
            }

            // Detect, if a control key was pressed.
            final boolean isControlKey = e.isActionKey() ||
                    keyCode == KeyEvent.VK_BACK_SPACE ||
                    keyCode == KeyEvent.VK_DELETE ||
                    keyCode == KeyEvent.VK_ENTER ||
                    keyCode == KeyEvent.VK_SPACE ||
                    keyCode == KeyEvent.VK_TAB ||
                    keyCode == KeyEvent.VK_ESCAPE ||
                    keyCode == KeyEvent.VK_ALT ||
                    keyCode == KeyEvent.VK_ALT_GRAPH ||
                    keyCode == KeyEvent.VK_CONTROL ||
                    keyCode == KeyEvent.VK_SHIFT ||
                    keyCode == KeyEvent.VK_META;

            // Release control keys.
            if (isControlKey) {
                //LOGGER.debug("release key \"{}\" ({})", keyCode, KeyEvent.getKeyText(keyCode));
                handler.sendKeyRelease(keyCode);
                e.consume();
            }

            // Release other keys, if they are pressed together with a modifier key.
            else if (e.isControlDown() || e.isMetaDown() || windowsKeyDown || (!SystemUtils.IS_OS_MAC && e.isAltDown()) || pressedKeys.contains(keyCode)) {
                //LOGGER.debug("release key \"{}\" ({})", keyCode, KeyEvent.getKeyText(keyCode));
                handler.sendKeyRelease(keyCode);
                pressedKeys.remove((Integer) keyCode);
                e.consume();
            }

            // Forget, that the Windows key is pressed.
            if (keyCode == KeyEvent.VK_WINDOWS) {
                synchronized (Frame.this) {
                    windowsKeyDown = false;
                }
            }
        }

        @Override
        protected synchronized void doHandleKeyTyped(KeyEvent e) {
            if (handler == null) return;
            //LOGGER.debug("key typed: " + e.paramString());
            final char keyChar = e.getKeyChar();

            // Don't type non printable characters.
            if (keyChar == KeyEvent.CHAR_UNDEFINED || Character.isWhitespace(keyChar) || Character.isISOControl(keyChar) || Character.isIdentifierIgnorable(keyChar)) {
                //LOGGER.debug("non printable {} / {}", Character.isWhitespace(keyChar), Character.isISOControl(keyChar));
                return;
            }

            // Don't type a character, if a modifier key is pressed at the same time.
            if (e.isControlDown() || e.isMetaDown() || windowsKeyDown || (!SystemUtils.IS_OS_MAC && e.isAltDown())) {
                //LOGGER.debug("modifier {} / {} / {} / {}", e.isControlDown(), e.isAltDown(), e.isMetaDown(), windowsKeyDown);
                return;
            }

            //LOGGER.debug("type character \"{}\" ({})", keyChar, e.getKeyCode());
            handler.sendKeyType(keyChar);
            e.consume();
        }

        @Override
        protected void doHandleMouseMotion(MouseEvent e) {
            if (handler == null) return;
            //LOGGER.debug("mouse moved: " + e.paramString());

            mouseMotionEvent = e;
            if (mouseMotionTimer != null) {
                return;
            }
            mouseMotionTimer = new Timer(100, e1 -> {
                if (handler != null && mouseMotionEvent != null)
                    handler.sendMouseMove(mouseMotionEvent.getX(), mouseMotionEvent.getY());
                mouseMotionEvent = null;
                mouseMotionTimer = null;
            });
            mouseMotionTimer.setRepeats(false);
            mouseMotionTimer.start();
        }

        @Override
        protected void doHandleMousePress(MouseEvent e) {
            if (handler == null) return;
            //LOGGER.debug("mouse pressed: " + e.paramString());
            handler.sendMousePress(
                    InputEvent.getMaskForButton(e.getButton()));
        }

        @Override
        protected void doHandleMouseRelease(MouseEvent e) {
            if (handler == null) return;
            //LOGGER.debug("mouse released: " + e.paramString());
            handler.sendMouseRelease(
                    InputEvent.getMaskForButton(e.getButton()));
        }

        @Override
        protected void doHandleMouseWheel(MouseWheelEvent e) {
            if (handler == null) return;
            //LOGGER.debug("mouse wheel moved: " + e.paramString());
            handler.sendMouseWheel(
                    e.getScrollAmount() * e.getWheelRotation());
        }

        @Override
        protected void doQuit() {
            stop(true);
            System.exit(0);
        }

        @Override
        protected void doResize() {
            if (handler == null) return;
            //LOGGER.debug("screen resized");

            if (resizeTimer != null) return;
            resizeTimer = new Timer(500, event -> {
                if (handler != null) handler.sendScreenRequest();
                resizeTimer = null;
            });
            resizeTimer.setRepeats(false);
            resizeTimer.start();
        }

        @Override
        protected void doStart() {
            pressedKeys.clear();
            windowsKeyDown = false;
            start();
        }

        @Override
        protected void doStop() {
            stop(true);
        }
    }

    private static class Handler extends SocketHandler {
        private int serverScreenWidth = 0;
        private int serverScreenHeight = 0;
        private Timer monitoringTimer = null;
        private DataMonitor downloadMonitor = null;
        private DataMonitor uploadMonitor = null;

        private Handler(Socket socket) {
            super(socket);
        }

        @Override
        protected ObjectInputStream createObjectInputStream(InputStream input) throws IOException {
            downloadMonitor = new DataMonitor();
            return super.createObjectInputStream(
                    new MonitoringInputStream(input, downloadMonitor));
        }

        @Override
        protected ObjectOutputStream createObjectOutputStream(OutputStream output) throws IOException {
            uploadMonitor = new DataMonitor();
            return super.createObjectOutputStream(
                    new MonitoringOutputStream(output, uploadMonitor));
        }

        @Override
        public void processReceivedObject(Serializable object) {
            if (object instanceof ScreenResponse) {
                //LOGGER.debug("RECEIVE SCREEN RESPONSE");

                final ScreenResponse response = (ScreenResponse) object;
                serverScreenWidth = response.screenWidth;
                serverScreenHeight = response.screenHeight;

                //int byteCount = 0;
                //int sliceCount = 0;
                List<BufferedImage> slices = new ArrayList<>();
                for (ScreenTile tile : response.tiles) {
                    if (tile == null) {
                        slices.add(null);
                        continue;
                    }
                    //byteCount += tile.data.length;
                    //sliceCount++;
                    //LOGGER.debug("received slice (" + tile.data.length + " bytes)");
                    try (InputStream input = new ByteArrayInputStream(tile.data)) {
                        BufferedImage slice = ImageUtils.read(input);
                        if (slice == null) {
                            LOGGER.warn("Can't read tile!");
                            slices.add(null);
                        } else {
                            slices.add(slice);
                        }

                    } catch (Exception ex) {
                        LOGGER.warn("Can't read tile!", ex);
                        slices.add(null);
                    }
                }

                //float bytesPerSlice = (float) byteCount / (float) sliceCount;
                //LOGGER.debug("update screen ("
                //        + sliceCount + " slices, "
                //        + byteCount + " bytes, "
                //        + NumberFormat.getIntegerInstance().format(bytesPerSlice) + " bytes per slice)"
                //);
                frame.updateScreen(
                        slices,
                        response.imageWidth,
                        response.imageHeight,
                        response.tileWidth,
                        response.tileHeight
                );
            } else {
                LOGGER.warn("Received an unsupported object (" + object.getClass().getName() + ")!");
            }
        }

        private void sendCopyText(String text) {
            send(new CopyTextRequest(text));
        }

        private void sendKeyPress(int keyCode) {
            //LOGGER.debug("sendKeyPress | code: " + keyCode);
            send(new KeyPressRequest(keyCode));
        }

        private void sendKeyRelease(int keyCode) {
            //LOGGER.debug("sendKeyRelease | code: " + keyCode);
            send(new KeyReleaseRequest(keyCode));
        }

        private void sendKeyType(char keyChar) {
            //LOGGER.debug("sendKeyType | char: " + keyChar);
            send(new KeyTypeRequest(keyChar));
        }

        private void sendMouseMove(int x, int y) {
            final int viewWidth = frame.getScreenWidth();
            final int viewHeight = frame.getScreenHeight();
            final int imageWidth = frame.getScreenImageWidth();
            final int imageHeight = frame.getScreenImageHeight();
            final int offsetLeft = (viewWidth > imageWidth) ?
                    (int) (((double) (viewWidth - imageWidth)) / 2d) :
                    0;
            final int offsetTop = (viewHeight > imageHeight) ?
                    (int) (((double) (viewHeight - imageHeight)) / 2d) :
                    0;

            if (x < offsetLeft || y < offsetTop) return;

            x -= offsetLeft;
            y -= offsetTop;

            if (x > imageWidth || y > imageHeight) return;

            final double scaleX = (double) serverScreenWidth / (double) imageWidth;
            final double scaleY = (double) serverScreenHeight / (double) imageHeight;

            x = (int) (scaleX * ((double) x));
            y = (int) (scaleY * ((double) y));

            //LOGGER.debug("mouse move    : " + x + " / " + y);
            //LOGGER.debug("> server size : " + serverScreenWidth + " / " + serverScreenHeight);
            //LOGGER.debug("> view size   : " + viewWidth + " / " + viewHeight);
            //LOGGER.debug("> image size  : " + viewWidth + " / " + viewHeight);
            //LOGGER.debug("> offset      : " + offsetLeft + " / " + offsetTop);
            //LOGGER.debug("> coordinates : " + x + " / " + y);

            send(new MouseMoveRequest(x, y));
        }

        private void sendMousePress(int buttons) {
            send(new MousePressRequest(buttons));
        }

        private void sendMouseRelease(int buttons) {
            send(new MouseReleaseRequest(buttons));
        }

        private void sendMouseWheel(int wheelAmt) {
            send(new MouseWheelRequest(wheelAmt));
        }

        private void sendScreenRequest() {
            //LOGGER.debug("SEND SCREEN REQUEST");
            send(new ScreenRequest(
                    frame.getScreenWidth(),
                    frame.getScreenHeight()
            ));
        }

        @Override
        public void start() {
            monitoringTimer = new Timer(1000, e -> {
                if (uploadMonitor == null || downloadMonitor == null)
                    return;
                try {
                    frame.setRates(downloadMonitor.getAverageRate(), uploadMonitor.getAverageRate());

                    Date minAge = new Date(System.currentTimeMillis() - 2000);
                    downloadMonitor.removeOldSamples(minAge);
                    uploadMonitor.removeOldSamples(minAge);
                } catch (Exception ex) {
                    LOGGER.warn("Can't upload monitoring!", ex);
                }
            });
            monitoringTimer.setRepeats(true);
            monitoringTimer.start();

            super.start();
        }

        @Override
        public void stop() {
            super.stop();

            if (monitoringTimer != null) {
                monitoringTimer.stop();
                monitoringTimer = null;
            }

            StaffApplication.stop(false);
        }
    }
}
