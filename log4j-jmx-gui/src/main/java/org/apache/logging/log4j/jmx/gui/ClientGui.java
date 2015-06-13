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
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
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
import javax.swing.WindowConstants;

import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.jmx.StatusLoggerAdminMBean;

/**
 * Swing GUI that connects to a Java process via JMX and allows the user to view
 * and modify the Log4j 2 configuration, as well as monitor status logs.
 *
 * @see <a href=
 *      "http://docs.oracle.com/javase/6/docs/technotes/guides/management/jconsole.html"
 *      >http://docs.oracle.com/javase/6/docs/technotes/guides/management/
 *      jconsole.html</a >
 */
public class ClientGui extends JPanel implements NotificationListener {
    private static final long serialVersionUID = -253621277232291174L;
    private static final int INITIAL_STRING_WRITER_SIZE = 1024;
    private final Client client;
    private final Map<ObjectName, Component> contextObjNameToTabbedPaneMap = new HashMap<>();
    private final Map<ObjectName, JTextArea> statusLogTextAreaMap = new HashMap<>();
    private JTabbedPane tabbedPaneContexts;

    public ClientGui(final Client client) throws IOException, JMException {
        this.client = Objects.requireNonNull(client, "client");
        createWidgets();
        populateWidgets();

        // register for Notifications if LoggerContext MBean was added/removed
        final ObjectName addRemoveNotifs = MBeanServerDelegate.DELEGATE_NAME;
        final NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType(Server.DOMAIN); // only interested in Log4J2 MBeans
        client.getConnection().addNotificationListener(addRemoveNotifs, this, null, null);
    }

    private void createWidgets() {
        tabbedPaneContexts = new JTabbedPane();
        this.setLayout(new BorderLayout());
        this.add(tabbedPaneContexts, BorderLayout.CENTER);
    }

    private void populateWidgets() throws IOException, JMException {
        for (final LoggerContextAdminMBean ctx : client.getLoggerContextAdmins()) {
            addWidgetForLoggerContext(ctx);
        }
    }

    private void addWidgetForLoggerContext(final LoggerContextAdminMBean ctx) throws MalformedObjectNameException,
            IOException, InstanceNotFoundException {
        final JTabbedPane contextTabs = new JTabbedPane();
        contextObjNameToTabbedPaneMap.put(ctx.getObjectName(), contextTabs);
        tabbedPaneContexts.addTab("LoggerContext: " + ctx.getName(), contextTabs);

        final String contextName = ctx.getName();
        final StatusLoggerAdminMBean status = client.getStatusLoggerAdmin(contextName);
        if (status != null) {
            final JTextArea text = createTextArea();
            final String[] messages = status.getStatusDataHistory();
            for (final String message : messages) {
                text.append(message + '\n');
            }
            statusLogTextAreaMap.put(ctx.getObjectName(), text);
            registerListeners(status);
            final JScrollPane scroll = scroll(text);
            contextTabs.addTab("StatusLogger", scroll);
        }

        final ClientEditConfigPanel editor = new ClientEditConfigPanel(ctx);
        contextTabs.addTab("Configuration", editor);
    }

    private void removeWidgetForLoggerContext(final ObjectName loggerContextObjName) throws JMException, IOException {
        final Component tab = contextObjNameToTabbedPaneMap.get(loggerContextObjName);
        if (tab != null) {
            tabbedPaneContexts.remove(tab);
        }
        statusLogTextAreaMap.remove(loggerContextObjName);
        final ObjectName objName = client.getStatusLoggerObjectName(loggerContextObjName);
        try {
            // System.out.println("Remove listener for " + objName);
            client.getConnection().removeNotificationListener(objName, this);
        } catch (final ListenerNotFoundException ignored) {
        }
    }

    private JTextArea createTextArea() {
        final JTextArea result = new JTextArea();
        result.setEditable(false);
        result.setBackground(this.getBackground());
        result.setForeground(Color.black);
        result.setFont(new Font(Font.MONOSPACED, Font.PLAIN, result.getFont().getSize()));
        result.setWrapStyleWord(true);
        return result;
    }

    private JScrollPane scroll(final JTextArea text) {
        final JToggleButton toggleButton = new JToggleButton();
        toggleButton.setAction(new AbstractAction() {
            private static final long serialVersionUID = -4214143754637722322L;

            @Override
            public void actionPerformed(final ActionEvent e) {
                final boolean wrap = toggleButton.isSelected();
                text.setLineWrap(wrap);
            }
        });
        toggleButton.setToolTipText("Toggle line wrapping");
        final JScrollPane scrollStatusLog = new JScrollPane(text, //
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, //
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollStatusLog.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, toggleButton);
        return scrollStatusLog;
    }

