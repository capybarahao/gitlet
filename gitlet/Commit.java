package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    // parent of this commit, can have two parents.
    private String parentA;
    private String parentB;

    /** The message of this Commit. */
    private String message;

    private String timestamp;

    // a mapping of file names to blob references
    // maybe a map of file name / blob Sha1?
    // Use: put(K, V), get(K)
    private Map<String, String> fileToblob = new HashMap<>();

    public Commit (String message, String parentA, String parentB) {
        this.message = message;
        this.parentA = parentA;
        this.parentB = parentB;
        timestamp = LocalDateTime.now().toString();
    }
}
