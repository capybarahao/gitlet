package gitlet;

import java.io.File;
import java.time.LocalDateTime;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");


    //Creates a new Gitlet version-control system in the current directory.
    // This system will automatically start with one commit:
    // a commit that contains no files and has the commit message initial commit
    // It will have a single branch: master, which initially points to this initial commit,
    // and master will be the current branch.
    // The timestamp for this initial commit will be 00:00:00 UTC, Thursday, 1 January 1970
    // in whatever format you choose for dates (this is called “The (Unix) Epoch”,
    // represented internally by the time 0.)
    // the initial commit in all repositories created by Gitlet will have exactly the same content

    //If there is already a Gitlet version-control system in the current directory,
    // it should abort. It should NOT overwrite the existing system with a new one.
    // Should print the error message
    // A Gitlet version-control system already exists in the current directory.
    public static void init() {
        // if .gitlet dir already exist then print error msg and exit

        // create .gitlet dir


        Commit initial = Commit.createInitialCommit();
        String initialSha = Utils.sha1(initial);
        // write the commit sha to file master and head

    }
}
