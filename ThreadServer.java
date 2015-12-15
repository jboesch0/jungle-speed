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

                if (game.players.size() == 0){
                    ok = true;
                } else {
                    for (Player p : game.players){
                        if (p.name.equals(pseudo)){
                            ok = false;
                            break;
                        } else {
                            ok = true;
                        }
                    }
                }


                if (ok){
                    player = new Player(pseudo);
                    game.players.add(player);
                    oos.writeBoolean(true);
                    ok = true;
                } else {
                    oos.writeBoolean(false);
                    ok = true;
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
            System.err.println("problem with client connection. Aborting (ThreadServer ");
            return;
        }
        catch(ClassNotFoundException e) {
            System.err.println("problem with client request. Aborting (ThreadServer)");
            return;
        }
        try {
            while (true) {
                initLoop();
                oos.writeBoolean(true); // synchro signals so that thread client does not sends to quickly a request
                oos.flush();
                try {
                    System.out.println("Party Loop");
                    partyLoop();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
            System.err.println("client sent an illegal request: (ThreadServer)"+e.getMessage());
        }
        catch(IOException e) {
            System.err.println("pb with client connection: (ThreadServer)"+e.getMessage());
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
                    stop = true;
                    break;
                case 2:
                    requestCreateParty();
                    stop = true;
                    break;
                case 3: requestJoinParty();
                    stop = true;
                    break;
                default:
                    throw new IllegalRequestException("Requete illegale (ThreadServer)");
            }
            // recevoir n° requete
            // si n° correspond à LIST PARTY, CREATE PARTY ou JOIN PARTY appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void partyLoop() throws IllegalRequestException,IOException, InterruptedException  {

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
                    throw new IllegalRequestException("Requete illegale (ThreadServer)");
            }
            // si etat partie == fin, retour
            // recevoir n° requete
            // si etat partie == fin, retour
            // si n° req correpsond à WAIT PARTY STARTS, WAIT TURN STARTS, PLAY, appeler la méthode correspondante
            // sinon générer une exception IllegalRequest
        }
    }

    public void requestListParty() throws IOException,IllegalRequestException {
        String nomParty;
        try {
            System.out.println(game.parties.size());
            if (game.parties.size() == 0){
                nomParty = "Pas de parties";
            } else{
                List<Party> parties = game.parties;
                nomParty = "";

                for (int i = 0, partiesSize = parties.size(); i < partiesSize; i++) {
                    Party p = parties.get(i);
                    nomParty += "Partie n° " + i + p.name + " createur " + p.creator.name +"\n";
                }
            }
            oos.writeObject(nomParty);
            oos.flush();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public boolean requestCreateParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de créer une nouvelle partie.
        try {
            String nomParty = (String) ois.readObject();
            int nbJoueurs = ois.readInt();
            Party party = game.createParty(nomParty ,player, nbJoueurs);
            party.pool.addStream(player.id, oos);
            currentParty = party;
            rep = true;

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // traiter requete CREATE PARTY (sans oublier les cas d'erreur)
        // NB : ne pas oublier d'ajouter le flux oos au pool de la partie créée
        //oos.writeInt(JungleServer.REQ_WAITPARTYSTARTS);
        //oos.flush();
        return rep;
    }

    public boolean requestJoinParty() throws IOException,IllegalRequestException {

        boolean rep = false; // mis a true si la requête permet effectivement de rejoindre une partie existante

        // traiter requete JOIN PARTY (sans oublier les cas d'erreur)
        int numParty = (int) ois.readInt();
        Party party = game.parties.get(numParty);

        if (game.playerJoinParty(player, game.parties.get(numParty))){
            party.pool.addStream(player.id, oos);
            rep = true;
        }
        return rep;
    }

    public void requestWaitPartyStarts() throws IOException,IllegalRequestException {

        try {
            currentParty.waitForPartyStarts();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        oos.writeInt(player.id);
        // traiter requete WAIT PARY STARTS (sans oublier les cas d'erreur)

    }

    public void requestWaitTurnStarts() throws IOException, IllegalRequestException, InterruptedException {

        Player currentPlayer;
        currentParty.waitForTurnStarts();
        currentPlayer = currentParty.currentPlayer;
        if (currentParty.state == Party.PARTY_END){
            oos.writeInt(-1);
        } else {
            oos.writeInt(currentPlayer.id);
        }
        if (currentParty.currentPlayer == player){
            wait(3000);
            currentParty.revealCard();
            Object visibles = currentParty.getCurrentCards();
            currentParty.setCurrentState(Party.PARTY_MUSTPLAY);
            currentParty.pool.sendToAll(visibles);
        }
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

        try {
            String ordreClient = (String) ois.readObject();
            switch (ordreClient){
                case "TT":
                    idAction = JungleServer.ACT_TAKETOTEM;
                    break;
                case  "TH":
                    idAction = JungleServer.ACT_HANDTOTEM;
                    break;
                case "":
                    idAction = JungleServer.ACT_NOP;
                    break;
                default:
                    idAction = JungleServer.ACT_INCORRECT;
                    break;
            }
            lastPlayed = currentParty.integratePlayerOrder(player, idAction);
            if (lastPlayed){
                if (currentParty.state == 3){
                    currentParty.pool.sendToAll("Partie finie");
                    oos.writeBoolean(true);
                    return;
                }
                currentParty.analyseResults();
                String resultMsg = currentParty.resultMsg;
                currentParty.pool.sendToAll(resultMsg);
                if (currentParty.state == 3){
                    oos.writeBoolean(true);
                } else {
                    oos.writeBoolean(false);
                }
            }



        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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
