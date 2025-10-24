package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Qiyue
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

    // a mapping of file names "wug.txt" to blob references "d12da..."
    // Use: put(K, V), get(K)
    Map<String, String> fileToBlob = new TreeMap<>();

    public Commit (String message, String parentA, String parentB) {
        this.message = message;
        this.parentA = parentA;
        this.parentB = parentB;
    }

    // factory method only for init()
    public static Commit createInitialCommit() {
        Commit initial = new Commit("initial commit", null, null);
        initial.timestamp = formattedEpochTime();
        // Output: 1970-01-01T00:00:00Z
        return initial;
    }

    private static String formattedEpochTime() {

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEE MMM dd HH:mm:ss yyyy Z")
                .withZone(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime epoch = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return formatter.format(epoch).toString();
    }

    public void setMessage(String msg) {
        this.message = msg;
    }

    public String getMessage() {
        return message;
    }

    public void setTimestamp(String ts) {
        this.timestamp = ts;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setParentA(String aHash) {
        this.parentA = aHash;
    }

    public String getParentA() {
        return parentA;
    }

    public void setParentB(String bHash) {
        this.parentB = bHash;
    }

    public String getParentB() {
        return parentB;
    }

    public Set<String> getParents() {
        Set<String> parents = null;
        String pa = getParentA();
        String pb = getParentB();
        if (pa != null) {
            parents.add(pa);
        }
        if (pb != null) {
            parents.add(pb);
        }
        return parents;
    }
}
