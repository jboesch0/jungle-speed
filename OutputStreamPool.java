/* NOTE :
  Cette classe est complète : aucun ajout n'y est normalement nécéssaire
 */

import java.io.*;
import java.util.*;

public class OutputStreamPool {

    Map<Integer, ObjectOutputStream> pool = null;

    public OutputStreamPool() {
        pool = new HashMap<Integer, ObjectOutputStream>();
    }

    public synchronized void addStream(Integer id, ObjectOutputStream oos) {
        pool.put(id, oos);
    }

    public synchronized void removeStream(Integer id) {
        pool.remove(id);
    }

    public synchronized void sendToAll(Object o) throws IOException {
        boolean err = false;
        String msg = "cannot send to ";
        for (Integer dest : pool.keySet()) {
            try {
                pool.get(dest).writeObject(o);
            }
            catch(IOException e) {
                err = true;
                msg += dest;
            }
        }
        if (err) {
            throw new IOException(msg);
        }
    }
}
  
