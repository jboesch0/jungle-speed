/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */


/**
 * Created by sdomas on 04/09/15.
 *
 * NOTE : this exception must be thrown by ThreadServer methods that
 * process client request, each time the request is not valid taking into account
 * the state of the Game/Party
 * For example, a client cannot send a LIST PARTY request while he is in a party.
 */


public class IllegalRequestException extends Exception {

    public IllegalRequestException(String message) {
        super("Illegal request: "+message);
    }
}
