/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kx;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import studio.ui.MyUserInfo;

/**
 *
 * @author sitsang
 */
public class SSHManager {
    JSch jsch=new JSch();
    Session session=null;
    FileObject fo = null;
    static UserInfo ui=new MyUserInfo();
    public static FileSystemOptions opts = new FileSystemOptions();

    public static FileObject resolveFile(String filename) throws FileSystemException {
        FileSystemManager manager = VFS.getManager();
        FileName urlname =  manager.resolveURI(filename);
        FileObject fo = manager.resolveFile(urlname.getRootURI(),opts);
        FileObject file = fo.resolveFile(urlname.getPath());
        return file;
    }
    public void init(String host) {
        try {
            String userid = System.getProperty("user.name");
            String home = System.getProperty("user.home");
            String pubKeyRSA = home + File.separator + ".ssh" + File.separator + "id_rsa";
            String pubKeyDSA = home + File.separator + ".ssh" + File.separator + "id_dsa";
            String knownHost = home + File.separator + ".ssh" + File.separator + "known_hosts";
            File idFile = null;
            if (Files.exists(Paths.get(pubKeyRSA))) {
                jsch.addIdentity(pubKeyRSA);
                idFile = new File(pubKeyRSA);
            }
            else if (Files.exists(Paths.get(pubKeyDSA))) {
                jsch.addIdentity(pubKeyDSA);
                idFile = new File(pubKeyDSA); 
            }
            if (Files.exists(Paths.get(knownHost))) {
                jsch.setKnownHosts(knownHost);
            }
            session=jsch.getSession(userid, host, 22);
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setUserInfo(ui);
            session.connect(10000);

            //int localPort = session.setPortForwardingL(0,"localhost",port);

            SftpFileSystemConfigBuilder sftp = SftpFileSystemConfigBuilder.getInstance();
            sftp.setUserDirIsRoot(opts, false);
            sftp.setKnownHosts(opts, new File(knownHost));
            sftp.setIdentities(opts, new File[]{idFile});
            sftp.setUserInfo(opts, ui);
            sftp.setPreferredAuthentications(opts, "publickey,keyboard-interactive,password");

            String url = "sftp://" + session.getUserName() + "@" + session.getHost();
            fo = VFS.getManager().resolveFile(url,opts);

            System.out.println("URL = " + url);
            ChannelSftp schannel = (ChannelSftp) session.openChannel("sftp");
            schannel.connect();
            String pwd = schannel.getHome();
            schannel.disconnect();    

            FileObject appFolder = fo.resolveFile(pwd);
            while (!appFolder.exists() || appFolder.getName().toString().equals("/")) {
                appFolder = appFolder.getParent();
            }
            fo = appFolder;

            System.out.println("Working Directory = " + appFolder.getName().getPath());
            //return localPort;
        } catch (Exception e) {
            try {
                session.disconnect();
            }
            finally {
                session = null;
            }
            //throw new IOException(e);
        }
    }
    
    
    public void close() {
        if (session != null)
            try {
                session.disconnect();
            }
            finally {
                session = null;
            }
    }
}
