package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
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


        // staging area data structure, a mapping of file name and blob references "d12da..."
        // <String, String>
        // Hello.txt - adfdsfsa3241234
        // fool.txt - dk4jkdl34332
        // Use: put(K, V), get(K)
        TreeMap<String, String> index;
        // If INDEX empty, create new structure, else read from INDEX.
        if (INDEX.length() == 0) {
            index = new TreeMap<>();
        }
        else {
            @SuppressWarnings("unchecked")
            TreeMap<String, String> temp =
                    (TreeMap<String, String>) readObject(INDEX, TreeMap.class);
            index = temp;
        }
        String blobHash = createBlob(fileName);

        index.put(fileName, blobHash);
        // write obj, exit
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
        // If no files have been staged, abort.
        // Print the message No changes added to the commit.
        // Todo: consider rm issue

        if (INDEX.length() == 0) {
            message("No changes added to the commit.");
            System.exit(0);
        }

        Commit cmt = getCurCommit();
        String cmtHash = readContentsAsString(join(GITLET_DIR, readContentsAsString(HEAD)));
        // Blob: a byte array object containing file content, with its SHA1 as its file name
        // read staging area, iterate it
            // create blob, save blob, with its SHA1 as its file name.
            // if new file, add fileName / blob sha to commit's fileToBlob. if updated file, update that.
        TreeMap<String, byte[]> stageForAdd = (TreeMap<String, byte[]>) readObject(INDEX, TreeMap.class);
        for (Map.Entry<String, byte[]> entry : stageForAdd.entrySet()) {
            String fileName = entry.getKey();
            byte[] contents = entry.getValue();
            String blobHash = sha1(contents);
            File blob = join(BLOBS_DIR, blobHash);
            blob.createNewFile();
            writeContents(blob, contents);
            if (cmt.fileToBlob.containsKey(fileName)) {
                cmt.fileToBlob.replace(fileName, blobHash);
            }
            else {
                cmt.fileToBlob.put(fileName, blobHash);
            }
        }
        // update the parent ref (add this commit to the commit tree)
        cmt.setParentA(cmtHash);
        // update metadata: message, timestamp
        cmt.setTimestamp(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        cmt.setMessage(msg);
        // generate hash for this commit. no more changes to this cmt object from now
        cmtHash = sha1(serialize(cmt));

        // update head pointer
        setHeadTo(cmtHash);
        // clear staging area
        stageForAdd.clear();
        // save commit obj, with its SHA1 as its file name.
        writeCmtObj(cmt, cmtHash);

        // Todo something for rm
    }





    /**
     * @return current commit (the HEAD).
     */
    static Commit getCurCommit() {
        String curCmtHash = readContentsAsString(join(GITLET_DIR, readContentsAsString(HEAD)));
        return getCommit(curCmtHash);
    }

    /**
     * @param cmtHash target commit's hash
     * @return the commit object
     */
    static Commit getCommit(String cmtHash) {
        File cmtFile = join(CMTS_DIR, cmtHash);
        return readObject(cmtFile, Commit.class);
    }


    static String createBlob(String fileName) throws IOException {
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
    /**
     * @return true if the working file content identical to its version in current commit.
     */
    static Boolean fileEqualsCurCmt(String fileName) {

        File fileToAdd = join(CWD, fileName);
        String fileHash = sha1(readContents(fileToAdd));
        // read current commit obj version of this file's sha1
        Commit curCommit = getCurCommit();
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
}
