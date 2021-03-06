
package studio.ui;

import studio.kdb.Server;
import studio.core.AuthenticationManager;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.GroupLayout;
import org.netbeans.editor.SettingsDefaults;
import studio.kdb.Config;
import studio.kdb.ConnectionPool;

public class ServerForm extends EscapeDialog {
    private int result= DialogResult.CANCELLED;
    private Server s;
    private String oldName = null;
    private Studio parent;

    public ServerForm(JFrame frame, Studio studio)
    {
        this(frame, new Server(), studio, false);
    }
    
    public ServerForm(JFrame frame, Server server, Studio studio, Boolean isClone){
        super(frame);
        parent = studio;
        
        initComponents();
        if (isClone) {
            s=new Server(server);
            jServerList.setEnabled(false);
        }
        else {
            s = server;
        }
        loadSetting();               
        reloadList(Config.getInstance().getOffset(s));
        
        logicalName.setToolTipText("The logical name for the server");
        hostname.setToolTipText("The hostname or ip address for the server");
        port.setToolTipText("The port for the server");
        username.setToolTipText("The username used to connect to the server");
        password.setToolTipText("The password used to connect to the server");
        authenticationMechanism.setToolTipText("The authentication mechanism to use");      
        SampleTextOnBackgroundTextField.setEditable(false);
        addWindowListener(new WindowAdapter()
        {
            public void windowOpened(WindowEvent e)
            {
                logicalName.requestFocus();
            }
        });
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) { 
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                testConnButton.setText("Test Connection");
            }
        };
        hostname.getDocument().addDocumentListener(dl);
        port.getDocument().addDocumentListener(dl);
        username.getDocument().addDocumentListener(dl);
        password.getDocument().addDocumentListener(dl);
        getRootPane().setDefaultButton(okButton);
    }
        
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    //
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jServerList = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        logicalNameLabel = new javax.swing.JLabel();
        logicalName = new javax.swing.JTextField();
        hostnameLabel = new javax.swing.JLabel();
        hostname = new javax.swing.JTextField();
        portLabel = new javax.swing.JLabel();
        port = new javax.swing.JTextField();
        usernameLabel = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        password = new javax.swing.JPasswordField();
        authenticationMechanism = new javax.swing.JComboBox();
        passwordLabel1 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        SampleTextOnBackgroundTextField = new javax.swing.JTextField();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        EditColorButton = new javax.swing.JButton();
        testConnButton = new javax.swing.JButton();
        jServerRemoveButton = new javax.swing.JButton();
        jServerCloneButton = new javax.swing.JButton();

        jSplitPane1.setDividerLocation(200);

        jServerList.setModel(new DefaultListModel());
        jServerList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jServerList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jServerListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jServerList);

        jSplitPane1.setLeftComponent(jScrollPane1);

        logicalNameLabel.setText("Name");

        hostnameLabel.setText("Host");

        portLabel.setText("Port");

        usernameLabel.setText("Username");

        passwordLabel.setText("Password");

        passwordLabel1.setText("Auth. Method");

        jLabel1.setText("Color");

        SampleTextOnBackgroundTextField.setText("Sample text on background");
        SampleTextOnBackgroundTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SampleTextOnBackgroundTextFieldActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSeparator2)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSeparator1))
                        .add(14, 14, 14))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(logicalNameLabel)
                            .add(hostnameLabel)
                            .add(portLabel)
                            .add(passwordLabel)
                            .add(usernameLabel))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(password)
                            .add(username)
                            .add(port)
                            .add(hostname)
                            .add(logicalName)))
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel1)
                            .add(passwordLabel1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(SampleTextOnBackgroundTextField)
                            .add(authenticationMechanism, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(jSeparator3))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(logicalNameLabel)
                    .add(logicalName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(hostnameLabel)
                    .add(hostname, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(portLabel)
                    .add(port, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(usernameLabel)
                    .add(username, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE, false)
                    .add(passwordLabel)
                    .add(password, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(authenticationMechanism, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(passwordLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(SampleTextOnBackgroundTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jSplitPane1.setRightComponent(jPanel1);

        okButton.setText("Ok");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onOk(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCancel(evt);
            }
        });

        EditColorButton.setText("Edit Color");
        EditColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onColor(evt);
            }
        });

        testConnButton.setText("Test Connection");
        testConnButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testConnButtonActionPerformed(evt);
            }
        });

        jServerRemoveButton.setIcon(Studio.getImage(Config.imageBase2 + "server_delete.png"));
        jServerRemoveButton.setToolTipText("");
        jServerRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jServerRemoveButtonActionPerformed(evt);
            }
        });

        jServerCloneButton.setIcon(Studio.getImage(Config.imageBase2 + "data_copy.png"));
        jServerCloneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jServerCloneButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .add(jServerRemoveButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jServerCloneButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(EditColorButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(testConnButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(cancelButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(okButton)
                        .addContainerGap())
                    .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 587, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(okButton)
                        .add(cancelButton)
                        .add(EditColorButton)
                        .add(testConnButton))
                    .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jServerRemoveButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                        .add(org.jdesktop.layout.GroupLayout.LEADING, jServerCloneButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .add(11, 11, 11))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void loadSetting() {
        logicalName.setText(s.getName());
        hostname.setText(s.getHost());
        String u= s.getUsername();
        if(u.trim().length()==0)
            u=System.getProperty("user.name");
        username.setText(u);
        port.setText(""+s.getPort());
        password.setText(s.getPassword());
        DefaultComboBoxModel dcbm= (DefaultComboBoxModel)authenticationMechanism.getModel();
        String [] am;
        try {
            am = AuthenticationManager.getInstance().getAuthenticationMechanisms();
            for(int i= 0;i < am.length; i++)
            {
                dcbm.addElement(am[i]);
                if(s.getAuthenticationMechanism().equals(am[i]))
                    dcbm.setSelectedItem(am[i]);
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (InstantiationException ex) {
            ex.printStackTrace();
        }
        catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        catch (InvocationTargetException ex) {
            ex.printStackTrace();
        }
        SampleTextOnBackgroundTextField.setBackground(s.getBackgroundColor());
        c = s.getBackgroundColor();
    }
    private void onApply(java.awt.event.ActionEvent evt) {
        logicalName.setText(logicalName.getText().trim());
        hostname.setText(hostname.getText().trim());
        username.setText(username.getText().trim());
        port.setText(port.getText().trim());
        password.setText(new String(password.getPassword()).trim());
        
        if(logicalName.getText().length() == 0)
        {
            JOptionPane.showMessageDialog(this,
                                          "The server's name cannot be empty",
                                          "Studio for kdb+",
                                          JOptionPane.ERROR_MESSAGE);
            logicalName.requestFocus();
            return;
        }    

        if(oldName != null)
        {
            JOptionPane.showMessageDialog(this,
                                          "Only one edit action is supported per editing",
                                          "Studio for kdb+",
                                          JOptionPane.ERROR_MESSAGE);
            logicalName.requestFocus();
            return;
        }
        else {
            if (!evt.getActionCommand().contains("Connection"))
                oldName = s.getName();
            s.setName(logicalName.getText().trim());
            s.setHost(hostname.getText().trim());
            s.setUsername(username.getText().trim());
            if(port.getText().length() == 0)
                s.setPort(0);
            else
                s.setPort(Integer.parseInt(port.getText()));

            s.setPassword(new String(password.getPassword()).trim());     
            DefaultComboBoxModel dcbm= (DefaultComboBoxModel)authenticationMechanism.getModel();
            s.setAuthenticationMechanism((String)dcbm.getSelectedItem());
        }
    }
        
    private void onOk(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onOk
        onApply(evt);
        dispose();
        result= DialogResult.ACCEPTED;
    }//GEN-LAST:event_onOk


    public int getResult(){return result;}
    public Server getServer(){return s;}
    public String getOldName(){return oldName;}
    
    private void onCancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCancel
        dispose();
    }//GEN-LAST:event_onCancel

    Color c = SettingsDefaults.defaultBackColor;
private void onColor(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onColor
      final JColorChooser chooser = new JColorChooser(c);
    //      chooser.setPreviewPanel(new CustomPane());
            c= SampleTextOnBackgroundTextField.getBackground();

            JDialog dialog = JColorChooser.createDialog(this,
            "Select background color for editor", true, chooser, new ActionListener() {
              public void actionPerformed(ActionEvent e)
              {
                  c=chooser.getColor();
              }
            }, null);
            
        dialog.setVisible(true);

        SampleTextOnBackgroundTextField.setBackground(c);
        s.setBackgroundColor(c);
}//GEN-LAST:event_onColor

private void SampleTextOnBackgroundTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SampleTextOnBackgroundTextFieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_SampleTextOnBackgroundTextFieldActionPerformed

    private void testConnButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testConnButtonActionPerformed
        kx.c conn = null;
        try {
            onApply(evt);
            ConnectionPool.getInstance().purge(s);
            conn = ConnectionPool.getInstance().leaseConnection(s);
            if (conn == null)
                JOptionPane.showMessageDialog(null, "Cannot establish connection","Connection failure",JOptionPane.ERROR_MESSAGE);
            ConnectionPool.getInstance().checkConnected(conn);
            testConnButton.setText("Connection OK");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(),"Connection failure",JOptionPane.ERROR_MESSAGE);
        } finally {
            if (conn != null)
                ConnectionPool.getInstance().freeConnection(s,conn);
        }
    }//GEN-LAST:event_testConnButtonActionPerformed

    private void jServerListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jServerListValueChanged
        if (!evt.getValueIsAdjusting()) {
            Server[] servers = Config.getInstance().getServers();
       
            int prev_offset = Config.getInstance().getOffset(s);    
            
            if (prev_offset == -1) {
                if (servers.length > 0)
                    s = servers[0];
                else 
                    s = new Server();
            }
            else if (prev_offset != evt.getLastIndex()) {
                if (evt.getLastIndex() <= servers.length - 1)
                    s = servers[evt.getLastIndex()];
            }
            else if (prev_offset != evt.getFirstIndex()) {
                if (evt.getLastIndex() <= servers.length - 1)
                    s = servers[evt.getFirstIndex()];
            }
            loadSetting();
        }
    }//GEN-LAST:event_jServerListValueChanged

    private void reloadList(int offset) {
        Server[] servers = Config.getInstance().getServers();
        DefaultListModel<String> model = (DefaultListModel<String>) jServerList.getModel();        
        
        for (int i = 0; i < servers.length; i++) {
            Server server  = servers[i];
            if (!model.contains(server.getName())) {
                model.add(i,server.getName());
            } else if (offset == -1 ) {
                if (model.removeElement(s.getName()))
                    break;
            }
        }
        if (offset >= 0 && servers.length > 0) {
            jServerList.setSelectedIndex(offset);
        }
    }
    
    private void jServerRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jServerRemoveButtonActionPerformed
        parent.actionServer(null, Studio.ACTION.REMOVE, s, s.getName());
        reloadList(-1);
    }//GEN-LAST:event_jServerRemoveButtonActionPerformed

    private void jServerCloneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jServerCloneButtonActionPerformed
        Server clone = new Server(s);
        clone.setName("Clone of " + clone.getName());
        s = clone;
        parent.actionServer(null, Studio.ACTION.CLONE, clone, null);
        int offset = Config.getInstance().getOffset(s);    
        reloadList(offset);
        //jServerList.setEnabled(false);
    }//GEN-LAST:event_jServerCloneButtonActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton EditColorButton;
    private javax.swing.JTextField SampleTextOnBackgroundTextField;
    private javax.swing.JComboBox authenticationMechanism;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField hostname;
    private javax.swing.JLabel hostnameLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JButton jServerCloneButton;
    private javax.swing.JList jServerList;
    private javax.swing.JButton jServerRemoveButton;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextField logicalName;
    private javax.swing.JLabel logicalNameLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JPasswordField password;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel passwordLabel1;
    private javax.swing.JTextField port;
    private javax.swing.JLabel portLabel;
    private javax.swing.JButton testConnButton;
    private javax.swing.JTextField username;
    private javax.swing.JLabel usernameLabel;
    // End of variables declaration//GEN-END:variables

}
