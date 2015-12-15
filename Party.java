/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */

import java.util.*;

class Party {

    private static Random loto = new Random(Calendar.getInstance().getTimeInMillis());

    public final static int PARTY_WAITING = 0; // waiting for sufficent number of player join the party
    public final static int PARTY_ONGOING = 1; // party begun nut not in special state (see below)
    public final static int PARTY_MUSTPLAY = 2; // players must play sthg
    public final static int PARTY_END = 3;

    public final static int RES_ERROR = -2; // represents an error of a player
    public final static int RES_LOST = -1; // represents the fact taht a player has lost (not an error) the current turn
    public final static int RES_NULL = 0; // represents the fact that nothing happens to a player during the current turn
    public final static int RES_WIN = 1; // represents the fact that a player won the current turn

    String name; // the name of the party (as given by the creator
    Player creator; // the creator of the party
    int nbNeededPlayers; // the needed number of players for this party
    ArrayList<Player> players; // in their order of arrival in the party
    OutputStreamPool pool; // the pool of output streams taht can be used to send msg to all clients
    int nbPlayerInParty; // number of players that begun the party. should always be the same as players.size()



    Semaphore semBeginTurn; // barrier to synchronize thread at the begining of a turn.
    /* NOTE :
      in order to manage the party correctly and notably, if it's over, state attribute
      represents one of the four possible states :
         0 = waiting for players,
         1 = in progress,
         2 = players must choose an action (take, hand or nothing)
         3 = end of party (winner or disconnection)
     */
    int state;
    CardPacket allCards; // the packet of all cards, before it is distributed to players.
    List<Card> underTotem; // list of cards currently under the totem

    int nbPlayerInTurn; // number of players that begun the current turn
    Player currentPlayer; // the player who reveals its card during the current turn
    Player playerOfNextTurn; // the player of the next turn
    Card lastRevealedCard; // the card revealed by the player during the current turn
    boolean totemTaken; // becomes true as soon as the totem is taken by a player during the current turn
    boolean totemHand; // becomes true as soon as a player is the first to put its hand on the totem during the current turn
    List<Player> played; // the list of players that played during the current turn
    List<Integer> result; // the result of the order of players.
    String resultMsg; // message sent at the end of the turn

    public Party(String name, Player creator, int nbNeededPlayers) {
        this.name = name;
        this.creator = creator;
        this.nbNeededPlayers = nbNeededPlayers;
        allCards = new CardPacket(nbNeededPlayers);
        players = new ArrayList<Player>();
        players.add(creator); // the creator of the party is the first player of the party
        nbPlayerInParty = 1;
        List<Card> heap = allCards.takeXFirst(12);
        creator.joinParty(1,heap);
        underTotem = new ArrayList<Card>();
        played = new ArrayList<Player>();
        result = new ArrayList<Integer>();
        pool = new OutputStreamPool();
        state = PARTY_WAITING;
        currentPlayer = null;
        playerOfNextTurn = null;
        semBeginTurn = new Semaphore(0);
    }


    public synchronized void addPlayer(Player p) {
        System.out.println("avant if addPlayer");
        if (!(players.contains(p)) && (nbPlayerInParty < nbNeededPlayers)){
            System.out.println("dans if addPlayer");
            players.add(p);
            nbPlayerInParty++;
            List<Card> paquet = allCards.takeXFirst(12);
            p.joinParty(nbPlayerInParty, paquet);
            if (nbPlayerInParty == nbNeededPlayers){
                playerOfNextTurn = players.get(0);
                nbPlayerInTurn = 0;
                initNewTurn();
                state = PARTY_ONGOING;
                notifyAll();
            }
        }
        // si p n'est pas déjà dans cette partie ET le nb de joueurs < nb joueurs nécessaires :
        //    ajouter p à players
        //    incrémenter le nb de joueurs dans la partie
        //    obtenir 12 cartes de allCards
        //    donner à p son id et son pacquet (cf. joinParty() de Player)
        //    si nb joueur == nb joueurs nécessaires
        //        joueur prochain tour = premier joueur de players
        //        nb joueurs dans le tour = 0
        //        initialiser un nouveau tour
        //        changer état partie à "en cours"
        //        réveiller les thread en attente
        //    fsi
        // fsi
    }

