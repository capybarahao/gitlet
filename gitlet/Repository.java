package gitlet;

import com.sun.codemodel.internal.JForEach;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static gitlet.Utils.*;
import static gitlet.Utils.writeContents;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Qiyue Hao
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
    //    .gitlet/ -- top level folder for all persistent data
    //      - heads/ -- folder containing branch file
    //          - master -- file containing this branch's head commit id
    //          - anotherBranch
    //      - objects/ -- folder containing blob and commit files
    //          - commits  -- folder
    //          - blobs -- folder
    //      - HEAD -- file containing ref to heads folder's branch file "heads/master"
    //      - INDEX -- file of staging area

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    // The heads folder
    public static final File HEADS_DIR = join(GITLET_DIR, "heads");

    public static final File OBJ_DIR = join(GITLET_DIR, "objects");
    public static final File CMTS_DIR = join(OBJ_DIR, "commits");
    public static final File BLOBS_DIR = join(OBJ_DIR, "blobs");
    // The head file, containing head ref info
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    // The master file, containing master branch info
    public static final File master = join(HEADS_DIR, "master");
    public static final File INDEX = join(GITLET_DIR, "INDEX");


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
    public static void init() throws IOException {
        // if .gitlet dir already exist then print error msg and exit
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        // creates any necessary folders or files
        GITLET_DIR.mkdir();
        HEADS_DIR.mkdir();
        OBJ_DIR.mkdir();
        CMTS_DIR.mkdir();
        BLOBS_DIR.mkdir();
        HEAD.createNewFile();
        master.createNewFile();
        INDEX.createNewFile();
        // set head to master branch "heads/master"
        String branchName = "master";
        String headRef = "heads" + System.getProperty("file.separator") + branchName;
        writeContents(HEAD, headRef);

        // create initial commit
        Commit initial = Commit.createInitialCommit();

        // generate sha for commit object
        String Sha = sha1(serialize(initial));

        // write commit object to file
        File commitFile = join(CMTS_DIR, Sha);
        commitFile.createNewFile();
        writeObject(commitFile, initial);

        // write the commit sha to head, (which leads to master)
        String ref = readContentsAsString(HEAD);
        File branchFile = join(GITLET_DIR, ref);
        writeContents(branchFile, Sha);
    }

    // java gitlet.Main add [file name]
    // Adds a copy of the file as it currently exists to the staging area (see the description of the commit command)
    // For this reason, adding a file is also called staging the file for addition
    // Staging an already-staged file overwrites the previous entry in the staging area with the new contents
    // The staging area should be somewhere in .gitlet.
    // If the current working version of the file is identical to the version in the current commit
    // do not stage it to be added, and remove it from the staging area if it is already there
    // (as can happen when a file is changed, added, and then changed back to it’s original version).
    // The file will no longer be staged for removal (see gitlet rm), if it was at the time of the command.
    //
    //Failure cases: If the file does not exist, print the error message
    // File does not exist.
    // and exit without changing anything.
    public static void add(String fileName) {

        // if the file not exist, print error msg and exit
        List<String> plainFiles = plainFilenamesIn(CWD);
        assert plainFiles != null;
        if (!plainFiles.contains(fileName)) {
            message("File does not exist.");
            System.exit(0);
        }
        File fileToAdd = join(CWD, fileName);

        // if file content identical to its version in current commit
        // do not stage it to be added, and remove it from the staging area if it is already there
        String fileSha = sha1(serialize(fileToAdd));
        // read current commit obj version of this file's sha1
        Commit curCommit = getCurCommit();
        String curCmtFileSha = curCommit.fileToBlob.get(fileName);

        Boolean fileEqualsCurCmt = false;
        if (curCmtFileSha != null & curCmtFileSha.equals(fileSha)) {
            fileEqualsCurCmt = true;
        }

        // staging area data structure, a mapping of file name and its contents (as byte array)
        // Hello.txt - "Hi!"(as byte[])
        // fool.txt - "bar"
        // Use: put(K, V), get(K)
        TreeMap stageForAdd = new TreeMap<>();

        // read the current staging area as the map
        stageForAdd = readObject(INDEX, TreeMap.class);

        //  if it's empty,
        //      if file content identical to current commit, do nothing (maybe a msg) and exit
        //      write to obj directly
        //  if not empty
        //      read the map in it
        //      if file... current commit, edit map, write obj

        // if file content identical to current commit
            // if staging area empty, do nothing (maybe a msg) and exit
            // else read staging area, get stageForAdd
            // if find file, remove it
            // else !find, do nothing (maybe a msg) and exit
        // if !file content identical to current commit
            // if staging area empty, add file
            // else read staging area, get stageForAdd
            // if find file, overwrite it
            // else !find file, add file

        // write obj, exit

        if (INDEX.length() == 0) {

        }
        else if (INDEX.length() != 0 & fileEqualsCurCmt){

        }
        else {

        }

        stageForAdd = readObject(INDEX, TreeMap.class);



        stageForAdd.put(fileName, readContents(fileToAdd));


    }

    /**
     *
     * @return current commit.
     */
    static Commit getCurCommit() {
        String curCommitSha = readContentsAsString(join(GITLET_DIR, readContentsAsString(HEAD)));
        File cmtFile = join(CMTS_DIR, curCommitSha);
        return readObject(cmtFile, Commit.class);
    }
}
