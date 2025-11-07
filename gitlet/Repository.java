package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        String headRef = "heads" + File.separator + branchName;
        writeContents(HEAD, headRef);

        // create initial commit
        Commit initial = Commit.createInitialCommit();

        // generate hash for commit object
        String cmtHash = sha1(serialize(initial));

        // write commit object to file
        writeCmtObj(initial, cmtHash);

        // index setup
        TreeMap<String, String> index = new TreeMap<>();
        writeObject(INDEX, index);

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

    public static void commit(String msg, String mergedHead) throws IOException {
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
//        for (Map.Entry<String, String> entry : index.entrySet()) {
//            String fileName = entry.getKey();
//            String blobHash = entry.getValue();
//            cmt.fileToBlob.put(fileName, blobHash);
//        }
        //safe copy instead of iteration process above.
        cmt.fileToBlob = new TreeMap<>(index);

        // update the parent ref (add this commit to the commit tree)
        String cmtHash = getHead();
        cmt.setParentA(cmtHash);
        if (mergedHead != null) { // it's a merged commit. called from merge method
            cmt.setParentB(mergedHead);
        }
        // update metadata: message, timestamp
        cmt.setTimestamp();
        cmt.setMessage(msg);
        // generate hash for this commit. no more changes to this cmt object from now
        cmtHash = sha1(serialize(cmt));

        // update head pointer
        setHeadTo(cmtHash);

        // below is a wrong idea about clear staging area. Index should not clean to empty!!!
        // clear staging area/index
        // index.clear();
        // writeObject(INDEX, index);

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

            System.out.printf("Date: %s%n", p.getTimestampString());
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
        List<String> cmtFiles = plainFilenamesIn(CMTS_DIR);
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

            System.out.printf("Date: %s%n", p.getTimestampString());
            System.out.println(p.getMessage());
            System.out.println();
        }
    }

    public static void find(String msg) {
        List<String> cmtFiles = plainFilenamesIn(CMTS_DIR);
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
        List<String> branches = plainFilenamesIn(HEADS_DIR);
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

        Commit headCmt = getCommit(getHead());

        System.out.println("=== Staged Files ===");

        TreeMap<String, String> index = readIndex();
        for (String skey : index.keySet()) {
            if (!headCmt.fileToBlob.containsKey(skey) || !index.get(skey).equals(headCmt.fileToBlob.get(skey))){
                System.out.println(skey);
            }
        }

        System.out.println();

        System.out.println("=== Removed Files ===");

        for(String skey: headCmt.fileToBlob.keySet()) {
            if (!index.containsKey(skey)){
                System.out.println(skey);
            }
        }

        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    // Takes the version of the file as it exists in the head commit and puts it in the working directory,
    // overwriting the version of the file that’s already there if there is one.
    // The new version of the file is not staged.
    // If the file does not exist in the previous commit, abort, printing the error message
    // File does not exist in that commit.
    // Do not change the CWD.
    public static void checkoutFileInHeadCmt(String fileName) throws IOException {
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
        writeCmtFileToCWD(cmt, fileName);
    }
    // Takes all files in the commit at the head of the given branch, and puts them in the working directory
    // overwriting the versions of the files that are already there if they exist.
    // Also, at the end of this command, the given branch will now be considered the current branch (HEAD).
    // Any files that are tracked in the current branch but are not present in the checked-out branch
    // are deleted. The staging area is cleared, unless the checked-out branch is the current branch
    // (see Failure cases below).

    // If a working file is untracked in the current branch and would be overwritten by the checkout, print
    // There is an untracked file in the way; delete it, or add and commit it first.
    // and exit; perform this check before doing anything else. Do not change the CWD.

    public static void checkoutBranch(String targetBranch) throws IOException {
        File targetBranchFile = join(HEADS_DIR, targetBranch);
        // If no branch with that name exists
        if (!targetBranchFile.exists()) {
            message("No such branch exists.");
            System.exit(0);
        }
        // If that branch is the current branch
        String curBranch = readContentsAsString(HEAD).substring(6);
        if (targetBranch.equals(curBranch)) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }
        // get the head commit of both branches
        Commit targetBranchCmt = getCommit(readContentsAsString(targetBranchFile));
        Commit curBranchCmt =getCommit(getHead());

        // | WD has file3 | Current HEAD has file3 | Target HEAD has file3 | Result                                                          |
        //| ------------ | ---------------------- | --------------------- | --------------------------------------------------------------- |
        //| yes          | no                     | no                    | checkout succeeds, file stays                                   |
        //| yes          | no                     | yes                   | **error: untracked file in the way**                            |
        //| yes          | yes                    | yes/no                | checkout overwrites with target’s version (you lose WD changes) |
        //| no           | no                     | yes                   | checkout create that file |

        // this failure case occurs when in above yes/no/yes situation
        List<String> workingDirFilesList = plainFilenamesIn(CWD);
        for (String fileName: workingDirFilesList) {
            if (!curBranchCmt.fileToBlob.containsKey(fileName) && targetBranchCmt.fileToBlob.containsKey(fileName)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // iterate cur branch cmt files, to potential delete, yes /yes /no
        for (String fileName: curBranchCmt.fileToBlob.keySet()) {
            if (workingDirFilesList.contains(fileName) && !targetBranchCmt.fileToBlob.containsKey(fileName)) {
                File fileToBeDel = join(CWD, fileName);
                restrictedDelete(fileToBeDel);
            }
        }

        // iterate target branch cmt files, to overwrite and create,
        for (String fileName: targetBranchCmt.fileToBlob.keySet()) {
            writeCmtFileToCWD(targetBranchCmt, fileName);
        }

        // clear staging area/index, this means set the index to target branch head commit mapping
        // this is safe copy
        TreeMap<String, String> index = new TreeMap<>(targetBranchCmt.fileToBlob);
        writeObject(INDEX, index);

        // set target branch as current branch "heads/branchName"
        String headRef = "heads" + File.separator + targetBranch;
        writeContents(HEAD, headRef);

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

    public static void rmBranch(String branchName) {
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        String curBranch = readContentsAsString(HEAD).substring(6);
        if (branchName.equals(curBranch)) {
            message("Cannot remove the current branch.");
            System.exit(0);
        }

        restrictedDelete(branchFile);

    }

    // Checks out all the files tracked by the given commit. Removes tracked files that are not present
    // in that commit. Also moves the current branch’s head to that commit node
    // See the intro for an example of what happens to the head pointer after using reset.
    // The [commit id] may be abbreviated as for checkout. The staging area is cleared.
    // The command is essentially checkout of an arbitrary commit that also changes the current branch head.
    // If no commit with the given id exists, print
    // No commit with that id exists.
    // If a working file is untracked in the current branch and would be overwritten by the reset, print
    // There is an untracked file in the way; delete it, or add and commit it first.

    public static void reset(String cmtID) throws IOException {
        File cmtFile = join(CMTS_DIR, cmtID);
        if (!cmtFile.exists()) {
            message("No commit with that id exists.");
            System.exit(0);
        }

        Commit curHeadCmt = getCommit(getHead());
        Commit targetCmt = getCommit(cmtID);

        // below is very similar code to checkout branch
        List<String> workingDirFilesList = plainFilenamesIn(CWD);
        for (String fileName: workingDirFilesList) {
            if (!curHeadCmt.fileToBlob.containsKey(fileName) && targetCmt.fileToBlob.containsKey(fileName)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // iterate cur cmt files, to potential delete, yes /yes /no
        for (String fileName: curHeadCmt.fileToBlob.keySet()) {
            if (workingDirFilesList.contains(fileName) && !targetCmt.fileToBlob.containsKey(fileName)) {
                File fileToBeDel = join(CWD, fileName);
                restrictedDelete(fileToBeDel);
            }
        }

        // iterate target cmt files, to overwrite and create,
        for (String fileName: targetCmt.fileToBlob.keySet()) {
            writeCmtFileToCWD(targetCmt, fileName);
        }

        // clear staging area/index, this means set the index to target commit mapping
        // this is safe copy
        TreeMap<String, String> index = new TreeMap<>(targetCmt.fileToBlob);
        writeObject(INDEX, index);

        setHeadTo(cmtID);
    }

    public static void merge(String givenBranch) throws IOException {

        TreeMap<String, String> index = readIndex();
        String curCmtHash = getHead();
        Commit curCmt = getCommit(curCmtHash);

        // Failure cases, before ANYTHING

        // If there are staged additions or removals present, print the error message
        // You have uncommitted changes.
        // and exit
        if (!curCmt.fileToBlob.equals(index)) {
            message("You have uncommitted changes.");
            System.exit(0);
        }

        // If a branch with the given name does not exist, print the error message
        // A branch with that name does not exist.
        File branchFile = join(HEADS_DIR, givenBranch);
        if (!branchFile.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }

        // If attempting to merge a branch with itself, print the error message
        // Cannot merge a branch with itself.
        String curBranch = readContentsAsString(HEAD).substring(6);
        if (curBranch.equals(givenBranch)) {
            message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        // get the given branch head
        String givenCmtHash = readContentsAsString(branchFile);
        Commit givenCmt = getCommit(givenCmtHash);
        // get the split point commit
        String spCmtHash = getSplitPointCmt(givenBranch);
        Commit spCmt = getCommit(spCmtHash);

        // Failure case: If merge would generate an error because the commit that it does has no changes in it,
        // just let the normal commit error message for this go through
        // no to do


        // If the split point is the same commit as the given branch, then we do nothing;
        // the merge is complete, and the operation ends with the message
        // Given branch is an ancestor of the current branch.
        if (spCmtHash.equals(givenCmtHash)) {
            message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        // If the split point is the current branch, then the effect is to check out the given branch,
        // and the operation ends after printing the message
        // Current branch fast-forwarded.
        if (spCmtHash.equals(curCmtHash)) {
            checkoutBranch(givenBranch);
            message("Current branch fast-forwarded.");
            System.exit(0);
        }

        // Failure case: If an untracked file in the current commit would be overwritten or deleted by the merge, print
        // There is an untracked file in the way; delete it, or add and commit it first.
        // This case is positioned here bc The untracked-file check is only needed when
        // about to modify the CWD

        // below is very similar code to checkout branch
        List<String> workingDirFilesList = plainFilenamesIn(CWD);
        for (String fileName: workingDirFilesList) {
            if (!curCmt.fileToBlob.containsKey(fileName) && givenCmt.fileToBlob.containsKey(fileName)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }


        // !! Now do a new commit, change file contents in CWD as well
        // remember the core of 3 way merge: Apply the changes made in given (since split) onto current.

        // Create a big set including all files related, so that all edge cases being handled
        Set<String> allFiles= new HashSet<>();
        allFiles.addAll(givenCmt.fileToBlob.keySet());
        allFiles.addAll(curCmt.fileToBlob.keySet());
        allFiles.addAll(spCmt.fileToBlob.keySet());

        boolean conflicted = false;

        for (String file : allFiles) {
            String givenBlobHash = givenCmt.fileToBlob.get(file);
            String curBlobHash = curCmt.fileToBlob.get(file);
            String spBlobHash = spCmt.fileToBlob.get(file);

            if (spBlobHash != null) {
                if (givenBlobHash != null) {
                    // 1. modified in the given branch since the split point, but not modified in the current branch since the split point
                    // changed to  version in the given branch, then all be automatically staged
                    if (!Objects.equals(spBlobHash, givenBlobHash) && Objects.equals(spBlobHash, curBlobHash)) {
                        writeCmtFileToCWD(givenCmt, file);
                        index.put(file, givenBlobHash);
                        continue;
                    }
                    // 7. present at the split point, unmodified in the given branch, and absent in the current branch
                    // should remain absent.
                    if (curBlobHash == null && Objects.equals(spBlobHash, givenBlobHash)) {
                        continue;
                    }
                }
                else { // given == null
                    // 6. present at the split point, unmodified in the current branch, and absent in the given branch
                    // should be removed (and untracked).
                    if (Objects.equals(spBlobHash, curBlobHash)) {
                        File fileToBeDel = join(CWD, file);
                        restrictedDelete(fileToBeDel);
                        index.remove(file);
                        continue;
                    }
                }

                // 2. modified in the current branch but not in the given branch since the split point
                // should stay as they are.
                if (curBlobHash != null && givenBlobHash != null &&
                        !Objects.equals(spBlobHash, curBlobHash) && Objects.equals(spBlobHash, givenBlobHash)) {
                    continue;
                }

                // 3. modified in both the current and given branch in the same way
                // (i.e., both files now have the same content or were both removed)
                // left unchanged by the merge.
                if (!Objects.equals(spBlobHash, givenBlobHash) && Objects.equals(givenBlobHash, curBlobHash)) {
                    continue;
                }
            }
            else { // spBlobHash == null
                // 4. not present at the split point and are present only in the current branch
                // should remain as they are.
                if (givenBlobHash == null) {
                    continue;
                }
                // 5. not present at the split point and are present only in the given branch
                // should be checked out and staged.
                if (curBlobHash == null) {
                    writeCmtFileToCWD(givenCmt, file);
                    index.put(file, givenBlobHash);
                    continue;
                }
            }

            // if none of above satisfied then it's a conflict merge
            // modified in different ways in the current and given branches are in conflict.
            // “Modified in different ways” can mean that the contents of both are changed and different from other,
            // or the contents of one are changed and the other file is deleted,
            // or the file was absent at the split point and has different contents in the given and current branches.
            // replace the contents of the conflicted file with
            //<<<<<<< HEAD
            //contents of file in current branch
            //=======
            //contents of file in given branch
            //>>>>>>>

            // get the file contents of cur and given
            String curContents = null;
            String givenContents = null;
            if (curBlobHash == null) {
                curContents = null;
                File givenBlobFile = join(BLOBS_DIR,givenBlobHash);
                givenContents = readContentsAsString(givenBlobFile);
            }
            else if (givenBlobHash == null) {
                givenContents = null;
                File curBlobFile = join(BLOBS_DIR,curBlobHash);
                curContents = readContentsAsString(curBlobFile);
            }
            else {
                File curBlobFile = join(BLOBS_DIR,curBlobHash);
                curContents = readContentsAsString(curBlobFile);

                File givenBlobFile = join(BLOBS_DIR,givenBlobHash);
                givenContents = readContentsAsString(givenBlobFile);
            }


            // new file contents
            String newFileContents = "<<<<<<< HEAD\n" + curContents + "curContents\n" + givenContents +">>>>>>>\n";

            // overwrite / create file with conflict, in CWD
            File workingFile = join(CWD, file);
            if (!workingFile.exists()) {
                workingFile.createNewFile();
            }
            writeContents(workingFile, newFileContents);

            // write it as blob
            String newBlobHash = writeBlobObj(file);

            //
            index.put(file, newBlobHash);
            conflicted = true;
        }

        String msg = "Merged " + givenBranch + " into " + curBranch + ".";
        commit(msg, givenCmtHash);

        if (conflicted) {
            message("Encountered a merge conflict.");
        }

    }




// -----------------------------------------------------------------------------------------------------------


    /**
     * @return current commit hash (the HEAD commit).
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
     * This sets head pointer(of current branch) to provided commit.
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


    // this assumes cmt contains that file
    // if file already exists, overwrite it
    static void writeCmtFileToCWD(Commit cmt, String fileName) throws IOException {
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

    /**
     * return the split point cmt hash.
     * The split point is the LATEST common ancestor of the current and given branch heads
     * @param branchName target branch name.
     */
    static String getSplitPointCmt(String branchName) {
        // do Reverse BFS / DFS for both nodes, get two ancestors group.
        // then find all common ancestors, then find the last one.
        //
        String curCmtHash = getHead();
        String branchCmtHash = readContentsAsString(join(HEADS_DIR, branchName));
        // get ancestors of both branch
        Set<String> curAncestors = getAncestors(curCmtHash);
        Set<String> bAncestors = getAncestors(branchCmtHash);
        // get common ancestors among two sets
        Set<String> common = new HashSet<>(curAncestors);
        common.retainAll(bAncestors);

        // find the latest common ancestor, by timestamp!!!!!!
        // definition: A latest common ancestor is a common ancestor that is not an ancestor of any other common ancestor

        String latestCmtHash = null;
        ZonedDateTime latestTime = null;
        for (String cmtHash : common) {
            Commit c = getCommit(cmtHash);
            ZonedDateTime time = c.getTimestamp();
            if (latestTime == null || time.isAfter(latestTime)) {
                latestTime = time;
                latestCmtHash = cmtHash;
            }
        }

        return latestCmtHash;
    }
    /**
     * return the commit's all ancestors.
     * INCLUDING the commit itself.
     *
     * @param cmtHash commit's hash.
     * @return a set of cmt hash, including this commit's all ancestors, and the commit itself.
     */
    static Set<String> getAncestors(String cmtHash) {
        Commit curCmt = getCommit(cmtHash);
        // 1. make a Set to store visited ancestors
        Set<String> acsts = new HashSet<>();
        acsts.add(cmtHash);
        // 2. make a Stack (or Deque) for traversal
        Stack<String> fringe = new Stack<>();
        // 3. push the starting commit
        fringe.push(cmtHash);
        // 4. while stack not empty:
        while (!fringe.empty()) {
            // pop one commit
            String sHash = fringe.pop();
            // look up its parents from the map
            Set<String> parents = getCommit(sHash).getParents();
            if (parents != null) { // have at least one parent
                // for each parent
                for (String p : parents) {
                    // if not seen before:
                    if (!acsts.contains(p)) {
                        // add to ancestors
                        // push parent into stack
                        acsts.add(p);
                        fringe.push(p);
                    }
                }
            }
        }
        // 5. return the set of ancestors
        return acsts;
    }
    /**
     * check if target is ancestor of other.
     * @param target target commit's hash.
     * @return true if target commit is ancestor of other commit(s)
     */

    static Boolean isAncestor(String target, String other) {
        Boolean isAncestor = false;
        Set<String> acsts = getAncestors(other);
        if (acsts.contains(target)) {
            isAncestor = true;
        }
        return isAncestor;
    }
}
