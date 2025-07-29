package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *  add
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            // call gitlet help?

        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // TODO: handle the `init` command

                break;
            case "add":
                // TODO: handle the `add [filename]` command
                break;
            // TODO: FILL THE REST IN
            case "commit":
                break;
        }
    }
    public static void init() {
        Commit initial = new Commit("initial commit", null);

    }
}
