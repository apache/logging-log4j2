/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.jmx.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;

/**
 * Panel for editing Log4j configurations.
 */
public class ClientEditConfigPanel extends JPanel {
    private static final long serialVersionUID = -7544651740950723394L;
    private static final int HORIZONTAL_GAP = 20;
    private static final int ERR_MSG_INITIAL_BUFFER_SIZE = 2048;
    private static final int LOCATION_TEXT_COLS = 50;
    private static final int CONFIG_TEXT_COLS = 60;
    private static final int CONFIG_TEXT_ROWS = 20;
    private static final int BUFFER_SIZE = 2048;

    private JTextField locationTextField;
    private JLabel locationLabel;
    private JButton buttonSendLocation;
    private JButton buttonSendConfigText;
    private JTextArea configTextArea;
    private final LoggerContextAdminMBean contextAdmin;

    private final AbstractAction actionReconfigureFromLocation = new AbstractAction(
            "Reconfigure from Location") {
        private static final long serialVersionUID = 6995219797596745774L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            try {
                contextAdmin.setConfigLocationUri(locationTextField.getText());
                populateWidgets();
                showConfirmation();
            } catch (final Exception ex) {
                populateWidgets();
                handle("Could not reconfigure from location", ex);
            }
        }
    };
    private final AbstractAction actionReconfigureFromText = new AbstractAction(
            "Reconfigure with XML Below") {
        private static final long serialVersionUID = -2846103707134292312L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            final String encoding = System.getProperty("file.encoding");
            try {
                contextAdmin.setConfigText(configTextArea.getText(), encoding);
                populateWidgets();
                showConfirmation();
            } catch (final Exception ex) {
                populateWidgets();
                handle("Could not reconfigure from XML", ex);
            }
        }
    };

    public ClientEditConfigPanel(final LoggerContextAdminMBean contextAdmin) {
        this.contextAdmin = contextAdmin;
        createWidgets();
        populateWidgets();
    }

    private void handle(final String msg, final Exception ex) {
        final StringWriter sr = new StringWriter(BUFFER_SIZE);
        final PrintWriter pw = new PrintWriter(sr);
        pw.println("Please check the StatusLogger tab for details");
        pw.println();
        ex.printStackTrace(pw);
        JOptionPane.showMessageDialog(this, sr.toString(), msg,
                JOptionPane.ERROR_MESSAGE);
    }

    private void showConfirmation() {
        JOptionPane.showMessageDialog(this, "Reconfiguration complete.",
                "Reconfiguration complete", JOptionPane.INFORMATION_MESSAGE);
    }

    private void populateWidgets() {
        try {
            configTextArea.setText(contextAdmin.getConfigText());
        } catch (final Exception ex) {
            final StringWriter sw = new StringWriter(ERR_MSG_INITIAL_BUFFER_SIZE);
            ex.printStackTrace(new PrintWriter(sw));
            configTextArea.setText(sw.toString());
        }
        final String uri = contextAdmin.getConfigLocationUri();
        locationTextField.setText(uri);
    }

    private void createWidgets() {
        configTextArea = new JTextArea(CONFIG_TEXT_ROWS, CONFIG_TEXT_COLS);
        // configTextArea.setEditable(false);
        configTextArea.setBackground(Color.white);
        configTextArea.setForeground(Color.black);
        configTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, configTextArea.getFont().getSize()));
        final JScrollPane scrollConfig = new JScrollPane(configTextArea);

        locationTextField = new JTextField(LOCATION_TEXT_COLS);
        locationLabel = new JLabel("Location: ");
        locationLabel.setLabelFor(locationTextField);
        buttonSendLocation = new JButton(actionReconfigureFromLocation);
        buttonSendConfigText = new JButton(actionReconfigureFromText);

        final JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.LINE_AXIS));
        north.add(locationLabel);
        north.add(locationTextField);
        north.add(buttonSendLocation);
        north.add(Box.createRigidArea(new Dimension(HORIZONTAL_GAP, 0)));
        north.add(buttonSendConfigText);

        this.setLayout(new BorderLayout());
        this.add(north, BorderLayout.NORTH);
        this.add(scrollConfig, BorderLayout.CENTER);
    }
}
