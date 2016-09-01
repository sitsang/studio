package studio.ui;

import com.googlecode.vfsjfilechooser2.VFSJFileChooser;
import com.googlecode.vfsjfilechooser2.filechooser.AbstractVFSFileFilter;
import com.googlecode.vfsjfilechooser2.utils.VFSUtils;
import studio.kdb.*;
import studio.utils.OSXAdapter;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import kx.c;
import org.netbeans.editor.*;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.ExtSettingsInitializer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.table.TableModel;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import kx.SSHManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.netbeans.editor.example.QKit;
import org.netbeans.editor.ext.q.QSettingsInitializer;
import studio.utils.BrowserLaunch;
import studio.utils.SwingWorker;

public class Studio extends JPanel implements Observer,WindowListener {
    public class QTextFileFilter extends AbstractVFSFileFilter {
        public boolean accept(FileObject f) {
          if (VFSUtils.isDirectory(f))
            return true;
          return f.getName().getBaseName().toLowerCase().endsWith(".q");
        }
        public String getDescription() {
          return "q script (*.q)";
        }
    }
    public class CSVTextFileFilter extends AbstractVFSFileFilter {
        public boolean accept(FileObject f) {
          if (VFSUtils.isDirectory(f))
            return true;
          return f.getName().getBaseName().toLowerCase().endsWith(".csv");
        }
        public String getDescription() {
          return "Comma Delimited (*.csv)";
        }
    }
    
    static {
        // Register us
        //LocaleSupport.addLocalizer(new Impl("org.netbeans.editor.Bundle"));

        Settings.addInitializer(new BaseSettingsInitializer(),Settings.CORE_LEVEL);
        Settings.addInitializer(new ExtSettingsInitializer(),Settings.CORE_LEVEL);

        QKit editorKit = new QKit();
        JEditorPane.registerEditorKitForContentType(editorKit.getContentType(),
                                                    editorKit.getClass().getName());
        Settings.addInitializer(new QSettingsInitializer());
        Settings.reset();
    }
    private JTable table;
    private String exportFilename;
    private String lastQuery = null;
    private JMenuBar menubar;
    private JToolBar toolbar;
    private JEditorPane textArea;
    private JSplitPane splitpane;
    private JTabbedPane tabbedPane;
    private Font font = null;
    private UserAction arrangeAllAction;
    private UserAction closeFileAction;
    private UserAction newFileAction;
    private UserAction openFileAction;
    private UserAction openInExcel;
    private UserAction codeKxComAction;
    private UserAction openFileInNewWindowAction;
    private UserAction saveFileAction;
    private UserAction saveAsFileAction;
    private UserAction saveRemoteFileAction;
    
    private UserAction loadCSVFileAction;
    private UserAction exportAction;
    private UserAction builtToolTipAction;
    private UserAction chartAction;
    private ActionFactory.UndoAction undoAction;
    private ActionFactory.RedoAction redoAction;
    private BaseKit.CutAction cutAction;
    private BaseKit.CopyAction copyAction;
    private BaseKit.PasteAction pasteAction;
    private BaseKit.SelectAllAction selectAllAction;
    private Action findAction;
    private Action replaceAction;
    private UserAction stopAction;
    private UserAction executeAction;
    private UserAction executeCurrentLineAction;
    private UserAction refreshAction;
    private UserAction aboutAction;
    private UserAction exitAction;
    private UserAction toggleDividerOrientationAction;
    private UserAction minMaxDividerAction;    private UserAction editServerAction;
    private UserAction addServerAction;
    private UserAction removeServerAction;
    private static int scriptNumber = 0;
    private static int myScriptNumber;
    private JFrame frame;
    public static java.util.List windowList = Collections.synchronizedList(new LinkedList());
    private int menuShortcutKeyMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private Studio studio = this;
    //private static FileSystemOptions opts = new FileSystemOptions();

    public void refreshFrameTitle() {
        String s = (String) textArea.getDocument().getProperty("filename");
        if (s == null)
            s = "Script" + myScriptNumber;
        String title = s.replace('\\','/');
        frame.setTitle(title + (getModified() ? " (not saved) " : "") + (server!=null?" @"+server.toString():"") +" Studio for kdb+ " + Lm.getVersionString());
    }

    public static class WindowListChangedEvent extends EventObject {
        public WindowListChangedEvent(Object source) {
            super(source);
        }
    }

    public interface WindowListChangedEventListener extends EventListener {
        public void WindowListChangedEventOccurred(WindowListChangedEvent evt);
    }

    public static class WindowListMonitor {
        protected javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();

        public synchronized void addEventListener(WindowListChangedEventListener listener) {
            listenerList.add(WindowListChangedEventListener.class,listener);
        }

        public synchronized void removeEventListener(WindowListChangedEventListener listener) {
            listenerList.remove(WindowListChangedEventListener.class,listener);
        }

        synchronized void fireMyEvent(WindowListChangedEvent evt) {
            Object[] listeners = listenerList.getListenerList();
            for (int i = 0;i < listeners.length;i += 2)
                if (listeners[i] == WindowListChangedEventListener.class)
                    ((WindowListChangedEventListener) listeners[i + 1]).WindowListChangedEventOccurred(evt);
        }
    }
    public static WindowListMonitor windowListMonitor = new WindowListMonitor();

