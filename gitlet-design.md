# Gitlet Design Document

**Name**: Qiyue Hao

Get started in 7.25
- logic delegation
- what in main what not, if not then in where
- Main class should mostly be calling helper methods in the the Repository class
-  A Gitlet system is considered “initialized” in a particular location if it has a .gitlet directory there

- file separator in Win / MacOS issue. Don't hardcode / or \ !! read "Things to avoid" in spec
- Be careful using a HashMap when serializing! The order of things within the HashMap is non-deterministic.
   The solution is to use a TreeMap which will always have the same order

- choice of data structure for storing file contents: not assume file as texts/strings but binary files. store them as byte[] instead of string.

- INDEX stores both add and remove info? 
- learning: index is kind of never cleared to empty, it always reflects the head commit mapping. 

- "ANY recursive algorithm can be implemented using iteration and a stack."

- java serialize an object, and will serialize any objects this object point to. thus causing the timestamp object null issue.
## Testing Command
- make 
- make clean
- python3 tester.py --verbose --keep student_tests/stest01-basic-add.in


## Classes and Data Structures

### Main
This is the entry point to this program. It takes in arguments from the command line and based on the command (the first element of the `args` array) calls the corresponding command in `Repository` which will actually execute the logic of the command. It also validates the arguments based on the command to ensure that proper arguments were passed in.

#### Fields
This class has no fields and hence no associated state: it simply validates arguments and defers the execution to the CapersRepository class.

### Commit
This class represents a `Commit` that includes all necessary info: a capture of current working directory, which is the core of a version control system.

The `Commit` object will be stored in a file. Each `Commit` is serialized to a file, with its SHA-1 hash value as its unique file name.

All `Commit` objects are stored in `.gitlet/objects/commits` dir. 


#### Fields

      private String parentA;
      
      private String parentB;
      
      private String message;
      
      private ZonedDateTime timestamp;
      
      private String timestampString;

      Map<String, String> fileToBlob = new TreeMap<>();
      a mapping of file names "wug.txt" to blob references "d12da..."

### Repository
This is where the main logic of gitlet program will live. This file will handle all of the actual gitlet commands by reading/writing from/to the correct file, setting up persistence, and additional error checking.

#### Fields

    public static final File CWD = new File(System.getProperty("user.dir"));

    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File HEADS_DIR = join(GITLET_DIR, "heads");

    public static final File OBJ_DIR = join(GITLET_DIR, "objects");

    public static final File CMTS_DIR = join(OBJ_DIR, "commits");

    public static final File BLOBS_DIR = join(OBJ_DIR, "blobs");

    public static final File HEAD = join(GITLET_DIR, "HEAD");

    public static final File master = join(HEADS_DIR, "master");

    public static final File INDEX = join(GITLET_DIR, "INDEX");


## Algorithms

---

### `Repository` — Core Version Control System

The `Repository` class implements a simplified Git-like version control system using persistent file-based storage under `.gitlet/`. All objects (commits and file contents) are identified by **SHA-1 hashes**. The **staging area** is represented as a serialized `TreeMap<String, String>` mapping filenames to blob hashes, stored in `.gitlet/INDEX`.

---

### `init()`

Initializes a new Gitlet repository:
- Ensures no existing `.gitlet` directory.
- Creates directory structure: `.gitlet/`, `heads/`, `objects/commits/`, `objects/blobs/`.
- Creates `HEAD` file pointing to `heads/master`.
- Generates the **initial commit** with:
   - Timestamp: `1970-01-01 00:00:00 UTC`
   - Message: `"initial commit"`
   - Empty file map
- Serializes and stores commit using its SHA-1 hash as filename.
- Initializes empty staging area (`INDEX`).
- Sets `master` branch to point to initial commit.

---

### `add(String fileName)`

Stages a file for addition:
- Validates file exists in current working directory (CWD).
- Computes SHA-1 hash of current file contents.
- Reuses existing blob if identical content already stored in `.gitlet/objects/blobs/`.
- Updates staging area (`INDEX`) with `filename → blobHash`.
- Overwrites any prior staging entry for the same file.
- **Removes** file from staging if current version matches version in HEAD commit.

---

### `commit(String message, String mergedHead)`

Creates a new commit from staged changes:
- Loads current commit and staging area.
- Aborts with error if staging area identical to current commit (no changes).
- Copies staging area into new commit’s `fileToBlob` map.
- Sets **parent pointers**:
   - First parent: current HEAD commit
   - Second parent (if `mergedHead != null`): head of merged branch
- Assigns current timestamp and user-provided message.
- Computes SHA-1 of serialized commit → becomes commit ID.
- Updates current branch’s head to new commit ID.
- **Preserves staging area** — cleared only by `checkout` or `reset`.

---

### `rm(String fileName)`

Unstages or stages file for removal:
- Loads staging area and current commit.
- Aborts if file is neither staged nor tracked in HEAD.
- Removes file from staging area.
- If tracked in current commit, deletes file from CWD and ensures absence in future commits.

---

### `log()`