    public synchronized boolean removePlayer(Player p) {
        players.remove(p);
        nbPlayerInParty--;
        currentPlayer.id = -1;
        semBeginTurn.put(1);
        if (nbPlayerInParty == 0 ){
            return true;
        } else {
            return false;
        }
        // supprimer p de players
        // décrémenter nb joueur dans la partie
        // remettre id de player à -1 (= pas dans une partie)
        // mettre des jetons dans le sémaphore (au cas où des threads soient bloqués dans la barrière de début de tour)
        // si nb joueurs dans partie == 0, renvoyer vrai sinon renvoyer false
    }

    public synchronized void waitForPartyStarts() throws InterruptedException {
        while (state == 0 && nbNeededPlayers != nbPlayerInParty){
            wait();
        }
        // tant que état partie == en attente ET nb joueurs dans la partie != nb joueurs nécessaires
        //    faire dodo
    }

    /* NOTE :
       waitForTurnStarts() is based on a semaphore to implement a synchro barrier.
       The seamphore is created with 0 token within. Thus, each thread trying to get a token will
       have to wait until another one put tokens in the semaphore.
     */
    public void waitForTurnStarts() {
        nbPlayerInTurn++;
        if (nbPlayerInTurn == nbPlayerInParty){
            nbPlayerInTurn = 0;
            currentPlayer = playerOfNextTurn;
            initNewTurn();
            semBeginTurn.put(1);
        }
        semBeginTurn.get();
        // incrementer nb joueurs dans le tour
        // si nb joueurs dans le tour == nb joueurs dans partie
        //    remettre nb joueurs dans le tour à 0
        //    initialiser joueurs courant avec valeur joueur du prochain tour
        //    initialiser nouveau tour
        //    mettre des jetons dans le semaphore
        // fsi
        // prendre un jeton dans le sémaphore
    }

    private synchronized void initNewTurn() {
        played.clear();
        result.clear();
        totemTaken = false;
        totemHand = false;
        resultMsg = "";
    }

    public synchronized Card revealCard() {

        // update lastRevealedCard by asking current player to reveal a card
        lastRevealedCard = currentPlayer.revealCard();
        return lastRevealedCard;
    }

    public synchronized Player getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized int getCurrentState() {
        return state;
    }

    public synchronized void setCurrentState(int newState) {
        // les règles à tester dans l'ordre pour gérer l'état de la partie :
        //    qq soit etat partie, si newState = fin -> etat = fin
        //    si etat partie = en attente, alors seul newState = en cours est valide
        //    si etat partie = en cours, alors seul newState = joueur doit jouer est valide
        //    si etat partie = joueur doit jouer, alors seul newState = en cours est valide
        // toute autre combinaison est invalide et ne fait rien
    }

    public synchronized Object getCurrentCards() {
        Object s = "";
        String res = "";
        CardPacket packet = new CardPacket();
        for(Player p : players) {
            packet.addCards(p.giveRevealedCards());
        }

        for (Card c : packet.cards){
            res +=  c.card + ", ";
        }

        s = res;
        // récupérer toutes les cartes actuellement visibles autour de la table et en faire un objet
        // NB : à vous de décider la classe de cette objet et comment il est construit.
        // Vous pouvez par exemple simplement créer une chaîne de caractères

        return s;
    }

    /* getAllRevealedCards() ;
       In case of players are in error, we must collect all visible cards plus those under the totem
       and distribute them among these players. This method does the first part
     */
    private CardPacket getAllRevealedCards() {

        CardPacket packet = new CardPacket();
        for(Player p : players) {
            packet.addCards(p.giveRevealedCards());
        }
        packet.addCards(underTotem);
        underTotem.clear();

        return packet;
    }

