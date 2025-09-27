package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Utils.*;
import static gitlet.Utils.writeContents;


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

        // generate hash for commit object
        String cmtHash = sha1(serialize(initial));

        // write commit object to file
        writeCmtObj(initial, cmtHash);

        // write the commit hash to head, (which leads to master)
        setHeadTo(cmtHash);
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
    public static void add(String fileName) throws IOException {

        // if the file not exist, print error msg and exit
        List<String> plainFiles = plainFilenamesIn(CWD);
        assert plainFiles != null;
        if (!plainFiles.contains(fileName)) {
            message("File does not exist.");
            System.exit(0);
        }

        // index: staging area data structure, a mapping of file name and blob references "d12da..."
        // <String, String>
        // Hello.txt - adfdsfsa3241234
        // fool.txt - dk4jkdl34332
        TreeMap<String, String> index = readIndex();

        // Blob: a byte array object containing file content, with its SHA1 as its file name
        // create blob(or not, if it exists), get the blob hash.
        String blobHash = writeBlobObj(fileName);

        // set the file as the latest version (put will do both add/replace)
        index.put(fileName, blobHash);

        // write index obj, exit
        writeObject(INDEX, index);
    }

    // java gitlet.Main commit [message]
    // To include multiword messages, surround them in quotes
    // Saves a snapshot of tracked files in the current commit and staging area
    // so they can be restored at a later time, creating a new commit.
    // The commit is said to be tracking the saved files. By default, each commit’s snapshot of files
    // will be exactly the same as its parent commit’s snapshot of files;
    // it will keep versions of files exactly as they are, and not update them.
    // A commit will only update the contents of files it is tracking that have been staged for addition
    // at the time of commit, in which case the commit will now include the version of the file that
    // was staged instead of the version it got from its parent.
    // A commit will save and start tracking any files staged for addition but weren’t tracked by its parent.
    // Finally, files tracked in the current commit may be untracked in the new commit
    // as a result being staged for removal by the rm command (below).

    public static void commit(String msg) throws IOException {
        // If no files have been staged, abort. (meaning index = fileToAdd?)
        // Print the message No changes added to the commit.
        Commit cmt = getCommit(getHead());
        TreeMap<String, String> index = readIndex();

        if (cmt.fileToBlob.equals(index)) {
            message("No changes added to the commit.");
            System.exit(0);
        }

        // prepare for setting fileToBlob
        cmt.fileToBlob.clear();

        // read index, iterate it, "copy" index to commit's fileToBlob
        for (Map.Entry<String, String> entry : index.entrySet()) {
            String fileName = entry.getKey();
            String blobHash = entry.getValue();
            cmt.fileToBlob.put(fileName, blobHash);
        }

        // update the parent ref (add this commit to the commit tree)
        String cmtHash = getHead();
        cmt.setParentA(cmtHash);
        // update metadata: message, timestamp
        cmt.setTimestamp(formattedNowTime());
        cmt.setMessage(msg);
        // generate hash for this commit. no more changes to this cmt object from now
        cmtHash = sha1(serialize(cmt));

        // update head pointer
        setHeadTo(cmtHash);

        // save commit obj, with its SHA1 as its file name.
        writeCmtObj(cmt, cmtHash);
    }

    // Unstage the file if it is currently staged for addition.
    // If the file is tracked in the current commit, stage it for removal and
    // remove the file from the working directory if the user has not already done so
    // (do not remove it unless it is tracked in the current commit).
    // Failure cases: If the file is neither staged nor tracked by the head commit,
    // print the error message
    // No reason to remove the file.
    public static void rm(String fileName) {
        // if the file not exist, print error msg and exit
        List<String> plainFiles = plainFilenamesIn(CWD);
        assert plainFiles != null;
        if (!plainFiles.contains(fileName)) {
            message("File does not exist.");
            System.exit(0);
        }
        TreeMap<String, String> index = readIndex();
        Commit cmt = getCommit(getHead());
        File fileToRm = join(CWD, fileName);

        if (!cmt.fileToBlob.containsKey(fileName) && !index.containsKey(fileName)) {
            message("No reason to remove the file.");
            System.exit(0);
        }

        // remove it from index
        index.remove(fileName);
        writeObject(INDEX, index);

        // if the file is tracked in the current commit, rm file in CWD
        if (cmt.fileToBlob.containsKey(fileName)) {
            restrictedDelete(fileToRm);
        }


    }

    public static void log() {
        String cmtHash = getHead();
        Commit p = getCommit(cmtHash);

        while (p != null) {

            System.out.println("===");
            System.out.printf("commit %s%n", cmtHash);
            // if a merged commit print merge info line
            // "Merge: 4975af1 2c1ead1"
            // Todo: after doing merge:
            //  The first parent is the branch you were on when you did the merge; the second is that of the merged-in branch.

            if (p.getParentB() != null) {
                System.out.printf("Merge: %s %s%n",p.getParentA().substring(0,7), p.getParentB().substring(0,7));
            }

            System.out.printf("Date: %s%n", p.getTimestamp());
            System.out.println(p.getMessage());
            System.out.println();
            cmtHash = p.getParentA();
            if (cmtHash == null) {
                break;
            }
            p = getCommit(cmtHash);
        }

    }

    public static void global_log() {
        List<String> cmtFiles = Utils.plainFilenamesIn(CMTS_DIR);
        for (String cmt: cmtFiles) {
            Commit p = getCommit(cmt);

            System.out.println("===");
            System.out.printf("commit %s%n", cmt);
            // if a merged commit print merge info line
            // "Merge: 4975af1 2c1ead1"
            // Todo: after doing merge:
            //  The first parent is the branch you were on when you did the merge; the second is that of the merged-in branch.

            if (p.getParentB() != null) {
                System.out.printf("Merge: %s %s%n",p.getParentA().substring(0,7), p.getParentB().substring(0,7));
            }

            System.out.printf("Date: %s%n", p.getTimestamp());
            System.out.println(p.getMessage());
            System.out.println();
        }
    }

    public static void find(String msg) {
        List<String> cmtFiles = Utils.plainFilenamesIn(CMTS_DIR);
        Commit p = null;
        boolean found = false;
        for (String cmt: cmtFiles) {
            p = getCommit(cmt);
            if (p.getMessage().equals(msg)) {
                System.out.println(cmt);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    //    === Branches ===
    //    *master
    //    other-branch
    //
    //    === Staged Files ===
    //    wug.txt
    //    wug2.txt
    //
    //    === Removed Files ===
    //    goodbye.txt
    //

    public static void status() {
        System.out.println("=== Branches ===");
        List<String> branches = Utils.plainFilenamesIn(HEADS_DIR);
        String curBranch = readContentsAsString(HEAD).substring(6);

        // Sort the list in lexicographical order
        Collections.sort(branches);
        for (String branch: branches) {
            if (branch.equals(curBranch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }

        System.out.println();

        System.out.println("=== Staged Files ===");

        TreeMap<String, String> index = readIndex();
        for (String key : index.keySet()) {
            System.out.println(key);
        }

        System.out.println();

        System.out.println("=== Removed Files ===");

        Commit cur = getCommit(getHead());
        for(String skey: cur.fileToBlob.keySet()) {
            if (!index.containsKey(skey)){
                System.out.println(skey);
            }
        }

        System.out.println();

    }

    // Takes the version of the file as it exists in the head commit and puts it in the working directory,
    // overwriting the version of the file that’s already there if there is one.
    // The new version of the file is not staged.
    // If the file does not exist in the previous commit, abort, printing the error message
    // File does not exist in that commit.
    // Do not change the CWD.
    public static void checkoutFileInCurCmt(String fileName) throws IOException {
        checkoutFileInCmt(getHead(), fileName);
    }


    public static void checkoutFileInCmt(String cmtID, String fileName) throws IOException {
        File cmtFile = join(CMTS_DIR, cmtID);
        if (!cmtFile.exists()) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        Commit cmt = getCommit(cmtID);
        if (!cmt.fileToBlob.containsKey(fileName)) {
            message("File does not exist in that commit.");
            System.exit(0);
        }
        // get the file content from commit
        String fileBlobHash = cmt.fileToBlob.get(fileName);
        File blobFile = join(BLOBS_DIR, fileBlobHash);
        // the file content is in  readContentsAsString(blobFile)

        // overwrite / create file, in CWD
        File workingFile = join(CWD, fileName);
        if (!workingFile.exists()) {
            workingFile.createNewFile();
        }
        writeContents(workingFile, readContentsAsString(blobFile));
    }

    public static void checkoutBranch(String branchName) {


        // Todo after branch done
    }

    public static void createBranch(String branchName) throws IOException {

        File branchFile = join(HEADS_DIR, branchName);
        if (branchFile.exists()) {
            message("A branch with that name already exists.");
            System.exit(0);
        }
        branchFile.createNewFile();
        writeContents(branchFile, getHead());
    }









    /**
     * @return current commit hash (the HEAD).
     */
    private static String getHead() {
        return readContentsAsString(join(GITLET_DIR, readContentsAsString(HEAD)));
    }

    /**
     * @param cmtHash target commit's hash
     * @return the commit object
     */
    private static Commit getCommit(String cmtHash) {
        File cmtFile = join(CMTS_DIR, cmtHash);
        return readObject(cmtFile, Commit.class);
    }


    static String writeBlobObj(String fileName) throws IOException {
        // create blob, save blob, with its SHA1 as its file name.
        // if blob exists, do nothing, if not then create and save.
        // return blob hash
        File fileToAdd = join(CWD, fileName);
        String blobHash = sha1(readContents(fileToAdd));
        File blob = join(BLOBS_DIR, blobHash);
        if (!blob.exists()) {
            blob.createNewFile();
            writeContents(blob, readContents(fileToAdd));
        }
        return blobHash;
    }


    // If INDEX empty, create new structure, else read from file INDEX.
    static TreeMap<String, String> readIndex(){
        TreeMap<String, String> index;
        if (INDEX.length() == 0) {
            index = new TreeMap<>();
        } else {
            @SuppressWarnings("unchecked")
            TreeMap<String, String> temp =
                    (TreeMap<String, String>) readObject(INDEX, TreeMap.class);
            index = temp;
        }
        return index;
    }

    /**
     * @return true if the working file content identical to its version in current commit.
     */
    static Boolean fileEqualsCurCmt(String fileName) {

        File fileToAdd = join(CWD, fileName);
        String fileHash = sha1(readContents(fileToAdd));
        // read current commit obj version of this file's sha1
        Commit curCommit = getCommit(getHead());
        String curCmtFileSha = curCommit.fileToBlob.get(fileName);

        boolean fileEqualsCurCmt = false;
        if (curCmtFileSha != null && curCmtFileSha.equals(fileHash)) {
            fileEqualsCurCmt = true;
        }
        return fileEqualsCurCmt;
    }

    /**
     * @param cmtHash commit's hash.
     *
     */
    static void setHeadTo(String cmtHash) {
        String ref = readContentsAsString(HEAD);
        File branchFile = join(GITLET_DIR, ref);
        writeContents(branchFile, cmtHash);
    }

    /**
     * @param cmt commit to be saved.
     * @param cmtHash commit's hash.
     */
    static void writeCmtObj(Commit cmt, String cmtHash) throws IOException {
        File commitObjFile = join(CMTS_DIR, cmtHash);
        commitObjFile.createNewFile();
        writeObject(commitObjFile, cmt);
    }

    static String formattedNowTime() {

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("EEE MMM dd HH:mm:ss yyyy Z")
                .withZone(ZoneId.of("Asia/Shanghai"));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return formatter.format(now).toString();
    }
}
