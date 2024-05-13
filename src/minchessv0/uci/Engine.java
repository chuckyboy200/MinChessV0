package minchessv0.uci;

import java.util.Scanner;

import minchessv0.game.Game;

public class Engine implements Runnable {

    public Engine() {
        this.scanner = new Scanner(System.in);
        this.quit = false;
    }

    @Override
    public void run() {
        while(!quit) {
            this.command = scanner.nextLine();
            while(this.command.length() != 0) {
                String token = getNextToken();
                switch(token) {
                    case "uci": {
                        handleUCI();
                        break;
                    }
                    case "isready": {
                        handleIsReady();
                        break;
                    }
                    case "position": {
                        handlePosition(this.command);
                        break;
                    }
                    case "go": {
                        handleGo(this.command);
                        break;
                    }
                    case "stop": {
                        handleStop();
                        break;
                    }
                    case "quit": {
                        handleQuit();
                        break;
                    }
                    case "makemove": {
                        handleMakeMove();
                        break;
                    }
                    case "eval": {
                        handleEval();
                        break;
                    }
                    case "pv": {
                        handlePV();
                        break;
                    }
                    case "ucinewgame": {
                        handleUCINewGame();
                        break;
                    }
                    case "draw": {
                        Game.INSTANCE.sendCommand("draw");
                        break;
                    }
                    default: break;
                }
            }
        }
    }

    public void sendQuit() {
        this.quit = true;
    }

    private Scanner scanner;
    private boolean quit;
    private String command;
    
    private String getNextToken() {
        if(this.command.isEmpty()) return "";
        this.command = this.command.trim();
        int index = this.command.charAt(0) == '\"' ? this.command.indexOf("\"", 1) + 1 : this.command.indexOf(" ", 0);
        String token = index == -1 ? this.command : this.command.substring(0, index);
        this.command = index == -1 ? "" : this.command.substring(index);
        return token;
    }

    private void handleUCI() {
        Game.INSTANCE.sendCommand("uci");
    }

    private void handleIsReady() {
        Game.INSTANCE.sendCommand("isready");
    }

    private void handlePosition(String command) {
        String token;
        while(!(token = getNextToken()).isEmpty()) {
            switch(token) {
                case "startpos": {
                    Game.INSTANCE.sendCommand("startpos");
                    break;
                }
                case "fen": {
                    Game.INSTANCE.sendCommand("fen", getNextToken());
                    break;
                }
                case "moves": {
                    while(!(token = getNextToken()).isEmpty()) {
                        Game.INSTANCE.sendCommand("moves", token);
                    }
                    break;
                }
                default: break;
            }
        }
    }

    private void handleGo(String command) {
        String token;
        while(!(token = getNextToken()).isEmpty()) {
            switch(token) {
                case "depth": {
                    Game.INSTANCE.sendCommand("depth", getNextToken());
                    break;
                }
                case "infinite": {
                    Game.INSTANCE.sendCommand("infinite");
                    break;
                }
                case "movetime": {
                    Game.INSTANCE.sendCommand("movetime", getNextToken());
                    break;
                }
                case "wtime": {
                    Game.INSTANCE.sendCommand("wtime", getNextToken());
                    break;
                }
                case "btime": {
                    Game.INSTANCE.sendCommand("btime", getNextToken());
                    break;
                }
                default: break;
            }
        }
        Game.INSTANCE.sendCommand("search");
    }

    private void handleStop() {
        Game.INSTANCE.sendCommand("stop");
    }

    private void handleQuit() {
        Game.INSTANCE.sendCommand("quit");
    }

    private void handleMakeMove() {
        Game.INSTANCE.sendCommand("makemove", getNextToken());
    }

    private void handleEval() {
        Game.INSTANCE.sendCommand("eval");
    }

    private void handlePV() {
        Game.INSTANCE.sendCommand("pv");
    }

    private void handleUCINewGame() {
        Game.INSTANCE.sendCommand("ucinewgame");
    }

}
