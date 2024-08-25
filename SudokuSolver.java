
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

public class SudokuSolver extends JFrame {

    private static final int SIZE = 9;
    private JTextField[][] cells = new JTextField[SIZE][SIZE];
    private int[][] grid = new int[SIZE][SIZE];
    @SuppressWarnings("unchecked")
    private HashSet<Integer>[] rows = new HashSet[SIZE];
    @SuppressWarnings("unchecked")
    private HashSet<Integer>[] columns = new HashSet[SIZE];
    @SuppressWarnings("unchecked")
    private HashSet<Integer>[] subgrids = new HashSet[SIZE];
    private JTextField speedInput;
    private volatile boolean stopSolving = false;

    public SudokuSolver() {
        setTitle("Sudoku Solver");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(SIZE, SIZE));
        for (int row = 0; row < SIZE; row++) {
            rows[row] = new HashSet<>();
            columns[row] = new HashSet<>();
            subgrids[row] = new HashSet<>();
            for (int col = 0; col < SIZE; col++) {
                cells[row][col] = new JTextField();
                cells[row][col].setHorizontalAlignment(JTextField.CENTER);
                cells[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                int topBorder = (row + 1) % 3 == 0 ? 3 : 1;
                int leftBorder = (col + 1) % 3 == 0 ? 3 : 1;
                int bottomBorder = (row + 1) % 3 == 0 ? 3 : 1;
                int rightBorder = (col + 1) % 3 == 0 ? 3 : 1;
                cells[row][col].setBorder(BorderFactory.createMatteBorder(1, 1, bottomBorder, rightBorder, Color.BLUE));
                gridPanel.add(cells[row][col]);
            }
        }
        add(gridPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 3));

        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSolving = false;
                solveButton.setEnabled(false);
                if (validateInput()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            loadUserInput();
                            solvePuzzle();
                        }
                    }).start();
                } else {
                    resetGrid();
                    solveButton.setEnabled(true);
                }
            }
        });
        buttonPanel.add(solveButton);

        JButton resetButton = new JButton("Reset");

        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSolving = true;
                resetGrid();
                solveButton.setEnabled(true);
            }
        });
        buttonPanel.add(resetButton);

        speedInput = new JTextField("10"); // default speed value
        buttonPanel.add(new JLabel("Speed (ms):"));
        buttonPanel.add(speedInput);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private boolean validateInput() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                String text = cells[row][col].getText().trim();
                if (!text.isEmpty()) {
                    try {
                        int num = Integer.parseInt(text);
                        if (num < 1 || num > 9) {
                            JOptionPane.showMessageDialog(this, "Invalid input at (" + (row + 1) + ", " + (col + 1) + "). Please enter numbers between 1 and 9.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(this, "Invalid input at (" + (row + 1) + ", " + (col + 1) + "). Please enter a valid number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void loadUserInput() {
        // Read the user input from the cells and fill the grid
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                String text = cells[row][col].getText().trim();
                if (!text.isEmpty()) {
                    int num = Integer.parseInt(text);
                    grid[row][col] = num;
                    cells[row][col].setEditable(false);
                    cells[row][col].setBackground(Color.GRAY);
                    cells[row][col].setForeground(Color.WHITE);
                    rows[row].add(num);
                    columns[col].add(num);
                    subgrids[(row / 3) * 3 + col / 3].add(num);
                } else {
                    grid[row][col] = 0;
                }
            }
        }
    }

    private void resetGrid() {
        // Clear the grid and make all cells editable again
        for (int row = 0; row < SIZE; row++) {
            rows[row].clear();
            columns[row].clear();
            subgrids[row].clear();
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = 0;
                cells[row][col].setText("");
                cells[row][col].setEditable(true);
                cells[row][col].setBackground(Color.WHITE);
                cells[row][col].setForeground(Color.BLACK);
            }
        }
    }

    private void solvePuzzle() {
        int temp = solve();
        if (temp == 1) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SudokuSolver.this, "Sudoku Solved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } else if (temp == 2) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SudokuSolver.this, "Solving stopped.", "Stopped", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } else if (temp == 3) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SudokuSolver.this, "No solution exists.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private boolean isValid(int row, int col, int num) {
        return !rows[row].contains(num) && !columns[col].contains(num) && !subgrids[(row / 3) * 3 + col / 3].contains(num);
    }

    private int solve() {
        int[] empty = findEmptyCell();
        if (empty == null) {
            return 1;
        }
        int row = empty[0];
        int col = empty[1];

        for (int num = 1; num <= SIZE; num++) {
            if (stopSolving) {
                return 2;
            }
            if (isValid(row, col, num)) {
                grid[row][col] = num;
                rows[row].add(num);
                columns[col].add(num);
                subgrids[(row / 3) * 3 + col / 3].add(num);
                update(row, col, num);
                delay(getSpeed());
                if (solve() == 1) {
                    return 1;
                }
                grid[row][col] = 0;
                rows[row].remove(num);
                columns[col].remove(num);
                subgrids[(row / 3) * 3 + col / 3].remove(num);
                update(row, col, 0);
                delay(getSpeed());
            }
        }
        return 3;
    }

    private int[] findEmptyCell() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (grid[row][col] == 0) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    private void update(int row, int col, int num) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cells[row][col].setText(num == 0 ? "" : String.valueOf(num));
            }
        });
    }

    private void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private int getSpeed() {
        try {
            return Integer.parseInt(speedInput.getText());
        } catch (NumberFormatException e) {
            return 20; // Default speed
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SudokuSolver solver = new SudokuSolver();
                solver.setVisible(true);
            }
        });
    }
}
