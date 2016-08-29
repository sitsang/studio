package studio.ui;

import studio.kdb.Server;
import javax.swing.JFrame;

public class EditServerForm extends ServerForm {
    public EditServerForm(JFrame owner,Server server,Studio studio, Boolean isClone) {
        super(owner,server,studio,isClone);
        this.setTitle("Edit Server Details");
    }
}
