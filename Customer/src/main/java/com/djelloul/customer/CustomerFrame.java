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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.djelloul.core.ImageUtils;
import com.djelloul.core.common.Constants;
import com.djelloul.core.gui.SidebarPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Main frame of the customer application.
 *
 * @author Djelloul
 */
@SuppressWarnings("WeakerAccess")
public abstract class CustomerFrame extends JFrame {
    @SuppressWarnings("unused")
    private final static Logger LOGGER = LoggerFactory.getLogger(CustomerFrame.class);
    private final CustomerOptions options;
    private JLabel portLabel = null;
    private JSpinner portField = null;
    private JLabel screenLabel = null;
    private JComboBox<GraphicsDevice> screenField = null;
    private JCheckBox sslField = null;
    private JLabel statusLabel = null;
    private JButton startButton = null;
    private JButton stopButton = null;
    private boolean started = false;

    public boolean isStarted() {
		return started;
	}

	public CustomerFrame(CustomerOptions options) {
        super();
        this.options = options;
    }

    @SuppressWarnings("Duplicates")
    public void createAndShow() {
        // init frame
        setTitle("Customer Support Tool" + Constants.version);
        setIconImage(ImageUtils.loadImage(CustomerApplication.resource("application.png")));
        setPreferredSize(new Dimension(500, 250));
        setMinimumSize(new Dimension(450, 200));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doQuit();
            }
        });
        getRootPane().setBackground(Color.WHITE);
        getRootPane().setOpaque(true);

        // title
        JLabel titleLabel = new JLabel();
        titleLabel.setText("Remote Server");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, titleLabel.getFont().getSize2D() + 5));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        // info
        JLabel infoLabel = new JLabel();
        infoLabel.setText("<html>" + "Use this program to share your screen with our support staff." + "</html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // status
        statusLabel = new JLabel();
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 3, 3, 3));
        statusLabel.setVisible(false);

        // sidebar
        SidebarPanel sidebarPanel = new SidebarPanel();
//        SidebarPanel sidebarPanel = new SidebarPanel(
//        		ImageUtils.loadImage(CustomerApplication.resource("sidebar_staff.png")),
//        		ImageUtils.loadImage(CustomerApplication.resourceBranding())
//        		);

        // port number field
        portLabel = new JLabel();
        portLabel.setText("Port" + ":");
        portField = new JSpinner(new SpinnerNumberModel(
                (int) options.getPort(), 1, 65535, 1));
        portField.setBackground(Color.WHITE);
        portField.setEditor(new JSpinner.NumberEditor(portField, "#"));
        portField.addChangeListener(e -> options.setPort((Integer) portField.getValue()));

     // screen selection field
        screenLabel = new JLabel();
        screenLabel.setText("Screen" + ":");
        screenField = new JComboBox<>();
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
        JPanel formPanel = new JPanel(new MigLayout(
                "insets 10 10 10 10",
                "[][grow][][]",
                ""
        ));
        formPanel.setOpaque(false);
        formPanel.add(titleLabel, "span 4, width 100::, grow, wrap");
        formPanel.add(infoLabel, "span 4, width 100::, grow, wrap");
        formPanel.add(portLabel, "align right");
        formPanel.add(portField, "wrap");
        formPanel.add(screenLabel, "align right");
        formPanel.add(screenField, "span 3, width 100::, grow, wrap");
        formPanel.add(new JLabel(), "align right");
        formPanel.add(sslField, "span 3");

        // start button
        startButton = new JButton();
        startButton.setText("Start");
        startButton.addActionListener(e -> doStart());

        // stop button
        stopButton = new JButton();
        stopButton.setText("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> doStop());

        // quit button
        JButton quitButton = new JButton();
        quitButton.setText("Quit");
        quitButton.addActionListener(e -> doQuit());

        // build bottom bar
        JPanel buttonBar = new JPanel(new FlowLayout());
        buttonBar.setOpaque(false);
        buttonBar.add(quitButton);

        JPanel buttonBarLeft = new JPanel(new FlowLayout());
        buttonBarLeft.setOpaque(false);
        buttonBarLeft.add(startButton);
        buttonBarLeft.add(stopButton);

        JPanel bottomBar = new JPanel(new BorderLayout(0, 0));
        bottomBar.setOpaque(false);
        bottomBar.add(buttonBarLeft, BorderLayout.WEST);
        bottomBar.add(buttonBar, BorderLayout.EAST);
        bottomBar.add(statusLabel, BorderLayout.SOUTH);

        // add components to the frame
        getRootPane().setLayout(new BorderLayout(0, 0));
        getRootPane().add(sidebarPanel, BorderLayout.WEST);
        getRootPane().add(formPanel, BorderLayout.CENTER);
        getRootPane().add(bottomBar, BorderLayout.SOUTH);

        // show frame
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // update form
        setStarted(false);
        startButton.requestFocus();
    }

    protected abstract void doQuit();

    protected abstract void doStart();

    protected abstract void doStop();

    public Integer getPort() {
        return (Integer) portField.getValue();
    }

    public GraphicsDevice getScreen() {
        return (GraphicsDevice) screenField.getSelectedItem();
    }

    public boolean isSsl() {
        return sslField.isSelected();
    }

    public void setStarted(boolean started) {
    	this.started = started;
        portLabel.setEnabled(!started);
        portField.setEnabled(!started);
        sslField.setEnabled(!started);
        startButton.setEnabled(!started);
        stopButton.setEnabled(started);

        if (this.started)
            requestFocus();
        else
            startButton.requestFocus();
    }

    public void setStatusConnected() {
        statusLabel.setText("Connection was established.");
        statusLabel.setIcon(ImageUtils.loadIcon(CustomerApplication.resource("icon_connected.png")));
        statusLabel.setVisible(true);
    }

    public void setStatusConnecting() {
        statusLabel.setText("Establishing the connection.");
        statusLabel.setIcon(ImageUtils.loadIcon(CustomerApplication.resource("icon_connecting.png")));
        statusLabel.setVisible(true);
    }

    public void setStatusDisconnected() {
        statusLabel.setText("Connection was closed.");
        statusLabel.setIcon(ImageUtils.loadIcon(CustomerApplication.resource("icon_disconnected.png")));
        statusLabel.setVisible(true);
    }
}