    private void updateKeyBindings(JEditorPane editorPane) {
        InputMap inputMap = editorPane.getInputMap();
        inputMap.put(KeyStroke.getKeyStroke("DELETE"),ExtKit.deleteNextCharAction);
        inputMap.put(KeyStroke.getKeyStroke("BACK_SPACE"),ExtKit.deletePrevCharAction);
        inputMap.put(KeyStroke.getKeyStroke("ENTER"),ExtKit.insertBreakAction);
        inputMap.put(KeyStroke.getKeyStroke("UP"),ExtKit.upAction);
        inputMap.put(KeyStroke.getKeyStroke("DOWN"),ExtKit.downAction);
        inputMap.put(KeyStroke.getKeyStroke("LEFT"),ExtKit.backwardAction);
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"),ExtKit.forwardAction);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Z"),ExtKit.undoAction);
        inputMap.put(KeyStroke.getKeyStroke("ctrl Y"),ExtKit.redoAction);
    }

    private void updateUndoRedoState(UndoManager um) {
        undoAction.setEnabled(um.canUndo());
        redoAction.setEnabled(um.canRedo());
    }

    private void initDocument() {
        initActions();
        refreshActionState();

        Document doc = null;
        if (textArea == null) {
            textArea = new JEditorPane("text/q","");
            Object o = textArea.getEditorKit();
            Action[] actions = textArea.getActions();

            for (int i = 0;i < actions.length;i++)
                if (actions[i] instanceof BaseKit.CopyAction) {
                    copyAction = (BaseKit.CopyAction) actions[i];
                    copyAction.putValue(Action.SHORT_DESCRIPTION,"Copy the selected text to the clipboard");
                    copyAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "copy.png"));
                    copyAction.putValue(Action.NAME,I18n.getString("Copy"));
                    copyAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_C));
                    copyAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_C,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof BaseKit.CutAction) {
                    cutAction = (BaseKit.CutAction) actions[i];
                    cutAction.putValue(Action.SHORT_DESCRIPTION,"Cut the selected text");
                    cutAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "cut.png"));
                    cutAction.putValue(Action.NAME,I18n.getString("Cut"));
                    cutAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_T));
                    cutAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_X,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof BaseKit.PasteAction) {
                    pasteAction = (BaseKit.PasteAction) actions[i];
                    pasteAction.putValue(Action.SHORT_DESCRIPTION,"Paste text from the clipboard");
                    pasteAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "paste.png"));
                    pasteAction.putValue(Action.NAME,I18n.getString("Paste"));
                    pasteAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_P));
                    pasteAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_V,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof ExtKit.FindAction) {
                    findAction = actions[i];
                    findAction.putValue(Action.SHORT_DESCRIPTION,"Find text in the document");
                    findAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "find.png"));
                    findAction.putValue(Action.NAME,I18n.getString("Find"));
                    findAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_F));
                    findAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_F,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof ExtKit.ReplaceAction) {
                    replaceAction = actions[i];
                    replaceAction.putValue(Action.SHORT_DESCRIPTION,"Replace text in the document");
                    replaceAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "replace.png"));
                    replaceAction.putValue(Action.NAME,I18n.getString("Replace"));
                    replaceAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_R));
                    replaceAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_R,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof BaseKit.SelectAllAction) {
                    selectAllAction = (BaseKit.SelectAllAction) actions[i];
                    selectAllAction.putValue(Action.SHORT_DESCRIPTION,"Select all text in the document");
                    selectAllAction.putValue(Action.SMALL_ICON,null);
                    selectAllAction.putValue(Action.NAME,I18n.getString("SelectAll"));
                    selectAllAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_A));
                    selectAllAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_A,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof ActionFactory.UndoAction) {
                    undoAction = (ActionFactory.UndoAction) actions[i];
                    undoAction.putValue(Action.SHORT_DESCRIPTION,"Undo the last change to the document");
                    undoAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "undo.png"));
                    undoAction.putValue(Action.NAME,I18n.getString("Undo"));
                    undoAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_U));
                    undoAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_Z,menuShortcutKeyMask));
                }
                else if (actions[i] instanceof ActionFactory.RedoAction) {
                    redoAction = (ActionFactory.RedoAction) actions[i];
                    redoAction.putValue(Action.SHORT_DESCRIPTION,"Redo the last change to the document");
                    redoAction.putValue(Action.SMALL_ICON,getImage(Config.imageBase2 + "redo.png"));
                    redoAction.putValue(Action.NAME,I18n.getString("Redo"));
                    redoAction.putValue(Action.MNEMONIC_KEY,new Integer(KeyEvent.VK_R));
                    redoAction.putValue(Action.ACCELERATOR_KEY,KeyStroke.getKeyStroke(KeyEvent.VK_Y,menuShortcutKeyMask));
                }

            doc = textArea.getDocument();
            doc.putProperty("filename",null);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
        //  doc.putProperty("created", Boolean.TRUE);
        }
        else
            doc = textArea.getDocument();

        JComponent c = (textArea.getUI() instanceof BaseTextUI) ? Utilities.getEditorUI(textArea).getExtComponent() : new JScrollPane(textArea);

        doc.putProperty("server",server);

        MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
        if (mdl == null) {
            mdl = new MarkingDocumentListener(c);
            doc.putProperty("MarkingDocumentListener",mdl);
            doc.addDocumentListener(mdl);
        }
        mdl.setModified(false);

        UndoManager um = (UndoManager) doc.getProperty(BaseDocument.UNDO_MANAGER_PROP);
        if (um == null) {
            um = new UndoManager() {
                public void undoableEditHappened(UndoableEditEvent e) {
                    super.undoableEditHappened(e);
                    updateUndoRedoState(this);
                }

                public synchronized void redo() throws CannotRedoException {
                    super.redo();
                    updateUndoRedoState(this);
                }

                public synchronized void undo() throws CannotUndoException {
                    super.undo();
                    updateUndoRedoState(this);
                }
            };
            doc.putProperty(BaseDocument.UNDO_MANAGER_PROP,um);
            doc.addUndoableEditListener(um);
        }
        um.discardAllEdits();
        updateUndoRedoState(um);

        if (splitpane.getTopComponent() != c) {
            splitpane.setTopComponent(c);
            splitpane.setDividerLocation(0.5);
        }

        rebuildToolbar();
        rebuildMenuBar();

        textArea.requestFocus();
    }

    private void refreshActionState() {
        newFileAction.setEnabled(true);
        arrangeAllAction.setEnabled(true);
        openFileAction.setEnabled(true);
        openFileInNewWindowAction.setEnabled(true);
        saveFileAction.setEnabled(true);
        saveAsFileAction.setEnabled(true);
        saveRemoteFileAction.setEnabled(true);
        loadCSVFileAction.setEnabled(true);
        exportAction.setEnabled(false);
        chartAction.setEnabled(false);
        openInExcel.setEnabled(false);
        stopAction.setEnabled(false);
        executeAction.setEnabled(true);
        executeCurrentLineAction.setEnabled(true);
        refreshAction.setEnabled(false);

//        helpAction.setEnabled(true);
        aboutAction.setEnabled(true);
        exitAction.setEnabled(true);
    }

    private FileObject getRemoteObject() {
        c connection  = null;
        try {
            connection = ConnectionPool.getInstance().leaseConnection(server);
            ConnectionPool.getInstance().checkConnected(connection);
            return connection.getFileObject();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (connection != null)
            ConnectionPool.getInstance().freeConnection(server,connection);
        }
    };
                
    private FileObject openFilename(AbstractVFSFileFilter filter) {
        VFSJFileChooser chooser;
        chooser = new VFSJFileChooser();    
        chooser.setFileHidingEnabled(false);
        chooser.setFileSelectionMode(VFSJFileChooser.SELECTION_MODE.FILES_ONLY);
        chooser.setFileFilter(filter);
        chooser.setPreferredSize(new Dimension(720,500));

        //String filename = (String) textArea.getDocument().getProperty("filename");
        /*if (filename != null) {
            File file = new File(filename);
            File dir = new File(file.getPath());
            chooser.setCurrentDirectory(dir);
        }*/

        VFSJFileChooser.RETURN_TYPE option = chooser.showOpenDialog(textArea);

        if (option == VFSJFileChooser.RETURN_TYPE.APPROVE) {
            return chooser.getSelectedFileObject();
        }

        return null;
    }

    private void exportAsExcel(final String filename) {
        new ExcelExporter().exportTableX(frame,table,new File(filename),false);
    }

    private void exportAsDelimited(final TableModel model,final String filename,final char delimiter) {
        final String message = "Exporting data to " + filename;

        final String note = "0% complete";

        String title = "Studio for kdb+";
        UIManager.put("ProgressMonitor.progressText",title);

        final int min = 0;
        final int max = 100;
        final ProgressMonitor pm = new ProgressMonitor(frame,message,note,min,max);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = new Runnable() {
            public void run() {
                if (filename != null) {
                    String lineSeparator = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

                    BufferedWriter fw = null;

                    try {
                        fw = new BufferedWriter(new FileWriter(filename));

                        for (int col = 0;col < model.getColumnCount();col++) {
                            if (col > 0)
                                fw.write(delimiter);

                            fw.write(model.getColumnName(col));
                        }
                        fw.write(lineSeparator);

                        int maxRow = model.getRowCount();
                        int lastProgress = 0;

                        for (int r = 1;r <= maxRow;r++) {
                            for (int col = 0;col < model.getColumnCount();col++) {
                                if (col > 0)
                                    fw.write(delimiter);

                                K.KBase o = (K.KBase) model.getValueAt(r - 1,col);
                                if (!o.isNull())
                                    fw.write(o.toString(false));
                            }
                            fw.write(lineSeparator);

                            boolean cancelled = pm.isCanceled();

                            if (cancelled)
                                break;
                            else {
                                final int progress = (100 * r) / maxRow;
                                if (progress > lastProgress) {
                                    final String note = "" + progress + "% complete";
                                    SwingUtilities.invokeLater(new Runnable() {

                                        public void run() {
                                            pm.setProgress(progress);
                                            pm.setNote(note);
                                        }
                                    });

                                    Thread.yield();
                                }
                            }
                        }

                        fw.close();
                    }
                    catch (FileNotFoundException ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    finally {
                        pm.close();
                    }
                }
            }
        };

        Thread t = new Thread(runner);
        t.setName("export");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsXml(final TableModel model,final String filename) {
        final String message = "Exporting data to " + filename;

        final String note = "0% complete";

        String title = "Studio for kdb+";
        UIManager.put("ProgressMonitor.progressText",title);

        final int min = 0;
        final int max = 100;
        final ProgressMonitor pm = new ProgressMonitor(frame,message,note,min,max);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = new Runnable() {
            public void run() {
                if (filename != null) {
                    String lineSeparator = (String) java.security.AccessController.doPrivileged(new sun.security.action.GetPropertyAction("line.separator"));

                    BufferedWriter fw = null;

                    try {
                        fw = new BufferedWriter(new FileWriter(filename));

                        fw.write("<R>");

                        int maxRow = model.getRowCount();
                        int lastProgress = 0;

                        fw.write(lineSeparator);

                        String[] columns = new String[model.getColumnCount()];
                        for (int col = 0;col < model.getColumnCount();col++)
                            columns[col] = model.getColumnName(col);

                        for (int r = 1;r <= maxRow;r++) {
                            fw.write("<r>");
                            for (int col = 0;col < columns.length;col++) {
                                fw.write("<" + columns[col] + ">");

                                K.KBase o = (K.KBase) model.getValueAt(r - 1,col);
                                if (!o.isNull())
                                    fw.write(o.toString(false));

                                fw.write("</" + columns[col] + ">");
                            }
                            fw.write("</r>");
                            fw.write(lineSeparator);

                            boolean cancelled = pm.isCanceled();

                            if (cancelled)
                                break;
                            else {
                                final int progress = (100 * r) / maxRow;
                                if (progress > lastProgress) {
                                    final String note = "" + progress + "% complete";
                                    SwingUtilities.invokeLater(new Runnable() {

                                        public void run() {
                                            pm.setProgress(progress);
                                            pm.setNote(note);
                                        }
                                    });

                                    Thread.yield();
                                }
                            }
                        }
                        fw.write("</R>");

                        fw.close();
                    }
                    catch (FileNotFoundException ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    catch (IOException ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();  //To change body of catch statement use Options | File Templates.
                    }
                    finally {
                        pm.close();
                    }
                }
            }
        };

        Thread t = new Thread(runner);
        t.setName("export");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsTxt(String filename) {
        exportAsDelimited(table.getModel(),filename,'\t');
    }

    private void exportAsCSV(String filename) {
        exportAsDelimited(table.getModel(),filename,',');
    }

    private void export() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Export result set as");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        FileFilter csvFilter = null;
        FileFilter txtFilter = null;
        FileFilter xmlFilter = null;
        FileFilter xlsFilter = null;

        if (table != null) {
            csvFilter =
                new FileFilter() {
                    public String getDescription() {
                        return "csv (Comma delimited)";
                    }

                    public boolean accept(File file) {
                        if (file.isDirectory() || file.getName().endsWith(".csv"))
                            return true;
                        else
                            return false;
                    }
                };

            txtFilter =
                new FileFilter() {
                    public String getDescription() {
                        return "txt (Tab delimited)";
                    }

                    public boolean accept(File file) {
                        if (file.isDirectory() || file.getName().endsWith(".txt"))
                            return true;
                        else
                            return false;
                    }
                };

            xmlFilter =
                new FileFilter() {
                    public String getDescription() {
                        return "xml";
                    }

                    public boolean accept(File file) {
                        if (file.isDirectory() || file.getName().endsWith(".xml"))
                            return true;
                        else
                            return false;
                    }
                };


            xlsFilter =
                new FileFilter() {
                    public String getDescription() {
                        return "xls (Microsoft Excel)";
                    }

                    public boolean accept(File file) {
                        if (file.isDirectory() || file.getName().endsWith(".xls"))
                            return true;
                        else
                            return false;
                    }
                };

            chooser.addChoosableFileFilter(csvFilter);
            chooser.addChoosableFileFilter(txtFilter);
            chooser.addChoosableFileFilter(xmlFilter);
            chooser.addChoosableFileFilter(xlsFilter);
        }

        if (exportFilename != null) {
            File file = new File(exportFilename);
            File dir = new File(file.getPath());
            chooser.setCurrentDirectory(dir);
            chooser.ensureFileIsVisible(file);
            if (table != null)
                if (exportFilename.endsWith(".xls"))
                    chooser.setFileFilter(xlsFilter);
                else if (exportFilename.endsWith(".csv"))
                    chooser.setFileFilter(csvFilter);
                else if (exportFilename.endsWith(".xml"))
                    chooser.setFileFilter(xmlFilter);
                else if (exportFilename.endsWith(".txt"))
                    chooser.setFileFilter(txtFilter);
        }

        int option = chooser.showSaveDialog(textArea);

        if (option == JFileChooser.APPROVE_OPTION) {
            File sf = chooser.getSelectedFile();
            File f = chooser.getCurrentDirectory();
            String dir = f.getAbsolutePath();

//            Cursor cursor= frame.getCursor();

            try {
                //              frame.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                FileFilter ff = chooser.getFileFilter();

                exportFilename = dir + "/" + sf.getName();

                if (table != null)
                    if (exportFilename.endsWith(".xls"))
                        exportAsExcel(exportFilename);
                    else if (exportFilename.endsWith(".csv"))
                        exportAsCSV(exportFilename);
                    else if (exportFilename.endsWith(".txt"))
                        exportAsTxt(exportFilename);
                    else if (exportFilename.endsWith(".xml"))
                        exportAsXml(table.getModel(),exportFilename);
                    /*                    else if (exportFilename.endsWith(".res")) {
                    exportAsBin(exportFilename);
                    }
                     */
                    else
                        if (ff == csvFilter)
                            exportAsCSV(exportFilename);
                        else if (ff == xlsFilter)
                            exportAsExcel(exportFilename);
                        else if (ff == txtFilter)
                            exportAsTxt(exportFilename);
                        else if (ff == xmlFilter)
                            exportAsXml(table.getModel(),exportFilename);
                        /*else if( ff == binFilter){
                        exportAsBin(exportFilename);
                        }
                         */
                        else
                            JOptionPane.showMessageDialog(frame,
                                                          "Warning",
                                                          "You did not specify what format to export the file as.\n Cancelling data export",
                                                          JOptionPane.WARNING_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/warning.png"));
            /*                else {
            exportAsBin(exportFilename);
            }
             */
            }
            catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                                              "Error",
                                              "An error occurred whilst writing the export file.\n Details are: " + e.getMessage(),
                                              JOptionPane.ERROR_MESSAGE,
                                              getImage(Config.imageBase + "32x32/error.png"));
            }
            finally {
                //            frame.setCursor(cursor);
            }
        }
    }

    public void newFile() {
        try {
            String filename = (String) textArea.getDocument().getProperty("filename");
            if (!saveIfModified(filename))
                return;

            textArea.getDocument().remove(0,textArea.getDocument().getLength());
            textArea.getDocument().putProperty("filename",null);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            initDocument();
            refreshFrameTitle();
        }
        catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void openFile() {
        String filename = (String) textArea.getDocument().getProperty("filename");
        if (!saveIfModified(filename))
            return;

        FileObject fo = openFilename(new QTextFileFilter());

        if (fo != null) {
            filename = fo.getName().toString().replaceFirst("file:///", "");
            loadFile(filename);
            addToMruFiles(filename);
        }
    }
    // returns true to continue
    public boolean saveIfModified(String filename) {
        if (getModified()) {
            int choice = JOptionPane.showOptionDialog(frame,
                                                      "Changes not saved.\nSave now?",
                                                      "Save changes?",
                                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      getImage(Config.imageBase + "32x32/question.png"),
                                                      null, // use standard button titles
                                                      null);      // no default selection

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    if (saveFile(filename,false))
                        // was cancelled so return
                        return false;
                }
                catch (Exception e) {
                    return false;
                }
                return true;
            }
            else if ((choice == JOptionPane.CANCEL_OPTION) || (choice == JOptionPane.CLOSED_OPTION))
                return false;
        }
        return true;
    }

    public void loadMRUFile(String filename,String oldFilename) {
        if (!saveIfModified(oldFilename))
            return;

        loadFile(filename);
        addToMruFiles(filename);
        try {
            setServer(server);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addToMruFiles(String filename) {
        if (filename == null)
            return;

        Vector v = new Vector();
        v.add(filename);
        String[] mru = Config.getInstance().getMRUFiles();
        for (int i = 0;i < mru.length;i++)
            if (!v.contains(mru[i]))
                v.add(mru[i]);
        Config.getInstance().saveMRUFiles((String[]) v.toArray(new String[0]));
        rebuildMenuBar();
    }

    static public String getContents(FileObject file) {
        try {
            InputStream inputStream = file.getContent().getInputStream(); 
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer);

            return writer.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }
    
    public void loadFile(String filename) {
        try {
            FileObject file = SSHManager.resolveFile(filename);
            InputStream inputStream = file.getContent().getInputStream(); 
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer);

            String s = writer.toString();

            textArea.getDocument().remove(0,textArea.getDocument().getLength());
            textArea.getDocument().insertString(0,s,null);
            textArea.getDocument().putProperty("filename",filename);
            windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
            initDocument();
            setServer(server);
            textArea.setCaretPosition(0);
            refreshFrameTitle();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public FileObject saveToFile(FileObject file, AbstractVFSFileFilter filter) {
        VFSJFileChooser chooser = new VFSJFileChooser(file);    
        chooser.setFileHidingEnabled(false);
        chooser.setFileSelectionMode(VFSJFileChooser.SELECTION_MODE.FILES_ONLY);
        chooser.setDialogType(VFSJFileChooser.DIALOG_TYPE.SAVE);
        chooser.setDialogTitle("Save script as");
        chooser.setFileFilter(filter);
        chooser.setPreferredSize(new Dimension(720,500));

        //String filename = (String) textArea.getDocument().getProperty("filename");
        
        VFSJFileChooser.RETURN_TYPE option = chooser.showSaveDialog(textArea);
        if (option == VFSJFileChooser.RETURN_TYPE.APPROVE) {
            FileObject sf = chooser.getSelectedFileObject();

            try {
                if (sf.exists()) {
                    if (!sf.isWriteable()) {
                        JOptionPane.showMessageDialog(frame,"Cannot write to the specified location","File Error",JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                    int choice = JOptionPane.showOptionDialog(frame,
                                                              sf.getName().getBaseName() + " already exists.\nOverwrite?",
                                                              "Overwrite?",
                                                              JOptionPane.YES_NO_CANCEL_OPTION,
                                                              JOptionPane.QUESTION_MESSAGE,
                                                              getImage(Config.imageBase + "32x32/question.png"),
                                                              null, // use standard button titles
                                                              null);      // no default selection

                    if (choice != JOptionPane.YES_OPTION)
                        return null;
                }

                return sf;

            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
    
    public boolean saveAsFile(FileObject file) {
        FileObject sf = saveToFile(file,new QTextFileFilter());
        if ( sf != null)
            return saveFile(sf.toString(),true);
        else
        return false;
    }
    //   private boolean wasLoaded=false;
    // returns true if saved, false if error or cancelled
    public boolean saveFile(String filename,boolean force) {
        if (filename == null)
            return saveAsFile(null);

        try {
            if (!force)
                if (null == textArea.getDocument().getProperty("filename"))
                    return saveAsFile(null);

            FileObject file = SSHManager.resolveFile(filename);
            try {
                OutputStream out = file.getContent().getOutputStream();     
                try (OutputStreamWriter writer = new OutputStreamWriter(out)) {
                    try (BufferedWriter buf = new BufferedWriter(writer)) {
                        textArea.write(buf);
                    }
                }
            } finally {
                file.close();
            }
            
            if (!file.getURL().getProtocol().equals("sftp")) {
                filename = file.getURL().getFile().replaceFirst("//*", "");
                textArea.getDocument().putProperty("filename",filename);
                windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
                setModified(false);               
                addToMruFiles(filename);
                refreshFrameTitle();
            }
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void arrangeAll() {
        int noWins = windowList.size();

        Iterator i = windowList.iterator();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int noRows = noWins > 3 ? 3 : noWins;
        int height = screenSize.height / noRows;

        for (int row = 0;row < noRows;row++) {
            int noCols = (noWins / 3);

            if ((row == 0) && ((noWins % 3) > 0))
                noCols++;
            else if ((row == 1) && ((noWins % 3) > 1))
                noCols++;

            int width = screenSize.width / noCols;

            for (int col = 0;col < noCols;col++) {
                Object o = i.next();
                JFrame f;

                if (o instanceof Studio)
                    f = ((Studio) o).frame;
                else
                    f = (JFrame) o;

                f.setSize(width,height);
                f.setLocation(col * width,((noRows - 1) - row) * height);
                ensureDeiconified(f);
            }
        }
    }

    private void setModified(boolean value) {
        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null) {
                MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
                if (mdl != null)
                    mdl.setModified(value);
            }
        }
    }

    private boolean getModified() {
        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null) {
                MarkingDocumentListener mdl = (MarkingDocumentListener) doc.getProperty("MarkingDocumentListener");
                if (mdl != null)
                    return mdl.getModified();
            }
        }

        return true;
    }

    private void setServer(Server server) throws Exception {
        if (server == null)
            return;

        c connection = null;
        try {
            connection = ConnectionPool.getInstance().leaseConnection(server);
            ConnectionPool.getInstance().checkConnected(connection);
            com.jcraft.jsch.Session session = connection.getSession();
            saveRemoteFileAction.setEnabled(session != null);
            loadCSVFileAction.setEnabled(session != null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,ex.getMessage(),"Server Error",JOptionPane.ERROR_MESSAGE);
            throw ex;
        } finally {
            if (connection != null)
                ConnectionPool.getInstance().freeConnection(server,connection);
        }

        this.server = server;
        if (textArea != null) {
            Document doc = textArea.getDocument();

            if (doc != null)
                doc.putProperty("server",server);
            Utilities.getEditorUI(textArea).getComponent().setBackground(server.getBackgroundColor());
        }

        if (server != null) {
            new ReloadQKeywords(server);
            Config.getInstance().setLRUServer(server);
        }
        refreshFrameTitle();
        windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
    }

    PrintWriter writer;
    enum ACTION { ADD, CLONE, EDIT, REMOVE };
    public void actionServer(ActionListener li, ACTION action, Server s, String serverName) {
        try {
            switch(action) {
                case ADD:
                    writer.println("A: Removing " + serverName);
                    Config.getInstance().removeServerByName(serverName);
                    writer.println("A: Adding " + s.getName());
                    Config.getInstance().addServer(s);
                    ConnectionPool.getInstance().purge(s);
                    setServer(s);
                    break;
                case EDIT:
                    writer.println("E: Removing " + serverName);
                    Config.getInstance().removeServerByName(serverName);
                    writer.println("E: Adding " + s.getName());
                    Config.getInstance().addServer(s);
                    ConnectionPool.getInstance().purge(s);
                    setServer(s);
                    break;
                case REMOVE:
                    writer.println("R: Removing " + serverName);
                    Config.getInstance().removeServerByName(serverName);
                    Server[] servers = Config.getInstance().getServers();

                    if (servers.length > 0) {
                        setServer(servers[0]);
                        writer.println("R: Switched to " + servers[0].getName());
                    }
                    break;
                case CLONE:
                    writer.println("C: Removing " + serverName);
                    Config.getInstance().removeServerByName(serverName);
                    writer.println("C: Adding " + s.getName());
                    Config.getInstance().addServer(s);
                    ConnectionPool.getInstance().purge(s);
                    writer.println("C: Switched to " + s.getName());
                    setServer(s);
                    break;
            }
        } catch (Exception ex) {
            writer.println("ERROR");
            //ex.printStackTrace();
        } finally {
            rebuildToolbar();
            rebuildMenuBar();
            if (li != null)
                windowListMonitor.fireMyEvent(new WindowListChangedEvent(li));
        }
    }
    
    private void initActions() {
        newFileAction = new UserAction(I18n.getString("New"),
                                       getImage(Config.imageBase2 + "document_new.png"),
                                       "Create a blank script",
                                       new Integer(KeyEvent.VK_N),
                                       KeyStroke.getKeyStroke(KeyEvent.VK_N,menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                //   PrintUtilities.printComponent(textArea);
                newFile();
            }
        };

        arrangeAllAction = new UserAction(I18n.getString("ArrangeAll"),
                                          getImage(Config.imageBase2 + "blank.png"),
                                          "Arrange all windows on screen",
                                          new Integer(KeyEvent.VK_A),
                                          null) {
            public void actionPerformed(ActionEvent e) {
                arrangeAll();
            }
        };

        minMaxDividerAction = new UserAction(I18n.getString("MaximizeEditorPane"),
                                             getImage(Config.imageBase2 + "blank.png"),
                                             "Maximize editor pane",
                                             new Integer(KeyEvent.VK_M),
                                             KeyStroke.getKeyStroke(KeyEvent.VK_M,menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
              minMaxDivider();
            }
        };

        toggleDividerOrientationAction = new UserAction(I18n.getString("ToggleDividerOrientation"),
                                                        getImage(Config.imageBase2 + "blank.png"),
                                                        "Toggle the window divider's orientation",
                                                        new Integer(KeyEvent.VK_C),
                                                        null) {
            public void actionPerformed(ActionEvent e) {
                toggleDividerOrientation();
            }
        };

        closeFileAction = new UserAction(I18n.getString("Close"),
                                         getImage(Config.imageBase2 + "blank.png"),
                                         "Close current document",
                                         new Integer(KeyEvent.VK_C),
                                         null) {
            public void actionPerformed(ActionEvent e) {
                quitWindow();
                if (windowList.size() == 0)
                    System.exit(0);
            }
        };

        openFileAction = new UserAction(I18n.getString("Open"),
                                        getImage(Config.imageBase2 + "folder.png"),
                                        "Open a script",
                                        new Integer(KeyEvent.VK_O),
                                        KeyStroke.getKeyStroke(KeyEvent.VK_O,menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        };

        openFileInNewWindowAction = new UserAction(I18n.getString("NewWindow"),
                                                   getImage(Config.imageBase2 + "blank.png"),
                                                   "Open a new window",
                                                   new Integer(KeyEvent.VK_N),
                                                   null) {
            public void actionPerformed(ActionEvent e) {
                new Studio(server,null);
            }
        };

        editServerAction = new UserAction(I18n.getString("Edit"),
                                          getImage(Config.imageBase2 + "server_information.png"),
                                          "Edit the server details",
                                          new Integer(KeyEvent.VK_E),
                                          null) {
            public void actionPerformed(ActionEvent e) {
                Server s = new Server(server);

                EditServerForm f = new EditServerForm(frame,s,studio,false);
                f.setModal(true);
                f.pack();
                Util.centerChildOnParent(f,frame);
                f.show();

                if (f.getResult() == DialogResult.ACCEPTED) {
                    if (stopAction.isEnabled())
                        stopAction.actionPerformed(e);

                    s = f.getServer();
                    String serverName = f.getOldName();
                    actionServer(this,ACTION.EDIT,s,serverName);
                }
            }
        };


        addServerAction = new UserAction(I18n.getString("Add"),
                                         getImage(Config.imageBase2 + "server_add.png"),
                                         "Configure a new server",
                                         new Integer(KeyEvent.VK_A),
                                         null) {
            public void actionPerformed(ActionEvent e) {
                AddServerForm f = new AddServerForm(frame,studio);
                f.setModal(true);
                f.pack();
                Util.centerChildOnParent(f,frame);
                f.show();

                if (f.getResult() == DialogResult.ACCEPTED) {
                    Server s = f.getServer();
                    String oldName = f.getOldName();
                    actionServer(this,ACTION.ADD,s,oldName);
                }
            }
        };

        removeServerAction = new UserAction(I18n.getString("Remove"),
                                            getImage(Config.imageBase2 + "server_delete.png"),
                                            "Remove this server",
                                            new Integer(KeyEvent.VK_R),
                                            null) {
            public void actionPerformed(ActionEvent e) {
                int choice = JOptionPane.showOptionDialog(frame,
                                                          "Remove server " + server.getName() + " from list?",
                                                          "Remove server?",
                                                          JOptionPane.YES_NO_CANCEL_OPTION,
                                                          JOptionPane.QUESTION_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/question.png"),
                                                          null, // use standard button titles
                                                          null);      // no default selection

                if (choice == 0) {
                    actionServer(this,ACTION.REMOVE,server,server.getName());
                }
            }
        };


        saveFileAction = new UserAction(I18n.getString("Save"),
                                        getImage(Config.imageBase2 + "disks.png"),
                                        "Save the script",
                                        new Integer(KeyEvent.VK_S),
                                        KeyStroke.getKeyStroke(KeyEvent.VK_S,menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                String filename = (String) textArea.getDocument().getProperty("filename");
                saveFile(filename,false);
            }
        };

        saveAsFileAction = new UserAction(I18n.getString("SaveAs"),
                                          getImage(Config.imageBase2 + "save_as.png"),
                                          "Save script as",
                                          new Integer(KeyEvent.VK_A),
                                          null) {
            public void actionPerformed(ActionEvent e) {
                saveAsFile(null);
            }
        };
        
        saveRemoteFileAction = new UserAction("Save to Remote...",
                                          getImage(Config.imageBase2 + "save_as.png"),
                                          "Save to Remote KDB Server",
                                          new Integer(KeyEvent.VK_R),
                                          null) {          
            @Override 
            public void actionPerformed(ActionEvent e) {
                try {
                    FileObject file = getRemoteObject();

                    if (file != null)
                        saveAsFile(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            };
        };

        loadCSVFileAction = new UserAction("Load CSV File...",
                                          getImage(Config.imageBase2 + "import1.png"),
                                          "Load CSV on Remote KDB Server",
                                          new Integer(KeyEvent.VK_R),
                                          null) {
            public void actionPerformed(ActionEvent e) {
                try {
                    FileObject localfile = openFilename(new CSVTextFileFilter());
                    if (localfile == null) return;
                    FileObject remoteURL = getRemoteObject();
                    
                    if (remoteURL != null) {
                        FileObject remotefile = saveToFile(remoteURL,new CSVTextFileFilter());
                        if (remotefile == null) return;
                        
                        String localContent = getContents(localfile);
                        
                        try {
                            OutputStream out = remotefile.getContent().getOutputStream();     
                            try (PrintStream printStream = new PrintStream(out)) {
                                printStream.print(localContent);
                            }
                        } finally {
                            remotefile.close();
                        }

                        Document doc = textArea.getDocument();                        
                        String cmd = "";
                        try {
                            Scanner scanner = new Scanner(localContent);
                            String[] header = scanner.nextLine().split(",");
                            StringBuilder define = new StringBuilder();
                            try {
                                String[] data = scanner.nextLine().split(",");
                                for (int i = 0; i < header.length; ++i) {
                                    if (data[i].matches("-?\\d+"))
                                        define.append("J");
                                    else if (data[i].matches("-?\\d+(\\.\\d+)?"))
                                        define.append("F");
                                    else if (data[i].matches("0D\\d{2}:\\d{2}:\\d{2}\\.\\d+"))
                                        define.append("N");
                                    else 
                                        define.append("S");
                                }            
                            } catch (Exception ex) {
                                for (String s:header) {
                                    define.append("S");
                                }
                            }
                            cmd = "(\"" + define.toString() + "\";enlist \",\") 0: `:" + remotefile.getName().getPath();
                        }
                        catch (Exception ex) {
                            cmd = "read0 `:" + remotefile.getName().getPath();
                        }
                        doc.insertString(doc.getLength(), cmd, null);
                        executeQuery(cmd);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        exportAction = new UserAction(I18n.getString("Export"),
                                      getImage(Config.imageBase2 + "export2.png"),
                                      "Export result set",
                                      new Integer(KeyEvent.VK_E),
                                      null) {
            public void actionPerformed(ActionEvent e) {
                export();
            }
        };

        chartAction = new UserAction(I18n.getString("Chart"),
                                     Util.getImage(Config.imageBase2 + "chart.png"),
                                     "Chart current data set",
                                     new Integer(KeyEvent.VK_E),
                                     KeyStroke.getKeyStroke(KeyEvent.VK_Q,menuShortcutKeyMask)) {
            public void actionPerformed(ActionEvent e) {
                new LineChart((KTableModel) table.getModel());
            //new PriceVolumeChart(table);
            }
        };


        stopAction = new UserAction(I18n.getString("Stop"),
                                    getImage(Config.imageBase2 + "stop.png"),
                                    "Stop the query",
                                    new Integer(KeyEvent.VK_S),
                                    null) {
            public void actionPerformed(ActionEvent e) {
                if (worker != null) {
                    worker.interrupt();
                    stopAction.setEnabled(false);
                    textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        };


        openInExcel = new UserAction(I18n.getString("OpenInExcel"),
                                     getImage(Config.imageBase + "excel_icon.gif"),
                                     "Open in Excel",
                                     new Integer(KeyEvent.VK_O),
                                     null) {
            
            public void actionPerformed(ActionEvent e) {
                try {
                    File file = File.createTempFile("studioExport",".xls");
                    new ExcelExporter().exportTableX(frame,table,file,true);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };


        executeAction = new UserAction(I18n.getString("Execute"),
                                       Util.getImage(Config.imageBase2 + "table_sql_run.png"),
                                       "Execute the full or highlighted text as a query",
                                       new Integer(KeyEvent.VK_E),
                                       KeyStroke.getKeyStroke(KeyEvent.VK_E,menuShortcutKeyMask)) {
            
            public void actionPerformed(ActionEvent e) {
                executeQuery();
            }
        };


        executeCurrentLineAction = new UserAction(I18n.getString("ExecuteCurrentLine"),
                                                  Util.getImage(Config.imageBase2 + "element_run.png"),
                                                  "Execute the current line as a query",
                                                  new Integer(KeyEvent.VK_ENTER),
                                                  KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,menuShortcutKeyMask)) {
            
            public void actionPerformed(ActionEvent e) {
                executeQueryCurrentLine();
            }
        };


        refreshAction = new UserAction(I18n.getString("Refresh"),
                                       getImage(Config.imageBase2 + "refresh.png"),
                                       "Refresh the result set",
                                       new Integer(KeyEvent.VK_R),
                                       KeyStroke.getKeyStroke(KeyEvent.VK_Y,menuShortcutKeyMask | Event.SHIFT_MASK)) {
            
            public void actionPerformed(ActionEvent e) {
                refreshQuery();
            }
        };

        aboutAction = new UserAction(I18n.getString("About"),
                                     Util.getImage(Config.imageBase2 + "about.png"),
                                     "About Studio for kdb+",
                                     new Integer(KeyEvent.VK_E),
                                     null) {
            
            public void actionPerformed(ActionEvent e) {
                about();
            }
        };

        exitAction = new UserAction(I18n.getString("Exit"),
                                    getImage(Config.imageBase2 + "blank.png"),
                                    "Close this window",
                                    new Integer(KeyEvent.VK_X),
                                    null) {
            
            public void actionPerformed(ActionEvent e) {
                if (quit())
                    System.exit(0);
            }
        };

        codeKxComAction = new UserAction("code.kx.com",
                                         Util.getImage(Config.imageBase2 + "text.png"),
                                         "Open code.kx.com",
                                         new Integer(KeyEvent.VK_C),
                                         null) {
            
            public void actionPerformed(ActionEvent e) {
                    try {
                        BrowserLaunch.openURL("http://code.kx.com/trac/wiki/Reference");
                    } catch (Exception ex) {
                       JOptionPane.showMessageDialog(null, "Error attempting to launch web browser:\n" + ex.getLocalizedMessage());
                    }
            }
        };
    }

    public void about() {
        HelpDialog help = new HelpDialog(frame);
        Util.centerChildOnParent(help,frame);
        // help.setTitle("About Studio for kdb+");
        help.pack();
        help.setVisible(true);
    }

    public boolean quit() {
        boolean okToExit = true;

        Object[] objs = windowList.toArray();

        for (int i = 0;i < objs.length;i++) {
            Object o = objs[i];

            if (o instanceof Studio) {
                if (!((Studio) o).quitWindow())
                    okToExit = false;
            }
            else
                if (o instanceof JFrame) {
                    JFrame f = (JFrame) o;
                    f.setVisible(false);

                    f.dispose();
                }
        }

        return okToExit;
    }

    public boolean quitWindow() {
        if (getModified()) {
            int choice = JOptionPane.showOptionDialog(frame,
                                                      "Changes not saved.\nSave now?",
                                                      "Save changes?",
                                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                                      JOptionPane.QUESTION_MESSAGE,
                                                      getImage(Config.imageBase + "32x32/question.png"),
                                                      null, // use standard button titles
                                                      null);      // no default selection

            if (choice == 0)
                try {
                    String filename = (String) textArea.getDocument().getProperty("filename");
                    if (!saveFile(filename,false))
                        // was cancelled so return
                        return false;
                }
                catch (Exception e) {
                    return false;
                }
            else if ((choice == 2) || (choice == JOptionPane.CLOSED_OPTION))
                return false;
        }

        windowList.remove(this);
        windowListMonitor.removeEventListener(windowListChangedEventListener);
        windowListMonitor.fireMyEvent(new WindowListChangedEvent(this));
        frame.dispose();

        return true;
    }

    private void rebuildMenuBar() {
        menubar = createMenuBar();
        SwingUtilities.invokeLater(
            new Runnable() {
            
                public void run() {
                    if (frame != null) {
                        frame.setJMenuBar(menubar);
                        menubar.validate();
                        menubar.repaint();
                        frame.validate();
                        frame.repaint();
                    }
                }
            });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(I18n.getString("File"));
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(new JMenuItem(newFileAction));
        menu.add(new JMenuItem(openFileAction));
        menu.add(new JMenuItem(saveFileAction));
        menu.add(new JMenuItem(saveAsFileAction));
        menu.add(new JMenuItem(saveRemoteFileAction));
        menu.add(new JMenuItem(closeFileAction));

        menu.addSeparator();
//        menu.add(new JMenuItem(importAction));
        menu.add(new JMenuItem(openInExcel));
        menu.addSeparator();
        menu.add(new JMenuItem(exportAction));
        menu.add(new JMenuItem(loadCSVFileAction));
        menu.addSeparator();
        menu.add(new JMenuItem(chartAction));

        String[] mru = Config.getInstance().getMRUFiles();

        if (mru.length > 0) {
            menu.addSeparator();
            char[] mnems = "123456789".toCharArray();

            for (int i = 0;i < (mru.length > mnems.length ? mnems.length : mru.length);i++) {
                final String filename = mru[i];

                JMenuItem item = new JMenuItem("" + (i + 1) + " " + filename);
                item.setMnemonic(mnems[i]);
                item.setIcon(getImage(Config.imageBase2 + "blank.png"));
                item.addActionListener(new ActionListener() {
                    
                                       public void actionPerformed(ActionEvent e) {
                                           loadMRUFile(filename,(String) textArea.getDocument().getProperty("filename"));
                                       }
                                   });
                menu.add(item);
            }
        }

        if (!MAC_OS_X) {
            menu.addSeparator();
            menu.add(new JMenuItem(exitAction));
        }
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Edit"));
        menu.setMnemonic(KeyEvent.VK_E);
        menu.add(new JMenuItem(undoAction));
        menu.add(new JMenuItem(redoAction));
        menu.addSeparator();
        menu.add(new JMenuItem(cutAction));
        menu.add(new JMenuItem(copyAction));
        menu.add(new JMenuItem(pasteAction));
        menu.addSeparator();
        menu.add(new JMenuItem(selectAllAction));
        menu.addSeparator();
        menu.add(new JMenuItem(findAction));
        menu.add(new JMenuItem(replaceAction));
//        menu.addSeparator();
//        menu.add(new JMenuItem(editFontAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Server"));
        menu.setMnemonic(KeyEvent.VK_S);
        menu.add(new JMenuItem(addServerAction));
        menu.add(new JMenuItem(editServerAction));
        menu.add(new JMenuItem(removeServerAction));

        Server[] servers = Config.getInstance().getServers();
        if (servers.length > 0) {
            JMenu subMenu = new JMenu(I18n.getString("Clone"));
            subMenu.setIcon(Util.getImage(Config.imageBase2 + "data_copy.png"));
           
            for (int i = 0;i < servers.length;i++) {
                final Server s = servers[i];

                JMenuItem item = new JMenuItem(s.getName());
                item.addActionListener(new ActionListener() {
                                        
                                       public void actionPerformed(ActionEvent e) {
                                           Server clone = new Server(s);
                                           clone.setName("Clone of " + clone.getName());

                                           EditServerForm f = new EditServerForm(frame,clone,studio,true);
                                           f.setModal(true);
                                           f.pack();
                                           Util.centerChildOnParent(f,frame);
                                           //   f.setStartLocation(frame);

                                           f.setVisible(true);

                                           if (f.getResult() == DialogResult.ACCEPTED) {
                                               clone = f.getServer();
                                               String oldName = f.getOldName();
                                               actionServer(this, ACTION.CLONE, clone, oldName);
                                           }
                                       }
                                   });

                subMenu.add(item);
            }

            menu.add(subMenu);
        }

        menubar.add(menu);

        menu = new JMenu(I18n.getString("Query"));
        menu.setMnemonic(KeyEvent.VK_Q);
        menu.add(new JMenuItem(executeCurrentLineAction));
        menu.add(new JMenuItem(executeAction));
        menu.add(new JMenuItem(stopAction));
        menu.add(new JMenuItem(refreshAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Window"));
        menu.setMnemonic(KeyEvent.VK_W);

		menu.add(new JMenuItem(minMaxDividerAction));        menu.add(new JMenuItem(toggleDividerOrientationAction));
        menu.add(new JMenuItem(openFileInNewWindowAction));
        menu.add(new JMenuItem(arrangeAllAction));


        if (windowList.size() > 0) {
            menu.addSeparator();

            int i = 0;
            Iterator it = windowList.iterator();

            while (it.hasNext()) {
                String t = "unknown";

                final Object o = it.next();

                if (o instanceof Studio) {
                    Studio r = (Studio) o;
                    String filename = (String) r.textArea.getDocument().getProperty("filename");

                    if (filename != null)
                        t = filename.replace('\\','/');

                    if (r.server != null)
                        t = t + "[" + r.server.getName() + "]";
                    else
                        t = t + "[no server]";
                }
                else
                    if (o instanceof JFrame)
                        t = ((JFrame) o).getTitle();

                JMenuItem item = new JMenuItem("" + (i + 1) + " " + t);
                item.addActionListener(new ActionListener() {
                    
                                       public void actionPerformed(ActionEvent e) {
                                           if (o instanceof Studio) {
                                               JFrame f = ((Studio) o).frame;
                                               ensureDeiconified(f);
                                           }
                                           else
                                               ensureDeiconified((JFrame) o);
                                       }
                                   });

                if (o == this)
                    item.setIcon(getImage(Config.imageBase2 + "check2.png"));
                else
                    item.setIcon(getImage(Config.imageBase2 + "blank.png"));

                menu.add(item);
                i++;
            }
        }
        menubar.add(menu);
        menu = new JMenu(I18n.getString("Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new JMenuItem(codeKxComAction));
        if (!MAC_OS_X)
            menu.add(new JMenuItem(aboutAction));
        menubar.add(menu);

        return menubar;
    }

    private void ensureDeiconified(JFrame f) {
        int state = f.getExtendedState();
        state = state & ~Frame.ICONIFIED;
        f.setExtendedState(state);
        f.show();
    }

    private void rebuildToolbar() {
        if (toolbar != null) {
            toolbar.removeAll();

            String[] names = Config.getInstance().getServerNames();

            if ((names != null) && (names.length > 0)) {
                toolbar.add(new JLabel(I18n.getString("Server")));

                JComboBox combo = new JComboBox(names) {
                    
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }

                    
                    public Dimension getMaximumSize() {
                        return getPreferredSize();
                    }
                };

                int offset = Config.getInstance().getOffset(server);

                if (offset == -1) {
                    Server[] servers = Config.getInstance().getServers();

                    if (servers.length > 0)  {
                        for (int i = 0; i <= servers.length && (i < servers.length || offset == -1); ++i) {
                            try {
                                setServer(servers[i]);
                                offset = i;
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
  
                }

                combo.setSelectedIndex(offset);
                combo.setToolTipText("Select the server context");

                final Observer o = this;

                ActionListener al = new ActionListener() {
                    
                    public void actionPerformed(ActionEvent e) {
                        JComboBox combobox = (JComboBox) e.getSource();
                        String selection = (String) combobox.getSelectedItem();

                        try {
                            setServer(Config.getInstance().getServer(selection));

                        //  setLanguage(Language.Q);

                            SwingUtilities.invokeLater(new Runnable() {
                            
                                                   public void run() {
                                                       rebuildToolbar();
                                                       toolbar.validate();
                                                       toolbar.repaint();
                                                   }
                                               });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            combobox.setSelectedIndex(Config.getInstance().getOffset(server));
                        }
                    }
                };

                combo.addActionListener(al);
                combo.setFocusable(false);
                combo.setRequestFocusEnabled(false);

                toolbar.add(combo);
                toolbar.addSeparator();
            }

            if (server == null) {
                addServerAction.setEnabled(true);
                editServerAction.setEnabled(false);
                removeServerAction.setEnabled(false);
                stopAction.setEnabled(false);
                executeAction.setEnabled(false);
                executeCurrentLineAction.setEnabled(false);
                refreshAction.setEnabled(false);
            }
            else {
                executeAction.setEnabled(true);
                executeCurrentLineAction.setEnabled(true);
                editServerAction.setEnabled(true);
                removeServerAction.setEnabled(true);
            }

            toolbar.add(stopAction);
            toolbar.add(executeAction);
            toolbar.add(refreshAction);
            toolbar.addSeparator();

            toolbar.add(openFileAction);
            toolbar.add(saveFileAction);
            toolbar.add(saveAsFileAction);
            toolbar.addSeparator();
//            toolbar.add(importAction);
            toolbar.add(openInExcel);
            toolbar.addSeparator();
            toolbar.add(exportAction);
            toolbar.add(loadCSVFileAction);
            toolbar.addSeparator();

            toolbar.add(chartAction);
            toolbar.addSeparator();

            toolbar.add(undoAction);
            toolbar.add(redoAction);
            toolbar.addSeparator();

            toolbar.add(cutAction);
            toolbar.add(copyAction);
            toolbar.add(pasteAction);

            toolbar.addSeparator();
            toolbar.add(findAction);

            toolbar.add(replaceAction);

            toolbar.addSeparator();
            toolbar.add(codeKxComAction);

            for (int j = 0;j < toolbar.getComponentCount();j++) {
                Component c = toolbar.getComponentAtIndex(j);

                if (c instanceof JButton)
                    ((JButton) c).setRequestFocusEnabled(false);
            }
        }
    }

    private JToolBar createToolbar() {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        rebuildToolbar();
        return toolbar;
    }

    private static class Impl extends FileView implements 
        LocaleSupport.Localizer {
        // FileView implementation
        
        public String getName(File f) {
            return null;
        }

        
        public String getDescription(File f) {
            return null;
        }

        
        public String getTypeDescription(File f) {
            return null;
        }

        
        public Boolean isTraversable(File f) {
            return null;
        }

        
        public Icon getIcon(File f) {
            if (f.isDirectory())
                return null;
            //     KitInfo ki = KitInfo.getKitInfoForFile(f);
            //   return ki == null ? null : ki.getIcon();
            return null;
        }
        private ResourceBundle bundle;

        public Impl(String bundleName) {
            bundle = ResourceBundle.getBundle(bundleName);
        }
        // Localizer
        
        public String getString(String key) {
            return bundle.getString(key);
        }
    }
    private WindowListChangedEventListener windowListChangedEventListener;

    private int dividerLastPosition; // updated from property change listener
    private void minMaxDivider(){
      //BasicSplitPaneDivider divider = ((BasicSplitPaneUI)splitpane.getUI()).getDivider();
      //((JButton)divider.getComponent(0)).doClick();
      //((JButton)divider.getComponent(1)).doClick();
      if(splitpane.getDividerLocation()>=splitpane.getMaximumDividerLocation()){
        // Minimize editor pane
        splitpane.getTopComponent().setMinimumSize(new Dimension());
        splitpane.getBottomComponent().setMinimumSize(null);
        splitpane.setDividerLocation(0.);
        splitpane.setResizeWeight(0.);
      }
      else if(splitpane.getDividerLocation()<=splitpane.getMinimumDividerLocation()){
        // Restore editor pane
        splitpane.getTopComponent().setMinimumSize(null);
        splitpane.getBottomComponent().setMinimumSize(null);
        splitpane.setResizeWeight(0.);
        // Could probably catch resize edge-cases etc in pce too
        if(dividerLastPosition>=splitpane.getMaximumDividerLocation()||dividerLastPosition<=splitpane.getMinimumDividerLocation())
          dividerLastPosition=splitpane.getMaximumDividerLocation()/2;
        splitpane.setDividerLocation(dividerLastPosition);
      }
      else{
        // Maximize editor pane
        splitpane.getBottomComponent().setMinimumSize(new Dimension());
        splitpane.getTopComponent().setMinimumSize(null);
        splitpane.setDividerLocation(splitpane.getOrientation()==JSplitPane.VERTICAL_SPLIT?splitpane.getHeight()-splitpane.getDividerSize():splitpane.getWidth()-splitpane.getDividerSize());
        splitpane.setResizeWeight(1.);
      }
    }
    private void toggleDividerOrientation() {
        if (splitpane.getOrientation() == JSplitPane.VERTICAL_SPLIT)
            splitpane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        else
            splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        splitpane.setDividerLocation(0.5);
    }

    public Studio(Server server,String filename) {
        super(true);
        try {
            writer = new PrintWriter(new FileWriter("console.out", true), true);
        } catch (Exception ex) {}
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        int s = SettingsDefaults.defaultFont.getSize();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                javax.swing.plaf.FontUIResource f0 = (javax.swing.plaf.FontUIResource) value;
                javax.swing.plaf.FontUIResource f2 = new FontUIResource(f0.getName(),f0.getStyle(),s);
                UIManager.put(key, f2);
            }
        }
        registerForMacOSXEvents();

        windowListChangedEventListener = new WindowListChangedEventListener() {
            
            public void WindowListChangedEventOccurred(WindowListChangedEvent evt) {
                rebuildMenuBar();
                rebuildToolbar();
            }
        };

        windowListMonitor.addEventListener(windowListChangedEventListener);

        splitpane = new JSplitPane();
        frame = new JFrame();
        windowList.add(this);

        initDocument();
        this.server = server;

        menubar = createMenuBar();
        toolbar = createToolbar();

        tabbedPane = new JTabbedPane();
        splitpane.setBottomComponent(tabbedPane);
        splitpane.setOneTouchExpandable(true);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        try {
            Component divider = ((BasicSplitPaneUI) splitpane.getUI()).getDivider();

            divider.addMouseListener(new MouseAdapter() {
                
                                     public void mouseClicked(MouseEvent event) {
                                         if (event.getClickCount() == 2)
                                             toggleDividerOrientation();
                                     }
                                 });
        }
        catch (ClassCastException e) {
        }
        splitpane.setContinuousLayout(true);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        frame.setJMenuBar(menubar);

        if (filename != null)
            loadFile(filename);
        else
            myScriptNumber = scriptNumber++;

        refreshFrameTitle();

        frame.getContentPane().add(toolbar,BorderLayout.NORTH);
        frame.getContentPane().add(splitpane,BorderLayout.CENTER);
        // frame.setSize(frame.getContentPane().getPreferredSize());

        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);
        frame.setSize((int) (0.8 * screenSize.width),
                      (int) (0.8 * screenSize.height));

        frame.setLocation(((int) Math.max(0,(screenSize.width - frame.getWidth()) / 2.0)),
                          (int) (Math.max(0,(screenSize.height - frame.getHeight()) / 2.0)));

        frame.setIconImage(getImage(Config.imageBase + "32x32/dot-chart.png").getImage());

        //     frame.pack();
        frame.setVisible(true);
        splitpane.setDividerLocation(0.5);

        textArea.requestFocus();
        splitpane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,new PropertyChangeListener(){
          public void propertyChange(PropertyChangeEvent pce){
            String s=splitpane.getDividerLocation()>=splitpane.getMaximumDividerLocation()?I18n.getString("MinimizeEditorPane"):splitpane.getDividerLocation()<=splitpane.getMinimumDividerLocation()?I18n.getString("RestoreEditorPane"):I18n.getString("MaximizeEditorPane");
            minMaxDividerAction.putValue(Action.SHORT_DESCRIPTION,s);
            minMaxDividerAction.putValue(Action.NAME,s);
            if(splitpane.getDividerLocation()<splitpane.getMaximumDividerLocation()&&splitpane.getDividerLocation()>splitpane.getMinimumDividerLocation())
              dividerLastPosition=splitpane.getDividerLocation();
          }
        });
        dividerLastPosition=splitpane.getDividerLocation();    }

    public void update(Observable obs,Object obj) {
    }
    public static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));
    private static boolean registeredForMaxOSXEvents = false;

    public void registerForMacOSXEvents() {
        if (registeredForMaxOSXEvents)
            return;

        if (MAC_OS_X)
            try {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(new QuitHandler(this),QuitHandler.class.getDeclaredMethod("quit",(Class[]) null));
                OSXAdapter.setAboutHandler(new AboutHandler(this),AboutHandler.class.getDeclaredMethod("about",(Class[]) null));
                registeredForMaxOSXEvents = true;
            }
            catch (Exception e) {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
    }

    public static void init(String[] args) {
        try {
            String filename = null;

            String[] mruFiles = Config.getInstance().getMRUFiles();
            if(args.length>0){
                File f=new File(args[0]);
                if(f.exists())
                    filename=args[0];
            } else if (mruFiles.length > 0) {
                File f = new File(mruFiles[0]);
                if (f.exists())
                    filename = mruFiles[0];
            }

            Locale.setDefault(Locale.US);

            Server s = null;
            String lruServer = Config.getInstance().getLRUServer();
            if (lruServer != null)
                s = Config.getInstance().getServer(lruServer);

            new Studio(s,filename);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshQuery() {
        table = null;
        executeK4Query(lastQuery);
    }

    public void executeQueryCurrentLine() {
        if (textArea.getSelectedText() == null)  {
            executeQuery(getCurrentLineEditorText(textArea));
        } else {
            executeQuery(getEditorText(textArea));
        }
    }

    public void executeQuery() {
        executeQuery(getEditorText(textArea));
    }

    private void executeQuery(String text) {
        table = null;

        if (text == null) {
            JOptionPane.showMessageDialog(frame,
                                          "\nNo text available to submit to server.\n\n",
                                          "Studio for kdb+",
                                          JOptionPane.OK_OPTION,
                                          getImage(Config.imageBase + "32x32/information.png"));

            return;
        }

        refreshAction.setEnabled(false);
        stopAction.setEnabled(true);
        executeAction.setEnabled(false);
        executeCurrentLineAction.setEnabled(false);
        exportAction.setEnabled(false);
        chartAction.setEnabled(false);
        openInExcel.setEnabled(false);

        executeK4Query(text);

        lastQuery = text;
    }

    private String getEditorText(JEditorPane editor) {
        String text = editor.getSelectedText();

        if (text != null) {
            if (text.length() > 0)
                if (text.trim().length() == 0)
                    return null; // selected text is whitespace
        }
        else
            text = editor.getText(); // get the full text then

        if (text != null)
            text = text.trim();

        if (text.trim().length() == 0)
            text = null;

        return text;
    }

    private String getCurrentLineEditorText(JEditorPane editor) {
        String newLine = "\n";
        String text = null;

        try {
            int pos = editor.getCaretPosition();
            int max = editor.getDocument().getLength();


            if ((max > pos) && (!editor.getText(pos,1).equals("\n"))) {
                String toeol = editor.getText(pos,max - pos);
                int eol = toeol.indexOf('\n');

                if (eol > 0)
                    pos = pos + eol;
                else
                    pos = max;
            }

            text = editor.getText(0,pos);

            int lrPos = text.lastIndexOf(newLine);

            if (lrPos >= 0) {
                lrPos += newLine.length(); // found it so skip it
                text = text.substring(lrPos,pos).trim();
            }
        }
        catch (BadLocationException e) {
        }

        if (text != null) {
            text = text.trim();

            if (text.length() == 0)
                text = null;
        }

        return text;
    }

    private void processK4Results(K.KBase r) throws c.K4Exception {
        if (r != null) {
            exportAction.setEnabled(true);

            if (FlipTableModel.isTable(r)) {
                QGrid grid = new QGrid(r);
                table = grid.getTable();

                openInExcel.setEnabled(true);
                //if(grid.getRowCount()<50000)
                chartAction.setEnabled(true);
                //else
                //    chartAction.setEnabled(false);              

                TabPanel frame = new TabPanel("Table [" + grid.getRowCount() + " rows] ",
                                              getImage(Config.imageBase2 + "table.png"),
                                              grid);
                frame.setTitle(I18n.getString("Table")+" [" + grid.getRowCount() + " "+I18n.getString("rows")+"] ");
//                frame.setBackground( Color.white);

                tabbedPane.addTab(frame.getTitle(),frame.getIcon(),frame.getComponent());
            }
            else {
                chartAction.setEnabled(false);
                openInExcel.setEnabled(false);
                LimitedWriter lm = new LimitedWriter(50000);
                try {
					if(!(r instanceof K.UnaryPrimitive&&0==((K.UnaryPrimitive)r).getPrimitiveAsInt()))                    r.toString(lm,true);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
                catch (LimitedWriter.LimitException ex) {
                }

                JEditorPane pane = new JEditorPane("text/plain",lm.toString());
                pane.setFont(font);

//pane.setLineWrap( false);
//pane.setWrapStyleWord( false);

                JScrollPane scrollpane = new JScrollPane(pane,
                                                         ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                TabPanel frame = new TabPanel("Console View ",
                                              getImage(Config.imageBase2 + "console.png"),
                                              scrollpane);

                frame.setTitle(I18n.getString("ConsoleView"));

                tabbedPane.addTab(frame.getTitle(),frame.getIcon(),frame.getComponent());
            }
        }
        else {
            // Log that execute was successful
        }
    }
    Server server = null;

      public void executeK4Query(final String text) {
        final Cursor cursor = textArea.getCursor();

        textArea.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        tabbedPane.removeAll();
        worker = new SwingWorker() {
            Server s = null;
            c c = null;
            K.KBase r = null;
            Throwable exception;
            boolean cancelled = false;
            long execTime=0;
            public void interrupt() {
                super.interrupt();

                cancelled = true;

                if (c != null)
                    c.close();
                cleanup();
            }

            public Object construct() {
                try {
                    this.s = server;
                    c = ConnectionPool.getInstance().leaseConnection(s);
                    ConnectionPool.getInstance().checkConnected(c);
                    c.setFrame(frame);
                    long startTime=System.currentTimeMillis();
                    c.k(new K.KCharacterVector(text));
                    r = c.getResponse();
                    execTime=System.currentTimeMillis()-startTime;
                }
                catch (Throwable e) {
                    exception = e;
                }

                return null;
            }

            public void finished() {
                if (!cancelled) {
                    if (exception != null)
                        try {
                            throw exception;
                        }
                        catch (IOException ex) {
                            JOptionPane.showMessageDialog(frame,
                                                          "\nA communications error occurred whilst sending the query.\n\nPlease check that the server is running on " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + ex.getMessage() + "\n\n",
                                                          "Studio for kdb+",
                                                          JOptionPane.ERROR_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/error.png"));
                        }
                        catch (c.K4Exception ex) {
                            JTextPane pane = new JTextPane();
                            String hint = QErrors.lookup(ex.getMessage());
                            if (hint != null)
                                hint = "\nStudio Hint: Possibly this error refers to " + hint;
                            else
                                hint = "";
                            pane.setText("An error occurred during execution of the query.\nThe server sent the response:\n" + ex.getMessage() + hint);
                            pane.setForeground(Color.RED);

                            JScrollPane scrollpane = new JScrollPane(pane);

                            TabPanel frame = new TabPanel("Error Details ",
                                                          getImage(Config.imageBase2 + "error.png"),
                                                          scrollpane);
                            frame.setTitle("Error Details ");

                            tabbedPane.addTab(frame.getTitle(),frame.getIcon(),frame.getComponent());

                        //            tabbedPane.setSelectedComponent(resultsTabbedPane);
                        }
                        catch (java.lang.OutOfMemoryError ex) {
                            JOptionPane.showMessageDialog(frame,
                                                          "\nOut of memory whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nThe result set is probably too large.\n\nTry increasing the memory available to studio through the command line option -J -Xmx512m\n\n",
                                                          "Studio for kdb+",
                                                          JOptionPane.ERROR_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/error.png"));
                        }
                        catch (Throwable ex) {
                            String message = ex.getMessage();

                            if ((message == null) || (message.length() == 0))
                                message = "No message with exception. Exception is " + ex.toString();

                            JOptionPane.showMessageDialog(frame,
                                                          "\nAn unexpected error occurred whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + message + "\n\n",
                                                          "Studio for kdb+",
                                                          JOptionPane.ERROR_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/error.png"));
                        }
                    else
                        try {
                            Utilities.setStatusText(textArea, "Last execution time:"+(execTime>0?""+execTime:"<1")+" mS");
                            processK4Results(r);
                        }
                        catch (Exception e) {
                            JOptionPane.showMessageDialog(frame,
                                                          "\nAn unexpected error occurred whilst communicating with " + server.getHost() + ":" + server.getPort() + "\n\nError detail is\n\n" + e.getMessage() + "\n\n",
                                                          "Studio for kdb+",
                                                          JOptionPane.ERROR_MESSAGE,
                                                          getImage(Config.imageBase + "32x32/error.png"));
                        }

                    cleanup();
                }
            }

            private void cleanup() {
                if (c != null)
                    ConnectionPool.getInstance().freeConnection(s,c);
                //if( c != null)
                //    c.close();
                c = null;

                textArea.setCursor(cursor);

                stopAction.setEnabled(false);
                executeAction.setEnabled(true);
                executeCurrentLineAction.setEnabled(true);
                refreshAction.setEnabled(true);

                System.gc();

                worker = null;
            }
        };

        worker.start();
    }
    private SwingWorker worker;
    
    public void windowClosing(WindowEvent e) {
        if (quitWindow())
            if (windowList.size() == 0)
                System.exit(0);
    }

    
    public void windowClosed(WindowEvent e) {
    }

    
    public void windowOpened(WindowEvent e) {
    }
    // ctrl-alt spacebar to minimize window
    
    public void windowIconified(WindowEvent e) {
    }

    
    public void windowDeiconified(WindowEvent e) {
    }

    
    public void windowActivated(WindowEvent e) {
        this.invalidate();
        SwingUtilities.updateComponentTreeUI(this);
    }

    
    public void windowDeactivated(WindowEvent e) {
    }

    private class MarkingDocumentListener implements DocumentListener {
        private boolean modified = false;

        private void setModified(boolean b) {
            modified = b;
        }

        private boolean getModified() {
            return modified;
        }
        private Component comp;

        public MarkingDocumentListener(Component comp) {
            this.comp = comp;
        }

        private void markChanged(DocumentEvent evt) {
            setModified(true);
            refreshFrameTitle();
        }

        
        public void changedUpdate(DocumentEvent e) {
        }

        
        public void insertUpdate(DocumentEvent evt) {
            markChanged(evt);
        }

        
        public void removeUpdate(DocumentEvent evt) {
            markChanged(evt);
        }
        /** Document property holding String name of associated file */
        private static final String FILE = "file";
        /** Document property holding Boolean if document was created or opened */
        private static final String CREATED = "created";
        /** Document property holding Boolean modified information */
        private static final String MODIFIED = "modified";
    }

    public static ImageIcon getImage(String strFilename) {
        Class thisClass = Studio.class;

        java.net.URL url = null;

        if (strFilename.startsWith("/"))
            url = thisClass.getResource(strFilename);
        else
            // Locate the desired image file and create a URL to it
            url = thisClass.getResource("/toolbarButtonGraphics/" + strFilename);

        // See if we successfully found the image
        if (url == null)
            //writer.println("Unable to load the following image: " +
            //                 strFilename);
            return null;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.getImage(url);
        return new ImageIcon(image);
    }
}