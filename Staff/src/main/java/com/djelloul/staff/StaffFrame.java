package com.djelloul.staff;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.djelloul.core.AppUtils;
import com.djelloul.core.ImageUtils;
import com.djelloul.core.common.Constants;
import com.djelloul.core.gui.SidebarPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Main frame of the staff application.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public abstract class StaffFrame extends JFrame {
	
	 @SuppressWarnings("unused")
	 private final static Logger LOGGER = LoggerFactory.getLogger(StaffFrame.class);
	 private final StaffOptions options;
	 private JPanel formPanel = null;
	 private JLabel hostLabel = null;
	 private JTextField hostField = null;
	 private JLabel portLabel = null;
	 private JSpinner portField = null;
	 private JLabel screenLabel = null;
	 private JComboBox<GraphicsDevice> screenField = null;
	 private JCheckBox sslField = null;
	 private JLabel statusLabel = null;
	 private JButton startButton = null;
	 private JButton stopButton = null;
	 private boolean started = false;
	 private ScreenPanel screenView = null;
	 private JButton actionsButton = null;
	 private JPopupMenu actionsMenu = null;
	 private JCheckBoxMenuItem sendKeyboardInput = null;
	 private JCheckBoxMenuItem sendMouseInput = null;
	 private JToggleButton optionsButton = null;
	 private JPanel optionsPanel = null;
	 private JLabel infoLabel = null;
	 private JLabel downloadLabel = null;
	 private JLabel uploadLabel = null;
	 private JSpinner localPortField = null;
	 private JCheckBox sshField = null;
	 private JLabel sshHostLabel = null;
	 private JTextField sshHostField = null;
	 private JLabel sshPortLabel = null;
	 private JSpinner sshPortField = null;
	 private JLabel sshRemotePortLabel = null;
	 private JSpinner sshRemotePortField = null;
	 private JLabel sshUserLabel = null;
	 private JTextField sshUserField = null;
	 private JLabel sshKeyLabel = null;
	 private JTextField sshKeyField = null;
	 private JButton sshKeyButton = null;
	 private JCheckBox sshKeyAuthField = null;
	 private CopyTextDialog copyTextDialog = null;

	 public StaffFrame(StaffOptions options) {
		 super();
		 this.options = options;
	 }

	 @SuppressWarnings("Duplicates")
	 public void createAndShow() {
		 // init frame
		 setTitle("Customer Support Tool" + Constants.version);

		 final BufferedImage applicationImage = ImageUtils.loadImage(StaffApplication.resource("application.png"));
		 
		 setIconImage(applicationImage);
		 setPreferredSize(new Dimension(600, 350));
		 setMinimumSize(new Dimension(500, 300));
		 setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		 addWindowListener(new WindowAdapter() {
			 @Override
			 public void windowClosing(WindowEvent e) {
				 doQuit();
			 }
		 });
		 
		 // screen view
		 screenView = new ScreenPanel(ObjectUtils.defaultIfNull(ImageUtils.loadImage(StaffApplication.resource("startup.png")), applicationImage));
		 screenView.setFocusTraversalKeysEnabled(false);
		 screenView.addKeyListener(new KeyAdapter() {
			 @Override
			 public void keyPressed(KeyEvent e) {
				 if (sendKeyboardInput.isSelected()) {
					 doHandleKeyPress(e);
				 }
			 }

			 @Override
			 public void keyReleased(KeyEvent e) {
				 if (sendKeyboardInput.isSelected()) {
					 doHandleKeyRelease(e);
				 }
			 }

			 @Override
			 public void keyTyped(KeyEvent e) {
				 if (sendKeyboardInput.isSelected()) {
					 doHandleKeyTyped(e);
				 }
			 }
		 });
		 screenView.addMouseListener(new MouseAdapter() {
			 @Override
			 public void mouseEntered(MouseEvent e) {
				 if (sendMouseInput.isSelected()) {
					 if (!started) return;
					 screenView.requestFocus();
				 }
			 }

			 @Override
			 public void mousePressed(MouseEvent e) {
				 if (sendMouseInput.isSelected()) {
					 doHandleMousePress(e);
				 }
			 }

			 @Override
			 public void mouseReleased(MouseEvent e) {
				 if (sendMouseInput.isSelected()) {
					 doHandleMouseRelease(e);
				 }
			 }
		 });
		 screenView.addMouseMotionListener(new MouseMotionAdapter() {
			 @Override
			 public void mouseMoved(MouseEvent e) {
				 if (sendMouseInput.isSelected()) {
					 doHandleMouseMotion(e);
				 }
			 }

			 @Override
			 public void mouseDragged(MouseEvent e) {
				 if (sendMouseInput.isSelected()) {
					 doHandleMouseMotion(e);
				 }
			 }
		 });
		 screenView.addMouseWheelListener(this::doHandleMouseWheel);
		 screenView.addComponentListener(new ComponentAdapter() {

			 @Override
			 public void componentResized(ComponentEvent e) {
				 doResize();
			 }
		 });
		 
		 getRootPane().setBackground(Color.WHITE);
		 getRootPane().setOpaque(true);

		 // title
		 JLabel titleLabel = new JLabel();
		 titleLabel.setText("Remote support for our customers");
		 titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 5));
		 titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		 // status
		 statusLabel = new JLabel();
		 statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 3, 3, 3));
		 statusLabel.setVisible(false);

		 // sidebar
		 SidebarPanel sidebarPanel = new SidebarPanel();