    /* getWinnerRevealedCards() ;
       In case of a player won the turn, we must collect its visible cards plus those under the totem
       and distribute them among loosers. This method does the first part
     */
    private CardPacket getWinnerRevealedCards(Player turnWinner) {

        CardPacket packet = new CardPacket();
        packet.addCards(turnWinner.giveRevealedCards());
        packet.addCards(underTotem);
        underTotem.clear();

        return packet;
    }

    private boolean checkSameCards(Player p) {
        boolean same = false;
        // si p != null
        //   same = true si la carte visible de p est la même qu'un autre joueur et false sinon
        //   ATTENTION : certains joueurs (dont p) n'ont peut être pas de carte visible

        return false;
    }

    /* NB : result values are :
       RES_ERROR: a player made an error (see below)
       RES_LOST: a player lost because he didn't take the totem while he should
       RES_NULL: a player took the good decision but is not the faster
       RES_WIN: a player has win the turn

       error cases are the following :
       - a player does nothing while the last revealed card is 'hand on totem'
       - a player takes the totem while the last revealed card is 'hand on totem'
       - a player puts his hand on the totem while the last revealed card is 'take totem'
       - a player takes the totem while he hasn't the same card than another player and the last revealed is != 'H' or 'T'

       NB: order values are :
       ACT_NOP: do nothing
       ACT_TAKETOTEM: take totem
       ACT_HANDTOTEM: hand on totem

       reutrned value : true if i'am the last thread to integrate the player order
     */
    public synchronized boolean integratePlayerOrder(Player player, int order) {

        played.add(player);

        /* NOTE : qq soit la valeur de order, il faut déterminer le résultat de l'action
           de player et ajouter celui-ci dans la list result.
           Ainsi, comme played et result sont remplis en même temps, on peut retrouver faiclement le résultat de chaque joueur.
        */

        // si dernière carte révélée == H

        //    si ordre == ACT_NOP -> le joueur a fait une erreur
        //    sinon si ordre == ACT_TAKETOTEM -> le joueur a fait une erreur
        //    sinon si ordre == ACT_HANDTOTEM
        //       si le totem n'a pas encore de main posé dessus
        //          le joueur est gagnant
        //          mettre à jour totemHand
        //       sinon
        //          si je suis le dernier à jouer (i.e. taille de played == nb joueurs dans partie)
        //             joueur a perdu
        //          sinon rien n'arrive au joueur
        //       fsi
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)

        // sinon si dernière carte révélée == T
        //    si ordre == ACT_NOP -> le joueur a perdu
        //    sinon si ordre == ACT_TAKETOTEM
        //       si le totem n'est pas encore pris
        //          le joueur est gagnant
        //          mettre à jour totemTaken
        //       sinon rien n'arrive au joueur
        //       fsi
        //    sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)

        // sinon (.ie. dernière carte révélée est normale)
        //    si ordre == ACT_NOP
        //       si le joueur a même carte que d'autres -> le joueur a perdu
        //       sinon rien n'arrive au joueur
        //    sinon si ordre == ACT_TAKETOTEM
        //       si le joueur a même carte que d'autres
        //          si le totem n'est pas encore pris
        //             le joueur est gagnant
        //             mettre à jour totemTaken
        //          sinon le joueur est perdant
        //       sinon le joueur a fait une erreur
        //    sinon si ordre == ACT_HANDTOTEM -> le joueur a fait une erreur
        //    sinon le joueur a fait une erreur (i.e. un ordre invalide)
        // fsi