Displays commit history from current HEAD:
- Traverses backward via **first parent** only.
- Prints for each commit:
   - Full SHA-1 ID
   - Merge line (if second parent exists): `Merge: abc1234 def5678`
   - Timestamp in formatted string
   - Commit message

---

### `global_log()`

Displays **all commits** in repository:
- Iterates over all files in `.gitlet/objects/commits/`.
- Prints same metadata format as `log()` for each commit, regardless of branch.

---

### `find(String message)`

Searches for commits by exact message:
- Scans all commit objects in `objects/commits/`.
- Prints full SHA-1 ID of each matching commit.
- Prints `"Found no commit with that message."` if none found.

---

### `status()`

Reports repository state:
- **Branches**: Lists all branch names; current branch prefixed with `*`.
- **Staged Files**: Files in staging area with modified or new content vs. HEAD.
- **Removed Files**: Files in HEAD commit but absent from staging area.
- (Placeholder sections for modified/untracked files.)

---

### `checkoutFileInHeadCmt(String fileName)`
### `checkoutFileInCmt(String commitId, String fileName)`

Restores a file from a specific commit:
- Validates commit ID and file existence in commit.
- Retrieves blob hash from commit’s `fileToBlob` map.
- Reads blob content from `.gitlet/objects/blobs/`.
- Overwrites or creates file in CWD.
- **Does not stage** the restored file.

---

### `checkoutBranch(String branchName)`

Switches to a different branch:
- Validates branch exists and is not current.
- Checks for **untracked files in CWD** that would be overwritten by target branch.
- Deletes files present in current branch but absent in target.
- Restores all files from target branch’s HEAD commit.
- Sets staging area to match target commit’s file map.
- Updates `HEAD` to point to `heads/<branchName>`.

---

### `createBranch(String branchName)`

Creates a new branch:
- Validates branch name does not already exist.
- Creates file `.gitlet/heads/<branchName>` containing current HEAD commit ID.

---

### `rmBranch(String branchName)`

Deletes a branch:
- Validates branch exists and is not current.
- Deletes `.gitlet/heads/<branchName>` file.

---

### `reset(String commitId)`

Resets current branch to a specific commit:
- Validates commit exists.
- Checks for untracked file overwrite conflicts.
- Deletes files in current commit but not in target.
- Restores all files from target commit.
- Sets staging area to match target commit.
- Updates current branch head to target commit ID.

---

### `merge(String givenBranch)` — Three-Way Merge Algorithm

#### **Part 1: Pre-Merge Validation**
- Aborts if staging area contains uncommitted changes.
- Aborts if given branch does not exist or is current branch.
- **Fast-forward cases**:
   - If given branch is ancestor of current → print message and exit.
   - If current is ancestor of given → `checkoutBranch(givenBranch)` + fast-forward message.
- Checks for untracked files in CWD that would be overwritten.

#### **Part 2: Find Latest Common Ancestor (Split Point)**
- Performs **reverse BFS** from both branch heads to collect all ancestor commit IDs.
- Finds intersection of ancestor sets.
- Selects **latest** common ancestor by comparing commit timestamps.

#### **Part 3: Three-Way Merge Logic**
Iterates over **union** of filenames from current, given, and split point commits:
- Compares blob hashes across all three versions.
- Applies merge rules in order:

| Rule | Condition                                                  | Action                        |
|------|------------------------------------------------------------|-------------------------------|
| 1    | Modified in given, unchanged in current                    | Checkout given version, stage |
| 2    | Modified in current, unchanged in given                    | Keep current                  |
| 3    | Modified identically in both                               | Keep (no change)              |
| 4    | Absent in split, only in current                           | Keep                          |
| 5    | Absent in split, only in given                             | Checkout + stage              |
| 6    | Absent in given, unchanged in current                      | Delete from CWD, unstage      |
| 7    | Absent in current, unchanged in given                      | Keep absent                   |
| 8    | Any conflict (different modifications or delete vs modify) | **Conflict**                  |

#### **Part 4: Conflict Resolution**
For conflicting files:
- Constructs conflict marker format:
  ```text
  <<<<<<< HEAD
  [current branch contents]
  =======
  [given branch contents]
  >>>>>>>
- Writes conflicted file to CWD.
- Creates new blob from conflicted contents.
- Stages conflicted version in index.

#### **Part 5: Finalize Merge**

- Persists updated staging area.
- Creates merge commit with message: "Merged <given> into <current>."
- Sets second parent to given branch’s HEAD.
- Prints "Encountered a merge conflict." if any conflicts occurred.

---
Key Design Principles:

- Idempotent blob storage: Identical file contents share the same blob.
- Safe CWD operations: Untracked file checks prevent data loss.
- Deterministic merge: Exhaustive rule-based resolution covers all edge cases.
- Efficient ancestry: BFS ensures correct split point detection.

## Persistence

     .gitlet/ -- top level folder for all persistent data
       - heads/ -- folder containing branch file
           - master -- file containing this branch's head commit id
           - anotherBranch
       - objects/ -- folder containing blob and commit files
           - commits  -- folder
           - blobs -- folder
       - HEAD -- file containing ref to heads folder's branch file "heads/master"
       - INDEX -- file of staging area