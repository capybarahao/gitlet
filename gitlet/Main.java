package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Utils.join;
import static gitlet.Utils.message;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Qiyue Hao
 */
public class Main {

    public static void main(String[] args) {
        try {
            runGitlet(args);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *  init
     *  add
     */
    private static void runGitlet(String[] args) throws IOException {
        // If a user doesn’t input any arguments, print the message
        // Please enter a command.
        // and exit.
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                validateRepo();
                // Every commit must have a non-blank message.
                //  If it doesn’t, print the error message Please enter a commit message.
                if (args.length == 1) {
                    Utils.message("Please enter a commit message.");
                    System.exit(0);
                }
                validateNumArgs(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validateRepo();
                validateNumArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                validateRepo();
                validateNumArgs(args, 1);
                Repository.global_log();
                break;
            case "find":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validateRepo();
                validateNumArgs(args, 1);
                Repository.status();
                break;
            case "checkout":
                validateRepo();
                switch(args.length) {
                    case 3: // checkout -- [file name]
                        if (args[1].equals("--")) {
                            Repository.checkoutFileInHeadCmt(args[2]);
                        }
                        else {
                            Utils.message("Incorrect operands.");
                            System.exit(0);
                        }
                        break;
                    case 4: // checkout [commit id] -- [file name]
                        if (args[2].equals("--")) {
                            Repository.checkoutFileInCmt(args[1], args[3]);
                        }
                        else {
                            Utils.message("Incorrect operands.");
                            System.exit(0);
                        }
                        break;
                    case 2: // checkout [branch name]
                        Repository.checkoutBranch(args[1]);
                        break;
                    default:
                        Utils.message("Incorrect operands.");
                        System.exit(0);
                }
                break;
            case "branch":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.createBranch(args[1]);
                break;
            case "rm-branch":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                validateRepo();
                validateNumArgs(args, 2);
                Repository.reset(args[1]);
                break;


            // If a user inputs a command that doesn’t exist, print the message
            // No command with that name exists.
            // and exit.
            default:
                Utils.message("No command with that name exists.");
                System.exit(0);
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * If a user inputs a command with the wrong number or format of operands, print the message
     * Incorrect operands.
     * and exit.
     *
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }
    // If a user inputs a command that requires being in an initialized Gitlet working directory
    // (i.e., one containing a .gitlet subdirectory), but is not in such a directory,
    // print the message
    // Not in an initialized Gitlet directory.
    public static void validateRepo() {
        // if .gitlet subdirectory not exist
        File CWD = new File(System.getProperty("user.dir"));
        File GITLET_DIR = join(CWD, ".gitlet");
        if (!GITLET_DIR.exists()) {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
