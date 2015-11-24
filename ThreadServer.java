/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */

import java.util.*;
import java.io.*;
import java.net.*;

class ThreadServer extends Thread {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    Socket comm;
    Game game;
    Player player;
    Party currentParty; // if null, player didn't join yet a party
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public ThreadServer(Game game, Socket comm) {
        this.game = game;
        this.comm = comm;
        currentParty = null;
    }

    public void run() {

        String pseudo;
        boolean ok = false;

        System.out.println("client connected.");
        try {
            ois = new ObjectInputStream(comm.getInputStream());
            oos = new ObjectOutputStream(comm.getOutputStream());

            while (!ok){
                pseudo = (String) ois.readObject();
                for (Player p : game.players){
                    if (p.name.equals(pseudo)){
                        ok = false;
                        break;
                    } else {
                        ok = true;
                    }
                }

                if (ok){
                    player = new Player(pseudo);
                    oos.writeBoolean(true);
                } else {
                    oos.writeBoolean(false);
                }
                oos.flush();

            }
            // initialisation des flux objet
            // tant que pas ok
            //    recevoir pseudo
            //    si pseudo n'existe pas
            //       créer joueur (cf. classe Game) -> player
            //       envoyer true au client
            //    sinon envoyer false au client
        }
        catch(IOException e) {
            System.err.println("problem with client connection. Aborting");
            return;
        }
        catch(ClassNotFoundException e) {
            System.err.println("problem with client request. Aborting");
            return;
        }

        try {
            while (true) {
                initLoop();
                oos.writeBoolean(true); // synchro signals so that thread client does not sends to quickly a request
                oos.flush();
                partyLoop();
                currentParty.pool.removeStream(player.id);
                boolean ret = currentParty.removePlayer(player);
                if (ret){
                    if (currentParty.nbPlayerInParty == 0){
                        game.removeParty(currentParty);
                    }
                }
                // supprimer le flux sortant associé à player du pool de la partie courante
                // ret = supprimer player de la partie courante
                // si ret == true (i.e. dernier joueur de la partie) supprimer la partie
            }
        }
        catch(IllegalRequestException e) {
            System.err.println("client sent an illegal request: "+e.getMessage());
        }
        catch(IOException e) {
            System.err.println("pb with client connection: "+e.getMessage());
        }
        // NB : si on arrive ici, c'est qu'il y a eu déconnexion ou erreur de requête


        if (currentParty != null){
            if (currentParty.getCurrentState() != 0){
                currentParty.state = 3;
            }
            currentParty.pool.removeStream(player.id);
            boolean ret = currentParty.removePlayer(player);
            if (ret){
                game.removeParty(currentParty);
            }
        }
        game.removePlayer(player);

        // si partie courante != null (i.e. le joueur s'est déconnnecté en pleine partie)
        //    si l'état partie != en attente, etat partie = fin
        //    supprimer le flux sortant associé à player du pool de la partie courante
        //    ret = supprimer player de la partie courante
        //    si ret == true (i.e. dernier joueur de la partie) supprimer la partie
        // fsi
        // supprimer le joueur de game
    }

    public void initLoop() throws IllegalRequestException,IOException  {

        int idReq;
        boolean stop = false; // devient true en cas de requête CREATE ou JOIN réussie
        while (!stop) {
             idReq = ois.readInt();
            switch (idReq){
                case 1:
                    requestListParty();
                    break;
                case 2:
                    requestCreateParty();
                    break;
                case 3: requestJoinParty();
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale");
            }
            // recevoir n° requete
            // si n° correspond à LIST PARTY, CREATE PARTY ou JOIN PARTY appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void partyLoop() throws IllegalRequestException,IOException  {

        int idReq;

        while (true) {

            if (currentParty.state == 3){
                return;
            }
            idReq = ois.readInt();
            if (currentParty.state == 3){
                return;
            }
            switch (idReq){
                case 4:
                    requestWaitPartyStarts();
                    break;
                case 5:
                    requestWaitTurnStarts();
                    break;
                case 6:
                    requestPlay();
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale");
            }
            // si etat partie == fin, retour
            // recevoir n° requete
            // si etat partie == fin, retour
            // si n° req correpsond à WAIT PARTY STARTS, WAIT TURN STARTS, PLAY, appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void requestListParty() throws IOException,IllegalRequestException {
        try {
            List<Party> parties = game.parties;
            String nomParty = "";
            for (Party p : parties){
                nomParty += "-" + p.name + "\n";
            }
            oos.writeObject(nomParty);
            oos.flush();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean requestCreateParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de créer une nouvelle partie.




        // traiter requete CREATE PARTY (sans oublier les cas d'erreur)

        // NB : ne pas oublier d'ajouter le flux oos au pool de la partie créée

        return rep;
    }

    public boolean requestJoinParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de rejoindre une partie existante

        // traiter requete JOIN PARTY (sans oublier les cas d'erreur)

        // NB : ne pas oublier d'ajouter le flux oos au pool de la partie rejointe
        return rep;
    }

    public void requestWaitPartyStarts() throws IOException,IllegalRequestException {

        // traiter requete WAIT PARY STARTS (sans oublier les cas d'erreur)

    }

    public void requestWaitTurnStarts() throws IOException,IllegalRequestException {

        Player currentPlayer;
        // traiter cas d'erreur
        // attendre début tour
        // récupérer le joueur courant dans le tour -> currentPlayer
        // si etat partie == fin, envoyer -1 au client sinon envoyer id joueur courant
        // si je suis le thread associé au joueur courant
        //    faire dodo entre 1 et 3s
        //    révéler une carte
        //    obtenir la liste des cartes visibles
        //    mettre état partie à "joueur doivent jouer".
        //    envoyer cette liste à tous les clients (grâce au pool)
        // fsi
    }

    public void requestPlay() throws IOException,IllegalRequestException {

        String action = "";
        int idAction = -1;
        boolean lastPlayed = false;

        // traiter cas d'erreur

        // recevoir la String qui indique l'ordre envoyé par le client
        // en fonction de cette String, initialiser idAction à ACT_TAKETOTEM ou ACT_HANDTOTEM ou ACT_NOP ou ACT_INCORRECT
        // lastPlayed <- intégrer l'ordre donné par le joueur (cf. integratePlayerOrder() )
        // si lastPLayer vaut true
        //    si etat partie == fin
        //       envoyer un message du style "partie finie" à tous les client
        //       envoyer true (= fin de partie) puyis retour
        //    fsi
        //    analyser les résultats
        //    envoyer resultMsg de la partie courante à tous les clients
        //    si etat partie == fin, envoyer true, sinon envoyer false
        // fsi
    }
}
