package minchessv0.gui;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JPanel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
 
public class Window extends JFrame {

    public static void init() {
        get().setBoard(new long[16]);
    }

    public static Window get() {
        if(Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public void setBoard(long[] board) {
        System.arraycopy(board, 0, this.board, 0, board.length);
    }

    public void drawBoard(long[] board) {

    }

    private final static int BOARD_LEFT = 16;
	private final static int BOARD_HORIZONTAL = 64;
	private final static int BOARD_TOP = 16;
	private final static int BOARD_VERTICAL = 64;
	private final static int BOARD_WIDTH = 8 * BOARD_HORIZONTAL;
	private final static int BOARD_HEIGHT = 8 * BOARD_VERTICAL;
	private final static int PANEL_WIDTH = 400;
	private final static double[] X_SCALE = { 0, 54, 56, 46, 55, 54, 38, 38 };
	private final static double[] Y_SCALE = { 0, 56, 53, 51, 56, 54, 50, 50 };
	private final static double[] X_OFFSET =  { 0,  6,  4,  8,  4,  4, 13, 13 };
	private final static double[] Y_OFFSET =  { 0,  4,  7,  8,  4,  7, 10, 10 };
	private final static Color LIGHT_COLOR = new Color(0xeeeed2);
	private final static Color LIGHT_COLOR_PREV = new Color(0xf6f669);
	private final static Color LIGHT_COLOR_BEST = new Color(0x75c7e8);
	private final static Color DARK_COLOR = new Color(0x769656);
	private final static Color DARK_COLOR_PREV = new Color(0xbaca2b);
	private final static Color DARK_COLOR_BEST = new Color(0x268ccc);

    private static Window window;
    private ActionHandler actionHandler;

    private long[] board;

    private Window() {
        this.setTitle("MinChess");
        this.setSize(BOARD_WIDTH + BOARD_LEFT * 2 + PANEL_WIDTH, BOARD_HEIGHT + BOARD_TOP * 2);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(new BorderLayout());
        JPanel boardPanel = new JPanel();
        boardPanel.setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        this.add(boardPanel, BorderLayout.CENTER);
        JPanel controlPanel = new JPanel();
        controlPanel.setPreferredSize(new Dimension(PANEL_WIDTH, BOARD_HEIGHT));
        this.add(controlPanel, BorderLayout.WEST);
        setupBoard(boardPanel);
        setupControlPanel(controlPanel);
        this.pack();
        this.setVisible(true);
        this.board = new long[16];
    }

    private void setupBoard(JPanel boardPanel) {

    }

    private void setupControlPanel(JPanel controlPanel) {
        controlPanel.setLayout(new FlowLayout());
        addButton(controlPanel, "White Player", e -> actionHandler.doWhitePlayer());
        addButton(controlPanel, "Black Player", e -> actionHandler.doBlackPlayer());
    }

    private void addButton(JPanel panel, String buttonName, ActionListener actionListener) {
        JButton button = new JButton(buttonName);
        button.addActionListener(actionListener);
        panel.add(button);
    }

    

}
