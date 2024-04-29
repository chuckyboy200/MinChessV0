package minchessv0.input;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class InputHandler {

    public static final String DRAW_COMMAND = "draw";
    public static final String GO_COMMAND = "go";
    public static final String QUIT_COMMAND = "quit";
    public static final String HALT_COMMAND = "halt";
    public static final String MOVE_COMMAND = "move";
    public static final String UNDO_COMMAND = "undo";
    public static final String EVAL_COMMAND = "eval";
    public static final String FEN_COMMAND = "fen";
    public static final String GEN_COMMAND = "gen";
    public static final String PERFT_COMMAND = "perft";
    
    public InputHandler() {
        this.scanner = new Scanner(System.in);
    }

    public List<String> getCommand() {
        System.out.print("> ");
        String input = scanner.nextLine();
        Pattern pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(input);
        List<String> parts = new ArrayList<>();
        while(matcher.find()) {
            if(matcher.group(1) != null) {
                parts.add(matcher.group(1));
            } else {
                parts.add(matcher.group());
            }
        }
        return parts;
    }

    private Scanner scanner;


}
