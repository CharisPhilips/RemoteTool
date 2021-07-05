/*
 * Copyright 2015-2019 OpenIndex.de.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.djelloul.customer;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.djelloul.core.io.ResponseFactory;
import com.djelloul.core.io.ScreenRequest;
import com.djelloul.core.io.ScreenResponse;
import com.djelloul.core.io.ScreenTile;
import com.djelloul.core.io.SocketHandler;
import com.djelloul.customer.utils.Robot;

import ch.qos.logback.classic.LoggerContext;

/**
 * Customer application.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public class CustomerApplication {
    @SuppressWarnings("unused")
    private static Logger LOGGER;
//    public final static ResourceBundle SETTINGS;
    public final static String NAME;
    public final static String TITLE;
    public final static String VERSION;
    public final static File WORK_DIR;
    private final static float JPEG_COMPRESSION = 0.6f;
    private final static int SLICE_WIDTH = 100;
    private final static int SLICE_HEIGHT = 100;
    private final static int SCREENSHOT_DELAY = 250;
    private static CustomerOptions options = null;
    private static CustomerFrame frame = null;
    private static Handler handler = null;
    private static Robot robot = null;
    private static GraphicsDevice screen = null;
    private static Timer screenshotTimer = null;
    
    static {
        NAME = Constants.name;
        TITLE = Constants.title;
        VERSION = Constants.version;

        // get work directory
        // use the AppData folder on Windows systems, if available
        String appDataPath = (SystemUtils.IS_OS_WINDOWS) ?
                SystemUtils.getEnvironmentVariable("APPDATA", null) :
                null;
        WORK_DIR = (StringUtils.isNotBlank(appDataPath)) ?
                new File(appDataPath, NAME) :
                new File(SystemUtils.getUserHome(), "." + NAME);
        if (!WORK_DIR.isDirectory() && !WORK_DIR.mkdirs()) {
            System.err.println("Can't create work directory at: " + WORK_DIR.getAbsolutePath());
            System.exit(1);
        }
        System.setProperty("app.dir", WORK_DIR.getAbsolutePath());

        // init logging
        LOGGER = LoggerFactory.getLogger(CustomerApplication.class);

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

        // configure truststore for SSL connections
        final File truststoreFile = new File(WORK_DIR, "truststore.jks");
        final File truststorePassFile = new File(WORK_DIR, "truststore.jks.txt");
        String truststorePassword = null;
        if (!Constants.customTrustStore) {
            LOGGER.info("loading internal truststore...");

            // copy internal truststore into the work directory
            // in order to make it usable with system properties
            try (InputStream input = resource("truststore.jks").openStream()) {
                FileUtils.copyToFile(input, truststoreFile);
            } catch (IOException ex) {
                LOGGER.warn("Can't copy internal truststore to work directory!", ex);
            }

            // read password of the internal truststore
            try (InputStream input = resource("truststore.jks.txt").openStream()) {
                truststorePassword = StringUtils.trimToEmpty(IOUtils.toString(input, "UTF-8"));
            } catch (IOException ex) {
                LOGGER.warn("Can't read internal truststore password!", ex);
            }
        } else {
            LOGGER.info("loading external truststore...");

            // copy internal truststore into the work directory,
            // if it is not available yet
//            if (!truststoreFile.isFile()) {
//                try (InputStream input = resource("truststore.jks").openStream()) {
//                    FileUtils.copyToFile(input, truststoreFile);
//                } catch (IOException ex) {
//                    LOGGER.warn("Can't copy internal truststore to work directory!", ex);
//                }
//            }
//
//            // copy password of the internal truststore into the work directory,
//            // if it is not available yet
//            if (!truststorePassFile.isFile()) {
//                try (InputStream input = resource("truststore.jks.txt").openStream()) {
//                    FileUtils.copyToFile(input, truststorePassFile);
//                } catch (IOException ex) {
//                    LOGGER.warn("Can't copy internal truststore password to work directory!", ex);
//                }
//            }
//
//            // read password of the external truststore
//            try (InputStream input = new FileInputStream(new File(WORK_DIR, "truststore.jks.txt"))) {
//                truststorePassword = StringUtils.trimToEmpty(IOUtils.toString(input, "UTF-8"));
//            } catch (IOException ex) {
//                LOGGER.warn("Can't read external truststore password!", ex);
//            }
        }
//        AppUtils.initTruststore(truststoreFile, StringUtils.trimToEmpty(truststorePassword));

        // load options
        options = new CustomerOptions(new File(WORK_DIR, "customer.properties"));
        try {
            options.read();
        } catch (IOException ex) {
            LOGGER.warn("Can't read customer options!", ex);
        }

        if (Constants.isShowCustom) {
        	// setup look and feel
        	SwingUtils.installLookAndFeel();
        	
        	// set application name for Gnome / Ubuntu
        	if (SystemUtils.IS_OS_LINUX)
        		SwingUtils.setAwtAppClassName(TITLE);
        	
        	// start application
        	frame = new Frame(options);
        	SwingUtilities.invokeLater(() -> frame.createAndShow());
        } else {
        	start();
        }

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown " + TITLE + "...");

            // write options
            try {
                options.write();
            } catch (IOException ex) {
                LOGGER.warn("Can't write customer options!", ex);
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
        return CustomerApplication.class.getResource("resources/" + file);
    }

    @SuppressWarnings("Duplicates")
    public static URL resourceBranding() {
        final Locale l = Locale.getDefault();

        URL branding = resource("branding_" + l.getLanguage() + "-" + l.getCountry() + ".png");
        if (branding == null) branding = resource("branding_" + l.getLanguage() + ".png");
        if (branding == null) branding = resource("branding.png");

        return branding;
    }

    private static void start() {
    	Integer port = Constants.default_localPort;
    	boolean ssl = Constants.default_ssl;
        
        if (Constants.isShowCustom) {
        	port = frame.getPort(); //options.getPort()
        	ssl = frame.isSsl(); //options.getSsl()
        	screen = frame.getScreen();
        	
        } else {
        	port = options.getPort();
        	ssl = options.getSsl();
        	String selectedScreenId = options.getScreenId();
        	for (GraphicsDevice scr : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
        		if (selectedScreenId != null && scr.getIDstring().equalsIgnoreCase(selectedScreenId)) {
        			screen = scr;
        			break;
        		} else if(selectedScreenId == null){
        			screen = scr;
        			break;
        		}
        	}
        }

        List<String> errors = new ArrayList<>();
        if (port == null || port < 1 || port > 65535)
        	errors.add("No valid port number was specified.");
        if (screen == null)
        	errors.add("No screen was selected.");
        
        if (!errors.isEmpty()) {
        	StringBuilder msg = new StringBuilder("Your settings are invalid.");
        	msg.append("\n\n");
        	for (String error : errors)
        		msg.append("- ").append(error).append("\n");
        	
        	AppUtils.showError(frame, msg.toString(), "Error");
        	return;
        }
        
        if (Constants.isShowCustom) {
	
	       frame.setStatusConnecting();
	       frame.setStarted(true);
	       
        } else {
        }
        
        try {
        	robot = new Robot(screen);
        } catch (AWTException ex) {
        	LOGGER.error("Can't create robot!", ex);
        	stop(true);
        }
        
        final int portVal = port;
        final boolean sslVal = ssl;
        
        new Thread(() -> {
        	ServerSocket serverSocket = null;
        	Socket socket = null;
        	do {
        		if (serverSocket != null && !serverSocket.isClosed() && (socket != null && (!socket.isClosed() && socket.isConnected()))) {
            		try {
            			System.out.println("Connection Status....");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
            		continue;
            	} else {
            		if (serverSocket != null && serverSocket.isClosed() || (socket != null && !(!socket.isClosed() && socket.isConnected()))) {
            			System.out.println("Connection Closing Status....");
            			if (serverSocket != null && !serverSocket.isClosed()) {
            				try {
            					serverSocket.close();
            				} catch (IOException e) {
            					e.printStackTrace();
            				}
            			}
            			if (socket != null && (!socket.isClosed() && socket.isConnected())) {
            				try {
            					socket.close();
            				} catch (IOException e) {
            					e.printStackTrace();
            				}
            			}
            		} else {
            			System.out.println("Connection Closed Status....");
            		}
            	}
            	
        		
        		try {
        			LOGGER.info("Creating server socket at port " + portVal + "...");
        			serverSocket = (sslVal) ?
        					SSLServerSocketFactory.getDefault().createServerSocket(portVal) :
        						ServerSocketFactory.getDefault().createServerSocket(portVal);
        		} catch (IOException ex) {
        			LOGGER.error("Can't open server socket at localPort " + String.valueOf(portVal) + "!", ex);
        			stop(true);
        			String message = "Can't setup server socket!" + "\n" + ex.getLocalizedMessage();
        			if (Constants.isShowCustom) {
        				AppUtils.showError(frame, message, "Error");
        			}
        			return;
        		}
        		
        		try {
        			LOGGER.info("Waiting for connections at localhost:" + String.valueOf(portVal) + "...");
        			socket = serverSocket.accept();
        			handler = new Handler(socket);
        			handler.start();
        			 if (Constants.isShowCustom) {
        				 frame.setStatusConnected();
        			 }
        			
        		} catch (IOException ex) {
        			LOGGER.error("Can't initiate communication!", ex);
        			stop(true);
        			String message = "Can't establish connection!" + "\n" + ex.getLocalizedMessage();
        			if (Constants.isShowCustom) {
        				AppUtils.showError(frame, message, "Error");
        			}
        		}
        	} while((Constants.isShowCustom && frame.isStarted()) || (!Constants.isShowCustom));
        }).start();
        
    }

    private static void stop(boolean stopHandler) {
        if (handler != null) {
            if (stopHandler) handler.stop();
            handler = null;
        }
        if (screenshotTimer != null) {
            screenshotTimer.stop();
            screenshotTimer = null;
        }

        robot = null;
        screen = null;
        frame.setStarted(false);
        frame.setStatusDisconnected();
    }

    private static class Frame extends CustomerFrame {
        private Frame(CustomerOptions options) {
            super(options);
        }

        @Override
        protected void doQuit() {
            stop(true);
            System.exit(0);
        }

        @Override
        protected void doStart() {
            start();
        }

        @Override
        protected void doStop() {
            stop(true);
        }
    }

    private static class Handler extends SocketHandler {

        private Handler(Socket socket) {
            super(socket);
        }

        @Override
        public void processReceivedObject(Serializable object) {
            try {
                if (object instanceof ScreenRequest) {

                    final ScreenRequest request = (ScreenRequest) object;

                    if (screenshotTimer != null) {
                        screenshotTimer.stop();
                        screenshotTimer = null;
                    }

                    //LOGGER.debug("create screenshot for {} x {}", request.maxWidth, request.maxHeight);
                    screenshotTimer = new Timer(SCREENSHOT_DELAY, new ScreenShooter(request));
                    screenshotTimer.setRepeats(true);
                    screenshotTimer.start();

                } else if (object instanceof KeyPressRequest) {

                    final KeyPressRequest request = (KeyPressRequest) object;
                    //LOGGER.debug("press key {} ({})", request.keyCode, KeyEvent.getKeyText(request.keyCode));

                    if (request.keyCode != KeyEvent.VK_UNDEFINED)
                        robot.keyPress(request.keyCode);

                } else if (object instanceof KeyReleaseRequest) {

                    final KeyReleaseRequest request = (KeyReleaseRequest) object;
                    //LOGGER.debug("release key {} ({})", request.keyCode, KeyEvent.getKeyText(request.keyCode));

                    if (request.keyCode != KeyEvent.VK_UNDEFINED)
                        robot.keyRelease(request.keyCode);

                } else if (object instanceof KeyTypeRequest) {

                    final KeyTypeRequest request = (KeyTypeRequest) object;
                    //LOGGER.debug("type key \"{}\"", request.keyChar);

                    if (request.keyChar != KeyEvent.CHAR_UNDEFINED)
                        robot.printCharacter(request.keyChar);

                } else if (object instanceof MouseMoveRequest) {

                    final MouseMoveRequest request = (MouseMoveRequest) object;
                    final GraphicsConfiguration screenConfiguration = screen.getDefaultConfiguration();

                    int x;
                    int y;

                    // On Windows systems we need to convert the coordinates
                    // according to the current screen scaling factor.
                    if (SystemUtils.IS_OS_WINDOWS) {
                        final AffineTransform transform = screenConfiguration.getDefaultTransform();
                        final double scaleX = (transform != null && transform.getScaleX() > 0) ?
                                transform.getScaleX() : 1;
                        final double scaleY = (transform != null && transform.getScaleY() > 0) ?
                                transform.getScaleY() : 1;

                        x = (int) ((double) request.x / scaleX);
                        y = (int) ((double) request.y / scaleY);
                    } else {
                        x = request.x;
                        y = request.y;
                    }

                    // Calculate absolute coordinates for the selected screen.
                    // Required for multi monitor setups.
                    Rectangle bounds = screenConfiguration.getBounds();
                    //LOGGER.debug("screen bounds {} x {}", bounds.x, bounds.y);
                    //LOGGER.debug("screen size   {} x {}", bounds.width, bounds.height);
                    //LOGGER.debug("mouse coords  {} x {}", x, y);
                    x += bounds.x;
                    y += bounds.y;
                    //LOGGER.debug("move mouse to {} x {}", x, y);

                    robot.mouseMove(x, y);

                } else if (object instanceof MousePressRequest) {

                    final MousePressRequest request = (MousePressRequest) object;
                    robot.mousePress(request.buttons);

                } else if (object instanceof MouseReleaseRequest) {

                    final MouseReleaseRequest request = (MouseReleaseRequest) object;
                    robot.mouseRelease(request.buttons);

                } else if (object instanceof MouseWheelRequest) {

                    final MouseWheelRequest request = (MouseWheelRequest) object;
                    robot.mouseWheel(request.wheelAmt);

                } else if (object instanceof CopyTextRequest) {

                    final CopyTextRequest request = (CopyTextRequest) object;
                    robot.copyText(request.text);

                } else {

                    LOGGER.warn("Received an unsupported object (" + object.getClass().getName() + ")!");

                }
            } catch (Exception ex) {
                LOGGER.error("Can't process received object!", ex);
            }
        }

        @Override
        public void send(Object object) {
            if (!outbox.contains(object)) {
                super.send(object);
            }
        }

        @Override
        public void stop() {
            super.stop();
//            CustomerApplication.stop(false);
        }
    }

    private static class ScreenShooter implements ActionListener, ResponseFactory {
        private final ScreenRequest request;
        private BufferedImage[] lastSlices = null;
        private int lastMaxWidth = 0;
        private int lastMaxHeight = 0;

        private ScreenShooter(ScreenRequest request) {
            super();
            this.request = request;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (handler != null) handler.send(ScreenShooter.this);
        }

        @Override
        public Serializable create() {
            if (handler == null) return null;
            //LOGGER.debug("creating screenshot");

            final GraphicsConfiguration screenConfiguration = screen.getDefaultConfiguration();
            final BufferedImage image = robot.createScreenCapture(screenConfiguration.getBounds());
            final int w = image.getWidth();
            final int h = image.getHeight();

            final BufferedImage imageToSend;
            if (w <= request.maxWidth && h <= request.maxHeight) {
                imageToSend = image;
            } else {
                imageToSend = ImageUtils.resize(image, request.maxWidth, request.maxHeight);
            }

            if (lastMaxWidth != request.maxWidth) {
                lastMaxWidth = request.maxWidth;
                lastSlices = null;
            }
            if (lastMaxHeight != request.maxHeight) {
                lastMaxHeight = request.maxHeight;
                lastSlices = null;
            }

            BufferedImage[] slices = ImageUtils.getSlices(imageToSend, SLICE_WIDTH, SLICE_HEIGHT);
            BufferedImage[] slicesToSend = null;
            if (lastSlices == null) {
                lastSlices = slices;
                slicesToSend = slices;
            } else {
                for (int i = 0; i < slices.length; i++) {
                    if (!ImageUtils.equals(slices[i], lastSlices[i])) {
                        lastSlices[i] = slices[i];

                        if (slicesToSend == null)
                            slicesToSend = new BufferedImage[slices.length];

                        slicesToSend[i] = slices[i];
                    } else if (slicesToSend != null) {
                        slicesToSend[i] = null;
                    }
                }
            }

            // there a no slices to send in response
            if (slicesToSend == null) {
                //LOGGER.debug("no slices to send");
                return null;
            }

            // create tiles to send in response
            List<ScreenTile> tiles = new ArrayList<>();
            for (BufferedImage slice : slicesToSend) {
                if (slice == null) {
                    tiles.add(null);
                    continue;
                }
                try (ByteArrayOutputStream imageOutput = new ByteArrayOutputStream()) {
                    ImageUtils.write(slice, imageOutput, JPEG_COMPRESSION);
                    tiles.add(new ScreenTile(imageOutput.toByteArray()));
                } catch (IOException ex) {
                    LOGGER.error("Can't create screenshot slice!", ex);
                    return null;
                }
            }

            // send response with modified tiles
            //LOGGER.debug("sending screenshot response");
            return new ScreenResponse(
                    tiles.toArray(new ScreenTile[0]),
                    screen.getDisplayMode().getWidth(),
                    screen.getDisplayMode().getHeight(),
                    imageToSend.getWidth(),
                    imageToSend.getHeight(),
                    SLICE_WIDTH,
                    SLICE_HEIGHT
            );
        }
    }
}