//		 SidebarPanel sidebarPanel = new SidebarPanel(
//				 ImageUtils.loadImage(StaffApplication.resource("sidebar_staff.png")),
//				 ImageUtils.loadImage(StaffApplication.resourceBranding())
//				 );

		 // hostname field
		 hostLabel = new JLabel();
		 hostLabel.setText("Host" + ":");
		 hostField = new JTextField();
		 hostField.setText(options.getHost());
		 hostField.setBackground(Color.WHITE);
		 hostField.getDocument().addDocumentListener(new DocumentListener() {
			 @Override
			 public void insertUpdate(DocumentEvent e) {
				 options.setHost(hostField.getText());
			 }

			 @Override
			 public void removeUpdate(DocumentEvent e) {
			 }

			 @Override
			 public void changedUpdate(DocumentEvent e) {
			 }
		 });

		 // port number field
		 portLabel = new JLabel();
		 portLabel.setText("Port" + ":");
		 portField = new JSpinner(new SpinnerNumberModel((int) options.getPort(), 1, 65535, 1));
		 portField.setBackground(Color.WHITE);
		 portField.setEditor(new JSpinner.NumberEditor(portField, "#"));
		 portField.addChangeListener(e -> options.setPort((Integer) portField.getValue()));

		 JLabel localPortLabel = new JLabel();
		 localPortLabel.setText("Port" + ":");
		 localPortField = new JSpinner(new SpinnerNumberModel((int) options.getLocalPort(), 1, 65535, 1));
		 localPortField.setBackground(Color.WHITE);
		 localPortField.setEditor(new JSpinner.NumberEditor(localPortField, "#"));
		 localPortField.addChangeListener(e -> options.setLocalPort((Integer) localPortField.getValue()));
		 
		 // ssl encryption field
		 sslField = new JCheckBox();
		 sslField.setText("SSL encryption");
		 sslField.setSelected(options.isSsl());
		 sslField.setOpaque(true);
		 sslField.setBackground(Color.WHITE);
		 sslField.addActionListener(e -> options.setSsl(sslField.isSelected()));

		 // ssh tunnel field
		 sshField = new JCheckBox();
		 sshField.setText("SSH tunneling");
		 sshField.setSelected(options.isSsh());
		 sshField.setOpaque(true);
		 sshField.setBackground(Color.WHITE);
		 sshField.addActionListener(e -> {
			 options.setSsh(sshField.isSelected());
			 setSshTunnel(sshField.isSelected());
		 });

		 // ssh host
		 sshHostLabel = new JLabel();
		 sshHostLabel.setText("SSH host" + ":");
		 sshHostField = new JTextField();
		 sshHostField.setText(options.getSshHost());
		 sshHostField.setBackground(Color.WHITE);
		 sshHostField.getDocument().addDocumentListener(new DocumentListener() {
			 @Override
			 public void insertUpdate(DocumentEvent e) {
				 options.setSshHost(sshHostField.getText());
			 }

			 @Override
			 public void removeUpdate(DocumentEvent e) {
			 }

			 @Override
			 public void changedUpdate(DocumentEvent e) {
			 }
		 });

		 // ssh port
		 sshPortLabel = new JLabel();
		 sshPortLabel.setText("SSH port" + ":");
		 sshPortField = new JSpinner(new SpinnerNumberModel(
				 (int) options.getSshPort(), 1, 65535, 1));
		 sshPortField.setBackground(Color.WHITE);
		 sshPortField.setEditor(new JSpinner.NumberEditor(sshPortField, "#"));
		 sshPortField.addChangeListener(e -> options.setSshPort((Integer) sshPortField.getValue()));

		 // ssh remote port
		 sshRemotePortLabel = new JLabel();
		 sshRemotePortLabel.setText("Remote port" + ":");
		 sshRemotePortField = new JSpinner(new SpinnerNumberModel(
				 (int) options.getSshRemotePort(), 1, 65535, 1));
		 sshRemotePortField.setBackground(Color.WHITE);
		 sshRemotePortField.setEditor(new JSpinner.NumberEditor(sshRemotePortField, "#"));
		 sshRemotePortField.addChangeListener(e -> options.setSshRemotePort((Integer) sshRemotePortField.getValue()));

		 // ssh user
		 sshUserLabel = new JLabel();
		 sshUserLabel.setText("SSH user" + ":");
		 sshUserField = new JTextField();
		 sshUserField.setText(options.getSshUser());
		 sshUserField.setBackground(Color.WHITE);
		 sshUserField.getDocument().addDocumentListener(new DocumentListener() {
			 @Override
			 public void insertUpdate(DocumentEvent e) {
				 options.setSshUser(sshUserField.getText());
			 }

			 @Override
			 public void removeUpdate(DocumentEvent e) {
			 }

			 @Override
			 public void changedUpdate(DocumentEvent e) {
			 }
		 });

		 // ssh key
		 sshKeyLabel = new JLabel();
		 sshKeyLabel.setText("SSH key" + ":");
		 sshKeyField = new JTextField();
		 sshKeyField.setText(options.getSshKey());
		 sshKeyField.setBackground(Color.WHITE);
		 sshKeyField.setEditable(false);
		 sshKeyButton = new JButton();
		 sshKeyButton.setText("Select");
		 sshKeyButton.addActionListener(e -> {
			 JFileChooser ch = new JFileChooser();
			 ch.setDialogTitle("Select private key.");

			 File f = null;
			 if (StringUtils.isNotBlank(sshKeyField.getText())) {
				 f = new File(sshKeyField.getText());
				 if (!f.isFile()) f = null;
			 }
			 if (f != null) {
				 ch.setSelectedFile(f);
			 } else {
				 f = new File(SystemUtils.getUserHome(), ".ssh");
				 ch.setCurrentDirectory((f.isFile()) ? f : SystemUtils.getUserHome());
			 }

			 if (ch.showOpenDialog(StaffFrame.this) != JFileChooser.APPROVE_OPTION) return;
			 sshKeyField.setText(ch.getSelectedFile().getAbsolutePath());
			 options.setSshKey(sshKeyField.getText());
		 });
		 JPanel sshKeyFieldPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]"));
		 sshKeyFieldPanel.setOpaque(false);
		 sshKeyFieldPanel.add(sshKeyField, "growx");
		 sshKeyFieldPanel.add(sshKeyButton);

		 // ssh public key authentication
		 sshKeyAuthField = new JCheckBox();
		 sshKeyAuthField.setText("Authentication via public key.");
		 sshKeyAuthField.setSelected(options.isSshKeyAuth());
		 sshKeyAuthField.setOpaque(true);
		 sshKeyAuthField.setBackground(Color.WHITE);
		 sshKeyAuthField.addActionListener(e -> {
			 options.setSshKeyAuth(sshKeyAuthField.isSelected());
			 setSshTunnel(sshField.isSelected());
		 });

		 // options panel
		 JPanel basicOptionsPanel = new JPanel(new MigLayout("insets 0"));
		 basicOptionsPanel.setOpaque(false);
		 basicOptionsPanel.add(localPortField);
		 basicOptionsPanel.add(sslField);
		 basicOptionsPanel.add(sshField);

		 optionsPanel = new JPanel(new MigLayout(
				 "fillx",
				 "[][grow][][]",
				 ""
				 ));
		 optionsPanel.setOpaque(false);
		 optionsPanel.setVisible(false);
		 optionsPanel.add(new JSeparator(), "span 4, growx, wrap");
		 optionsPanel.add(localPortLabel, "align right");
		 optionsPanel.add(basicOptionsPanel, "span 3, wrap");
		 optionsPanel.add(sshHostLabel, "align right");
		 optionsPanel.add(sshHostField, "growx");
		 optionsPanel.add(sshPortLabel, "align right");
		 optionsPanel.add(sshPortField, "growx, wrap");
		 optionsPanel.add(sshUserLabel, "align right, grow 0");
		 optionsPanel.add(sshUserField, "growx");
		 optionsPanel.add(sshRemotePortLabel, "align right");
		 optionsPanel.add(sshRemotePortField, "growx, wrap");
		 optionsPanel.add(sshKeyLabel, "align right, grow 0");
		 optionsPanel.add(sshKeyFieldPanel, "span 4, growx, wrap");
		 optionsPanel.add(new JLabel());
		 optionsPanel.add(sshKeyAuthField, "span 4, growx");

		 // start button
		 startButton = new JButton();
		 startButton.setText("Connect");
		 startButton.addActionListener(e -> doStart());

		 // stop button
		 stopButton = new JButton();
		 stopButton.setText("Disconnect");
		 stopButton.setEnabled(false);
		 stopButton.addActionListener(e -> doStop());
		 
		 // quit button
		 JButton quitButton = new JButton();
		 quitButton.setText("Quit");
		 quitButton.addActionListener(e -> doQuit());

		 // options button
		 optionsButton = new JToggleButton();
		 optionsButton.setText("Options");
		 optionsButton.addActionListener(e -> {
			 boolean isChecked = optionsButton.isSelected();
			 optionsPanel.setVisible(isChecked);
		 });
		 
		 // actions button
		 actionsButton = new JButton();
		 actionsButton.setText("Actions");
		 actionsButton.addActionListener(e -> actionsMenu.show(actionsButton, 0, actionsButton.getHeight()));
		 actionsMenu = new JPopupMenu();

		 // send keyboard input
		 sendKeyboardInput = new JCheckBoxMenuItem();
		 sendKeyboardInput.setText("Send keyboard input");
		 sendKeyboardInput.setSelected(true);
		 actionsMenu.add(sendKeyboardInput);

		 // send mouse input
		 sendMouseInput = new JCheckBoxMenuItem();
		 sendMouseInput.setText("Send mouse input");
		 sendMouseInput.setSelected(true);
		 actionsMenu.add(sendMouseInput);

		 // paste text
		 actionsMenu.addSeparator();
		 actionsMenu.add(new AbstractAction("Copy text") {
			 @Override
			 public void actionPerformed(ActionEvent e) {
				 if (copyTextDialog == null) {
					 copyTextDialog = new CopyTextDialog();
					 copyTextDialog.createAndShow();
				 } else {
					 if (!copyTextDialog.isVisible()) {
						 copyTextDialog.setLocationRelativeTo(StaffFrame.this);
						 copyTextDialog.setVisible(true);
					 }
					 copyTextDialog.toFront();
				 }
			 }
		 });
		 
		 // screen selection field
		 screenLabel = new JLabel();
		 screenLabel.setText("Screen" + ":");
		 screenField = new JComboBox<>();
		 //screenField.setOpaque(true);
		 screenField.setBackground(Color.WHITE);
		 screenField.setLightWeightPopupEnabled(false);
		 screenField.setRenderer(new DefaultListCellRenderer() {
			 @Override
			 public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				 final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				 final GraphicsDevice screen = (GraphicsDevice) value;
				 final GraphicsConfiguration cfg = screen.getDefaultConfiguration();
				 final Rectangle bounds = cfg.getBounds();

				 label.setText(screen.getIDstring() + " ("
						 + bounds.width + "x" + bounds.height + "@" + cfg.getColorModel().getPixelSize() + "bpp; "
						 + "x=" + bounds.x + "; y=" + bounds.y + ")");
				 return label;
			 }
		 });
		 String selectedScreenId = options.getScreenId();
		 for (GraphicsDevice screen : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			 if (screen.getType() != GraphicsDevice.TYPE_RASTER_SCREEN) continue;
			 screenField.addItem(screen);
			 if (selectedScreenId != null && screen.getIDstring().equalsIgnoreCase(selectedScreenId)) {
				 screenField.setSelectedIndex(screenField.getItemCount() - 1);
			 }
		 }
		 screenField.addActionListener(e -> {
			 GraphicsDevice screen = (GraphicsDevice) screenField.getSelectedItem();
			 options.setScreenId((screen != null) ? screen.getIDstring() : null);
		 });

		 // ssl encryption field
		 sslField = new JCheckBox();
		 sslField.setText("Use SSL encryption.");
		 sslField.setSelected(options.isSsl());
		 sslField.setOpaque(true);
		 sslField.setBackground(Color.WHITE);
		 sslField.addActionListener(e -> options.setSsl(sslField.isSelected()));

		 // build form
		 formPanel = new JPanel(new MigLayout(
				 "insets 10 10 10 10",
				 "[][grow][][]",
				 ""
				 ));
		 formPanel.setOpaque(false);
		 formPanel.add(titleLabel, "span 4, width 100::, grow, wrap");
		 formPanel.add(hostLabel, "align right");
		 formPanel.add(hostField, "width 100::, grow");
		 formPanel.add(portLabel, "align right");
		 formPanel.add(portField, "wrap");
		 formPanel.add(screenLabel, "align right");
		 formPanel.add(screenField, "span 3, width 100::, grow, wrap");
		 formPanel.add(new JLabel(), "align right");
		 formPanel.add(sslField, "span 3");

		 // build bottom bar
		 JPanel buttonBar = new JPanel(new FlowLayout());
		 buttonBar.setOpaque(false);
		 buttonBar.add(optionsButton);
		 buttonBar.add(quitButton);

		 JPanel buttonBarLeft = new JPanel(new FlowLayout());
		 buttonBarLeft.setOpaque(false);
		 buttonBarLeft.add(startButton);
		 buttonBarLeft.add(stopButton);

		 JPanel bottomBar = new JPanel(new BorderLayout(0, 0));
		 bottomBar.setOpaque(false);
		 bottomBar.add(buttonBarLeft, BorderLayout.WEST);
		 bottomBar.add(buttonBar, BorderLayout.EAST);
		 bottomBar.add(statusLabel, BorderLayout.CENTER);
		 bottomBar.add(optionsPanel, BorderLayout.SOUTH);

		 // add components to the frame
		 getRootPane().setLayout(new BorderLayout(0, 0));
