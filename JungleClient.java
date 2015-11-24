/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */


import java.io.*;
import java.net.*;
import javax.swing.*;

class JungleClient  {

    public static void main(String []args) {

        if (args.length != 0) {
            System.err.println("usage: JungleClient");
            System.exit(1);
        }

        javax.swing.SwingUtilities.invokeLater( new Runnable() {
            public void run() {

                JungleIG ig = new JungleIG();
            }
        });
    }
}
		
