/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */


import java.io.*;
import java.net.*;
import javax.swing.*;

class JungleClient  {

    public static void main(String []args) {



        javax.swing.SwingUtilities.invokeLater( new Runnable() {
            public void run() {

                JungleIG ig = new JungleIG();
            }
        });
    }
}
		
