/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */

import java.util.*;
import java.io.*;
import javax.swing.*;

class ThreadClient extends Thread {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    JungleIG ig;
    ObjectInputStream ois;
    ObjectOutputStream oos;

    public ThreadClient(JungleIG ig) {
        this.ig = ig;
        ois = ig.ois;
        oos = ig.oos;
    }

    public void run() {

        boolean stop = false;

        try {
            // recevoir booléen qui signale que le serveur est prêt
            // envoyer requête "attendre début partie"
            // recevoir mon id dans la partie

            while (!stop) {

                // envoyer requête "attendre début tour"
                // recevoir id joueur courant
                // si id joueur courant < 0, arreter thread
                // sinon si id joueur courant == mon id : afficher message dans ig du type "c'est mon tour"
                // sinon afficher message dans ig du type "c'est le tour de X"

                // recevoir la liste des cartes visibles et les afficher dans l'ig
                // débloquer le champ de saisie+bouton jouer
                // attendre 3s
                // bloquer le champ de saisie+bouton jouer

                // si pas d'rodre envoyé pdt les 3s
                //    envoyer requête PLAY avec comme paramètre chaîne vide

                // recevoir résultat du tour et l'afficher dans l'IG
                // recevoir booléen = true si partie finie, false sinon
            }
        }
        catch(IOException e) {}
        catch(ClassNotFoundException e) {}
	JOptionPane.showMessageDialog(null, "Party is over. Return to main panel");
	ig.setInitPanel();
    }

}
