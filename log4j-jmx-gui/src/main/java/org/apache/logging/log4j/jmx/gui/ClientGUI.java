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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.apache.logging.log4j.core.helpers.Assert;
import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;
import org.apache.logging.log4j.core.jmx.StatusLoggerAdminMBean;

/**
 * Swing GUI that connects to a Java process via JMX and allows the user to view and
 * modify the Log4j 2 configuration, as well as monitor status logs.
 *
 * @see <a href=
 *      "http://docs.oracle.com/javase/6/docs/technotes/guides/management/jconsole.html"
 *      >http://docs.oracle.com/javase/6/docs/technotes/guides/management/jconsole.html</a >
 */
public class ClientGUI extends JPanel implements NotificationListener {
    private static final long serialVersionUID = -253621277232291174L;
    private final Client client;
    private JTextArea statusLogTextArea;
    private JTabbedPane tabbedPane;
    private JToggleButton wrapLinesToggleButton;

    private final AbstractAction toggleWrapAction = new AbstractAction() {
        private static final long serialVersionUID = -4214143754637722322L;

        @Override
        public void actionPerformed(final ActionEvent e) {
            final boolean wrap = wrapLinesToggleButton.isSelected();
            statusLogTextArea.setLineWrap(wrap);
        }
    };

    public ClientGUI(final Client client) throws InstanceNotFoundException,
            MalformedObjectNameException, IOException {
        this.client = Assert.isNotNull(client, "client");
        createWidgets();
        populateWidgets();
        registerListeners();
    }

    private void createWidgets() {
        statusLogTextArea = new JTextArea();
        statusLogTextArea.setEditable(false);
        statusLogTextArea.setBackground(this.getBackground());
        statusLogTextArea.setForeground(Color.black);
        statusLogTextArea.setFont(new Font("Monospaced", Font.PLAIN,
                statusLogTextArea.getFont().getSize()));
        statusLogTextArea.setWrapStyleWord(true);

        wrapLinesToggleButton = new JToggleButton(toggleWrapAction);
        wrapLinesToggleButton.setToolTipText("Toggle line wrapping");
        final JScrollPane scrollStatusLog = new JScrollPane(statusLogTextArea, //
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, //
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollStatusLog.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, wrapLinesToggleButton);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("StatusLogger", scrollStatusLog);

        this.setLayout(new BorderLayout());
        this.add(tabbedPane, BorderLayout.CENTER);
    }

    private void populateWidgets() {

        final StatusLoggerAdminMBean statusAdmin = client.getStatusLoggerAdmin();
        final String[] messages = statusAdmin.getStatusDataHistory();
        for (final String message : messages) {
            statusLogTextArea.append(message + "\n");
        }

        for (final LoggerContextAdminMBean ctx : client.getLoggerContextAdmins()) {
            final ClientEditConfigPanel editor = new ClientEditConfigPanel(ctx);
            tabbedPane.addTab("LoggerContext: " + ctx.getName(), editor);
        }
    }

    private void registerListeners() throws InstanceNotFoundException,
            MalformedObjectNameException, IOException {
        final NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType(StatusLoggerAdminMBean.NOTIF_TYPE_MESSAGE);
        final ObjectName objName = new ObjectName(StatusLoggerAdminMBean.NAME);
        client.getConnection().addNotificationListener(objName, this, filter,
                null);
    }

    @Override
    public void handleNotification(final Notification notif, final Object paramObject) {
        if (StatusLoggerAdminMBean.NOTIF_TYPE_MESSAGE.equals(notif.getType())) {
            statusLogTextArea.append(notif.getMessage() + "\n");
        }
    }

    /**
     * Connects to the specified location and shows this panel in a window.
     * <p>
     * Useful links:
     * http://www.componative.com/content/controller/developer/insights
     * /jconsole3/
     *
     * @param args must have at least one parameter, which specifies the
     *            location to connect to. Must be of the form {@code host:port}
     *            or {@code service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi}
     *            or
     *            {@code service:jmx:rmi://<host>:<port>/jndi/rmi://<host>:<port>/jmxrmi}
     * @throws Exception if anything goes wrong
     */
    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            usage();
            return;
        }
        String serviceUrl = args[0];
        if (!serviceUrl.startsWith("service:jmx")) {
            serviceUrl = "service:jmx:rmi:///jndi/rmi://" + args[0] + "/jmxrmi";
        }
        final JMXServiceURL url = new JMXServiceURL(serviceUrl);
        final Map<String, String> paramMap = new HashMap<String, String>();
        for (final Object objKey : System.getProperties().keySet()) {
            final String key = (String) objKey;
            paramMap.put(key, System.getProperties().getProperty(key));
        }
        final JMXConnector connector = JMXConnectorFactory.connect(url, paramMap);
        final Client client = new Client(connector);
        final String title = "Log4j JMX Client - " + url;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                installLookAndFeel();
                try {
                    final ClientGUI gui = new ClientGUI(client);
                    final JFrame frame = new JFrame(title);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.getContentPane().add(gui, BorderLayout.CENTER);
                    frame.pack();
                    frame.setVisible(true);
                } catch (final Exception ex) {
                    // if console is visible, print error so that
                    // the stack trace remains visible after error dialog is
                    // closed
                    ex.printStackTrace();

                    // show error in dialog: there may not be a console window
                    // visible
                    final StringWriter sr = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sr));
                    JOptionPane.showMessageDialog(null, sr.toString(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void usage() {
        final String me = ClientGUI.class.getName();
        System.err.println("Usage: java " + me + " <host>:<port>");
        System.err.println("   or: java " + me
                + " service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi");
        final String longAdr = " service:jmx:rmi://<host>:<port>/jndi/rmi://<host>:<port>/jmxrmi";
        System.err.println("   or: java " + me + longAdr);
    }

    private static void installLookAndFeel() {
        try {
            for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