        if (played.size() >= nbPlayerInParty) {
            setCurrentState(PARTY_ONGOING);
            return true;
        }
        return false;
    }

    public synchronized void analyseResults() {

        List<Player> lstErrors = new ArrayList<Player>(); // list of players that made an error this turn
        List<Player> lstLoosers = new ArrayList<Player>(); // list of players that lost (not an error) this turn
        Player turnWinner = null;

        for(int i=0;i< nbPlayerInParty;i++) {
            if (result.get(i) == RES_ERROR) {
                lstErrors.add(played.get(i));
            }
            if (result.get(i) == RES_LOST) {
                lstLoosers.add(played.get(i));
            }
        }

        // if some players made an error
        if (!lstErrors.isEmpty()) {
            /* whatever the case, players that made an error are the ultimate loosers:
	           - collect all revealed cards
	           - add those under the totem
	           - distribute them among loosers
            */
            CardPacket errorPack = getAllRevealedCards();

            int nb = (errorPack.size()+1) / lstErrors.size();
            for(int i=0;i<lstErrors.size();i++) {
                Player p = lstErrors.get(i);
                if (i < lstErrors.size()-1) {
                    p.takeCards(errorPack.takeXFirst(nb));
                    resultMsg = resultMsg + p.name + " made an error: he takes " + nb + "cards from all players.\n";
                }
                else {
                    resultMsg = resultMsg + p.name + " made an error: he takes " + errorPack.size() + "cards from all players.\n";
                    p.takeCards(errorPack.getAll());
                }
            }
            // now check if someone has won
            for(Player p :players) {
                if (p.hasWon()) {
                    resultMsg = resultMsg + p.name + " wins the party";
                    setCurrentState(PARTY_END);
                    return;
                }
            }
            playerOfNextTurn = lstErrors.get(loto.nextInt(lstErrors.size()));
            resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
        }
        // else if no player made an error
        else {

            int indexWinner = -1;
            for(Integer r : result) {
                if (r == RES_WIN) {
                    indexWinner = r;
                    break;
                }
            }
            // if nobody wins this turn
            if (indexWinner == -1) {
                resultMsg = resultMsg + "Nobody won this turn\n";
                playerOfNextTurn = players.get(currentPlayer.id % nbPlayerInParty);
                resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
            }
            // else if a player is the winner : result depends on the last revealed cards
            else {
                turnWinner = players.get(indexWinner);
                resultMsg = resultMsg + turnWinner.name + " won the turn.\n";

                // if winner wins on a take totem
                if (lastRevealedCard.card == 'T') {
                    resultMsg = resultMsg + "He puts his cards under the totem.\n";
                    underTotem.addAll(turnWinner.revealedCards.getAll());
                    turnWinner.revealedCards.clear();
                }
                // else if winner wins on a hand on totem
                else if (lastRevealedCard.card == 'H') {
                    Player looser = lstLoosers.get(0); // normally there should be a single player in lstLoosers list
                    resultMsg = resultMsg + "He gives his cards and those under totem to "+looser.name+".\n";
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    looser.takeCards(winnerPack.getAll());
                }
                // if winner wins because he has the same card than some other players
                else {
                    // distribute winner's revealed card to loosers
                    CardPacket winnerPack = getWinnerRevealedCards(turnWinner);
                    int nb = (winnerPack.size()+1) / lstLoosers.size();
                    for(int i=0;i<lstLoosers.size();i++) {
                        Player p = players.get(i);
                        if (i < lstLoosers.size()-1) {
                            p.takeCards(winnerPack.takeXFirst(nb));
                            resultMsg = resultMsg + p.name + " lost his a duel with "+turnWinner.name+". He takes " + nb + "cards.\n";
                        }
                        else {
                            p.takeCards(winnerPack.getAll());
                            resultMsg = resultMsg + p.name + " lost his a duel with "+turnWinner.name+". He takes " + winnerPack.size() + "cards.\n";
                        }
                    }
                }
                // now check if someone has won
                for(Player p :players) {
                    if (p.hasWon()) {
                        resultMsg = resultMsg + p.name + " wins the party";
                        setCurrentState(PARTY_END);
                        return;
                    }
                }
                playerOfNextTurn = lstLoosers.get(loto.nextInt(lstLoosers.size()));
                resultMsg = resultMsg + "Next player: "+ playerOfNextTurn.name;
            }
        }
        System.out.println("-------------------------------------------------------------------");
        for(Player p : players) {
            System.out.println(p.name+" has "+p.revealedCards.size()+" revealed cards and "+p.hiddenCards.size()+ " hidden cards");
        }
        System.out.println("-------------------------------------------------------------------");
    }

}
