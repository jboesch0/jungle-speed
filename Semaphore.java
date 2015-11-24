/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */

/* NOTE :
   This class acts like a classic semaphore, i.e. a synchronization barrier to prevent
   thread to continue their execution until there are tokens in the semaphore.
   A semaphore is a box containing tokens. The behavior is the following :
      - a thread can directly take a token in the box if there is at least one.
      - if there are no tokens, the thread must wait until one is put.
      - a thread can put as many tokens he wants in the box. In this case, he wakes up threads taht are trying to take  a token.
 */
class Semaphore {

    int nbTokens; // nombre de jetons

    public Semaphore(int nbTokens) {
        this.nbTokens = nbTokens;
    }

    public synchronized void put(int nb) {
        nbTokens += nb;
        notifyAll();
    }

    public synchronized void get() {
        while (nbTokens == 0) {
            try {
                wait();
            }
            catch(InterruptedException e) {}
        }
        nbTokens -= 1;
    }
}