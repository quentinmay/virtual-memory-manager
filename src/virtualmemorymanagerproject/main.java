
package virtualmemorymanagerproject;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public class main {

    
    public static void main(String[] args) {
        memmgr manager = new memmgr();
        try {
            manager.run();
        } catch (IOException ex) {
            Logger.getLogger(main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