//		 getRootPane().add(screenView, BorderLayout.CENTER);
		 getRootPane().add(formPanel, BorderLayout.CENTER);
		 getRootPane().add(bottomBar, BorderLayout.SOUTH);

		 // info label
		 infoLabel = new JLabel();
		 infoLabel.setVisible(false);
		 infoLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_info.png")));

		 // upload label
		 uploadLabel = new JLabel();
		 uploadLabel.setVisible(false);
		 uploadLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_upload.png")));

		 // download label
		 downloadLabel = new JLabel();
		 downloadLabel.setVisible(false);
		 downloadLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_download.png")));

		 JPanel statusBar = new JPanel(new MigLayout("insets 0, aligny 50%, hidemode 3"));
		 statusBar.setOpaque(false);
		 statusBar.add(infoLabel);
		 statusBar.add(downloadLabel);
		 statusBar.add(uploadLabel);

		 // show frame
		 pack();
		 setLocationRelativeTo(null);
		 setVisible(true);

		 // update form
		 setStarted(false);
		 startButton.requestFocus();
	 }

	 public String getHost() {
		 return hostField.getText().trim();
	 }

	 public Integer getPort() {
		 return (Integer) portField.getValue();
	 }

	 public GraphicsDevice getScreen() {
		 return (GraphicsDevice) screenField.getSelectedItem();
	 }

	 public void setStarted(boolean started) {
		 this.started = started;
		 hostLabel.setEnabled(!started);
		 hostField.setEnabled(!started);
		 portLabel.setEnabled(!started);
		 portField.setEnabled(!started);
		 screenLabel.setEnabled(!started);
		 screenField.setEnabled(!started);
		 sslField.setEnabled(!started);
		 startButton.setEnabled(!started);
		 startButton.setVisible(!started);
		 stopButton.setEnabled(started);
		 stopButton.setVisible(started);
		 actionsButton.setEnabled(false);
		 actionsButton.setVisible(false);
		 optionsButton.setEnabled(!started);
		 optionsButton.setSelected(false);
		 optionsButton.setVisible(!started);
		 optionsPanel.setVisible(false);
		 infoLabel.setText(StringUtils.EMPTY);
		 infoLabel.setVisible(false);
		 downloadLabel.setText(StringUtils.EMPTY);
		 downloadLabel.setVisible(false);
		 uploadLabel.setText(StringUtils.EMPTY);
		 uploadLabel.setVisible(false);
		 
		 if (copyTextDialog != null) {
			 copyTextDialog.setVisible(false);
		 }
		 
		 if (started) {
			 requestFocus();
			 formPanel.setVisible(false);
			 getRootPane().add(screenView, BorderLayout.CENTER);
			 screenView.setVisible(true);
			 invalidate();
		 }
		 else {
			 startButton.requestFocus();
			 screenView.setVisible(false);
			 getRootPane().add(formPanel, BorderLayout.CENTER);
			 formPanel.setVisible(true);
			 invalidate();
		 }
	 }

	 public void setStatusConnected() {
		 statusLabel.setText("Connection was established.");
		 statusLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_connected.png")));
		 statusLabel.setVisible(true);
	 }

	 public void setStatusConnecting() {
		 statusLabel.setText("Establishing the connection.");
		 statusLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_connecting.png")));
		 statusLabel.setVisible(true);
	 }

	 public void setStatusDisconnected() {
		 statusLabel.setText("Connection was closed.");
		 statusLabel.setIcon(ImageUtils.loadIcon(StaffApplication.resource("icon_disconnected.png")));
		 statusLabel.setVisible(true);
	 }

	 public int getScreenHeight() {
		 return screenView.getHeight();
	 }

	 public int getScreenImageHeight() {
		 return screenView.getImageHeight();
	 }

	 public int getScreenImageWidth() {
		 return screenView.getImageWidth();
	 }

	 public int getScreenWidth() {
		 return screenView.getWidth();
	 }

	 public String getSshHost() {
		 return sshHostField.getText().trim();
	 }

	 public File getSshKey() {
		 String key = StringUtils.trimToNull(sshKeyField.getText());
		 if (key == null) return null;
		 File f = new File(key);
		 return (f.isFile()) ? f : null;
	 }

	 public Integer getSshPort() {
		 return (Integer) sshPortField.getValue();
	 }

	 public Integer getSshRemotePort() {
		 return (Integer) sshRemotePortField.getValue();
	 }

	 public String getSshUser() {
		 return sshUserField.getText().trim();
	 }

	 public boolean isSsh() {
		 return sshField.isSelected();
	 }

	 public boolean isSshKeyAuth() {
		 return sshKeyAuthField.isSelected();
	 }

	 public boolean isSsl() {
		 return sslField.isSelected();
	 }

	 public void setConnected(boolean connected) {
		 actionsButton.setEnabled(connected);
		 actionsButton.setVisible(connected);
	 }

	 public void setInfo(String txt) {
		 infoLabel.setText(txt);
		 uploadLabel.setVisible(false);
		 downloadLabel.setVisible(false);
		 infoLabel.setVisible(true);
	 }

	 public void setRates(float download, float upload) {
		 long downloadRate = (long) download;
		 long uploadRate = (long) upload;

		 downloadLabel.setText(AppUtils.getHumanReadableByteCount(downloadRate) + "/s");
		 uploadLabel.setText(AppUtils.getHumanReadableByteCount(uploadRate) + "/s");

		 infoLabel.setVisible(false);
		 downloadLabel.setVisible(true);
		 uploadLabel.setVisible(true);
	 }

	 public void setScreenDisabled() {
		 if (screenView != null) {
			 screenView.setDisabled();
			 screenView.repaint();
		 }
	 }

	 private void setSshTunnel(boolean enabled) {
		 sshHostLabel.setEnabled(enabled);
		 sshHostField.setEnabled(enabled);
		 sshPortLabel.setEnabled(enabled);
		 sshPortField.setEnabled(enabled);
		 sshRemotePortLabel.setEnabled(enabled);
		 sshRemotePortField.setEnabled(enabled);
		 sshUserLabel.setEnabled(enabled);
		 sshUserField.setEnabled(enabled);
		 sshKeyLabel.setEnabled(enabled);
		 sshKeyField.setEnabled(enabled && sshKeyAuthField.isSelected());
		 sshKeyButton.setEnabled(enabled && sshKeyAuthField.isSelected());
		 sshKeyAuthField.setEnabled(enabled);
	 }

	 public void updateScreen(List<BufferedImage> slices, int imageWidth, int imageHeight, int sliceWidth, int sliceHeight) {
		 screenView.setSlices(slices, imageWidth, imageHeight, sliceWidth, sliceHeight);
		 screenView.repaint();
	 }

	 protected abstract void doCopyText(String text);

	 protected abstract void doHandleKeyPress(KeyEvent e);

	 protected abstract void doHandleKeyRelease(KeyEvent e);

	 protected abstract void doHandleKeyTyped(KeyEvent e);

	 protected abstract void doHandleMouseMotion(MouseEvent e);

	 protected abstract void doHandleMousePress(MouseEvent e);

	 protected abstract void doHandleMouseRelease(MouseEvent e);

	 protected abstract void doHandleMouseWheel(MouseWheelEvent e);

	 protected abstract void doQuit();

	 protected abstract void doResize();

	 protected abstract void doStart();

	 protected abstract void doStop();

	 private class CopyTextDialog extends com.djelloul.staff.utils.CopyTextDialog {
		 private CopyTextDialog() {
			 super(StaffFrame.this);
		 }

		 @Override
		 protected void doSubmit(String text) {
			 doCopyText(text);
		 }
	 }

	 private static class ScreenPanel extends JPanel {
		 private final BufferedImage emptyImage;
		 private BufferedImage image = null;
		 private boolean disabled = false;

		 private ScreenPanel(BufferedImage emptyImage) {
			 super();
			 this.emptyImage = emptyImage;
		 }

		 private int getImageHeight() {
			 return (this.image != null) ? this.image.getHeight() : 0;
		 }

		 private int getImageWidth() {
			 return (this.image != null) ? this.image.getWidth() : 0;
		 }

		 private synchronized void setDisabled() {
			 if (image != null) {
				 image = ImageUtils.toGrayScale(image);
			 }
			 disabled = true;
		 }

		 private synchronized void setSlices(List<BufferedImage> slices, int imageWidth, int imageHeight, int sliceWidth, int sliceHeight) {
			 //LOGGER.debug("draw " + slices.size() + " slices");
			 if (image == null || image.getWidth() != imageWidth || image.getHeight() != imageHeight || disabled) {
				 disabled = false;
				 image = new BufferedImage(
						 imageWidth,
						 imageHeight,
						 BufferedImage.TYPE_INT_RGB
						 );
			 }

			 int x = 0;
			 int y = 0;
			 Graphics2D g = image.createGraphics();
			 for (BufferedImage slice : slices) {
				 if (slice != null) {
					 //LOGGER.debug("draw slice at " + x + " / " + y);
					 g.drawImage(slice, x, y, null);
				 }

				 if ((x + sliceWidth) < imageWidth) {
					 x += sliceWidth;
				 } else {
					 x = 0;
					 y += sliceHeight;
				 }
			 }
			 g.dispose();
		 }

		 @Override
		 protected void paintComponent(Graphics g) {
			 super.paintComponent(g);

			 final int panelWidth = getWidth();
			 final int panelHeight = getHeight();
			 final Graphics2D g2d = (Graphics2D) g;

			 try {
				 g2d.setColor(Color.BLACK);
				 g2d.fillRect(0, 0, panelWidth, panelHeight);

				 final Image img = (this.image != null) ? this.image : this.emptyImage;

				 if (img == null) return;

				 final int imgWidth = img.getWidth(null);
				 final int imgHeight = img.getHeight(null);
				 final int x = (int) ((double) (panelWidth - imgWidth) / 2d);
				 final int y = (int) ((double) (panelHeight - imgHeight) / 2d);

				 g2d.drawImage(img, x, y, null);
			 } finally {
				 g2d.dispose();
			 }
		 }
	 }

}
