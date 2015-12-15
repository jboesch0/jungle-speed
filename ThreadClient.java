/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */

import com.sun.xml.internal.bind.v2.TODO;

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
            stop = ois.readBoolean();
            oos.writeInt(JungleServer.REQ_WAITPARTYSTARTS);
            int idJoueur = ois.readInt();


            // recevoir booléen qui signale que le serveur est prêt
            // envoyer requête "attendre début partie"
            // recevoir mon id dans la partie

            while (!stop) {

                oos.writeInt(5);
                int idJoueurCourant = ois.readInt();
                if (idJoueurCourant < 0){
                    interrupt();
                } else if(idJoueurCourant == idJoueur) {
                    ig.textInfoParty.add(new JLabel("C'est mon tour"));
                } else {
                    ig.textInfoParty.add(new JLabel("C'est au tour de " + idJoueur));
                }


                // envoyer requête "attendre début tour"
                // recevoir id joueur courant
                // si id joueur courant < 0, arreter thread
                // sinon si id joueur courant == mon id : afficher message dans ig du type "c'est mon tour"
                // sinon afficher message dans ig du type "c'est le tour de X"

                String visibles = (String) ois.readObject();
                ig.textInfoParty.add(new JLabel(visibles));
                ig.textPlay.setEnabled(true);
                ig.butPlay.setEnabled(true);

                wait(3000);

                boolean ordre = ig.orderSent;
                ig.textPlay.setEnabled(false);
                ig.butPlay.setEnabled(false);

                if (!ordre){
                    oos.writeInt(JungleServer.REQ_PLAY);
                    oos.writeObject("");
                }

                String resultTour = (String) ois.readObject();
                ig.textInfoParty.append(resultTour);
                stop = ois.readBoolean();


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
        catch(ClassNotFoundException e) {} catch (InterruptedException e) {
            e.printStackTrace();
        }
        JOptionPane.showMessageDialog(null, "Party is over. Return to main panel");
	ig.setInitPanel();
    }

}
