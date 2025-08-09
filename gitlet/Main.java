package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Qiyue Hao
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *  add
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
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
                validateNumArgs("init", args, 1);

                break;
            case "add":
                validateNumArgs("add", args, 2);
                break;
            case "commit":

                break;
            // If a user inputs a command that doesn’t exist, print the message
            // No command with that name exists.
            // and exit.
            default:
                Utils.message("No command with that name exists.");
                System.exit(0);
        }
    }
    public static void init() {
        Commit initial = new Commit("initial commit", null);

    }

    /**
     * Checks the number of arguments versus the expected number,
     * If a user inputs a command with the wrong number or format of operands, print the message
     * Incorrect operands.
     * and exit.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            Utils.message("Incorrect operands.");
            System.exit(0);
        }
    }
}