    private void registerListeners(final StatusLoggerAdminMBean status) throws InstanceNotFoundException,
            MalformedObjectNameException, IOException {
        final NotificationFilterSupport filter = new NotificationFilterSupport();
        filter.enableType(StatusLoggerAdminMBean.NOTIF_TYPE_MESSAGE);
        final ObjectName objName = status.getObjectName();
        // System.out.println("Add listener for " + objName);
        client.getConnection().addNotificationListener(objName, this, filter, status.getContextName());
    }

    @Override
    public void handleNotification(final Notification notif, final Object paramObject) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() { // LOG4J2-538
                handleNotificationInAwtEventThread(notif, paramObject);
            }
        });
    }

    private void handleNotificationInAwtEventThread(final Notification notif, final Object paramObject) {
        if (StatusLoggerAdminMBean.NOTIF_TYPE_MESSAGE.equals(notif.getType())) {
            if (!(paramObject instanceof ObjectName)) {
                handle("Invalid notification object type", new ClassCastException(paramObject.getClass().getName()));
                return;
            }
            final ObjectName param = (ObjectName) paramObject;
            final JTextArea text = statusLogTextAreaMap.get(param);
            if (text != null) {
                text.append(notif.getMessage() + '\n');
            }
            return;
        }
        if (notif instanceof MBeanServerNotification) {
            final MBeanServerNotification mbsn = (MBeanServerNotification) notif;
            final ObjectName mbeanName = mbsn.getMBeanName();
            if (MBeanServerNotification.REGISTRATION_NOTIFICATION.equals(notif.getType())) {
                onMBeanRegistered(mbeanName);
            } else if (MBeanServerNotification.UNREGISTRATION_NOTIFICATION.equals(notif.getType())) {
                onMBeanUnregistered(mbeanName);
            }
        }
    }

    /**
     * Called every time a Log4J2 MBean was registered in the MBean server.
     *
     * @param mbeanName ObjectName of the registered Log4J2 MBean
     */
    private void onMBeanRegistered(final ObjectName mbeanName) {
        if (client.isLoggerContext(mbeanName)) {
            try {
                final LoggerContextAdminMBean ctx = client.getLoggerContextAdmin(mbeanName);
                addWidgetForLoggerContext(ctx);
            } catch (final Exception ex) {
                handle("Could not add tab for new MBean " + mbeanName, ex);
            }
        }
    }

    /**
     * Called every time a Log4J2 MBean was unregistered from the MBean server.
     *
     * @param mbeanName ObjectName of the unregistered Log4J2 MBean
     */
    private void onMBeanUnregistered(final ObjectName mbeanName) {
        if (client.isLoggerContext(mbeanName)) {
            try {
                removeWidgetForLoggerContext(mbeanName);
            } catch (final Exception ex) {
                handle("Could not remove tab for " + mbeanName, ex);
            }
        }
    }

    private void handle(final String msg, final Exception ex) {
        System.err.println(msg);
        ex.printStackTrace();

        final StringWriter sw = new StringWriter(INITIAL_STRING_WRITER_SIZE);
        ex.printStackTrace(new PrintWriter(sw));
        JOptionPane.showMessageDialog(this, sw.toString(), msg, JOptionPane.ERROR_MESSAGE);
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
        final Properties props = System.getProperties();
        final Map<String, String> paramMap = new HashMap<>(props.size());
        for (final String key : props.stringPropertyNames()) {
            paramMap.put(key, props.getProperty(key));
        }
        final JMXConnector connector = JMXConnectorFactory.connect(url, paramMap);
        final Client client = new Client(connector);
        final String title = "Log4j JMX Client - " + url;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                installLookAndFeel();
                try {
                    final ClientGui gui = new ClientGui(client);
                    final JFrame frame = new JFrame(title);
                    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
                    JOptionPane.showMessageDialog(null, sr.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void usage() {
        final String me = ClientGui.class.getName();
        System.err.println("Usage: java " + me + " <host>:<port>");
        System.err.println("   or: java " + me + " service:jmx:rmi:///jndi/rmi://<host>:<port>/jmxrmi");
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
