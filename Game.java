/* NOTE :
  Cette classe est incomplète : les commentaires sont là pour vous guider
 */


import java.util.*;

class Game {


    List<Party> parties; // all created parties
    List<Player> players; // all players connected to the server

    public Game() {
        parties = new ArrayList<Party>();
        players = new ArrayList<Player>();
    }

    public synchronized Party createParty(String name, Player creator, int nbPlayers) {
        Party p = null;
        // si partie déjà créée par creator, alors renvoyer null
        // sinon, créer une Party p et l'ajouter à parties
        return p;
    }

    public synchronized Player createPlayer(String name) {
        Player p = null;
        // si player avec name copmme nom existe déjà, renvoyer null
        // sinon, créer une Player p et l'ajouter à players
        return p;
    }

    public synchronized void removeParty(Party p) {
        // supprimer p de parties
    }

    public synchronized void removePlayer(Player p) {
        // supprimer p de players
    }

    public synchronized boolean playerJoinParty(Player player, Party party) {
        // si le player n'est pas dans players, renvoyer false
        // si la party n'est pas dans parties, renvoyer false
        // si l'id player == -1 (i.e. le joueur est déjà dans une partie), renvoyer false
        // sinon, ajouter player à party
        return true;
    }
}
    
