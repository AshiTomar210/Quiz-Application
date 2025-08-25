import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Quiz Swing Application
 * Compatible with: Java 8, Windows 7 (32-bit)
 * Features:
 *  - OOP question types (MCQ / True-False / Fill-in-the-Blank)
 *  - File handling: loads questions.txt, appends to results.txt
 *  - Timer per question (default 10 seconds)
 *  - Shuffle questions
 *  - User profiles (enter your name)
 *  - Show correct answer when the user is wrong or times out
 *  - Leaderboard (Top N) with username + score
 *
 * File format: questions.txt (UTF-8) — blocks of:
 *   MCQ
 *   <question>
 *   <option1>
 *   <option2>
 *   <option3>
 *   <option4>
 *   <correctIndex (1-4)>
 *
 *   TF
 *   <question>
 *   <True or False>
 *
 *   FIB
 *   <question with or without blanks>
 *   <answer text>
 */

enum QuestionType { MCQ, TF, FIB }

abstract class Question {
    protected String questionText;
    protected QuestionType type;

    public Question(String questionText, QuestionType type) {
        this.questionText = questionText;
        this.type = type;
    }

    public String getQuestionText() { return questionText; }
    public QuestionType getType() { return type; }

    /** Returns true if userAnswer is correct (string interpretation per type). */
    public abstract boolean isCorrect(String userAnswer);

    /** String representation of the correct answer (for feedback). */
    public abstract String getCorrectAnswerAsString();
}

class MCQQuestion extends Question {
    private String[] options; // length 4
    private int correctIndex; // 1-based index

    public MCQQuestion(String questionText, String[] options, int correctIndex) {
        super(questionText, QuestionType.MCQ);
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String[] getOptions() { return options; }

    @Override
    public boolean isCorrect(String userAnswer) {
        if (userAnswer == null) return false;
        userAnswer = userAnswer.trim();
        // Accept either the option number (1-4) or exact text match (case-insensitive)
        try {
            int idx = Integer.parseInt(userAnswer);
            return idx == correctIndex;
        } catch (NumberFormatException ignore) {
            return options[correctIndex - 1].equalsIgnoreCase(userAnswer);
        }
    }

    @Override
    public String getCorrectAnswerAsString() {
        return correctIndex + ". " + options[correctIndex - 1];
    }
}

class TrueFalseQuestion extends Question {
    private boolean correct;

    public TrueFalseQuestion(String questionText, boolean correct) {
        super(questionText, QuestionType.TF);
        this.correct = correct;
    }

    @Override
    public boolean isCorrect(String userAnswer) {
        if (userAnswer == null) return false;
        return ("true".equalsIgnoreCase(userAnswer) || "t".equalsIgnoreCase(userAnswer)) == correct
            || ("false".equalsIgnoreCase(userAnswer) || "f".equalsIgnoreCase(userAnswer)) == !correct;
    }

    @Override
    public String getCorrectAnswerAsString() {
        return correct ? "True" : "False";
    }
}

class FillBlankQuestion extends Question {
    private String answer;

    public FillBlankQuestion(String questionText, String answer) {
        super(questionText, QuestionType.FIB);
        this.answer = answer == null ? "" : answer.trim();
    }

    @Override
    public boolean isCorrect(String userAnswer) {
        if (userAnswer == null) return false;
        return answer.equalsIgnoreCase(userAnswer.trim());
    }

    @Override
    public String getCorrectAnswerAsString() {
        return answer;
    }
}

class QuestionLoader {
    public static java.util.List<Question> loadFromFile(String path) throws IOException {
        java.util.List<Question> list = new java.util.ArrayList<Question>();
        java.util.List<String> lines = java.nio.file.Files.readAllLines(
            java.nio.file.Paths.get(path),
            java.nio.charset.StandardCharsets.UTF_8
             );
        for (int i = 0; i < lines.size();) {
            String typeLine = lines.get(i).trim();
            if (typeLine.isEmpty()) { i++; continue; }
            if ("MCQ".equalsIgnoreCase(typeLine)) {
                if (i + 6 >= lines.size()) break; // not enough lines
                String q = lines.get(++i);
                String[] opts = new String[4];
                for (int k = 0; k < 4; k++) opts[k] = lines.get(++i);
                int correct = Integer.parseInt(lines.get(++i).trim());
                list.add(new MCQQuestion(q, opts, correct));
                i++;
            } else if ("TF".equalsIgnoreCase(typeLine)) {
                if (i + 2 >= lines.size()) break;
                String q = lines.get(++i);
                String tf = lines.get(++i).trim();
                boolean correct = tf.equalsIgnoreCase("true");
                list.add(new TrueFalseQuestion(q, correct));
                i++;
            } else if ("FIB".equalsIgnoreCase(typeLine)) {
                if (i + 2 >= lines.size()) break;
                String q = lines.get(++i);
                String ans = lines.get(++i);
                list.add(new FillBlankQuestion(q, ans));
                i++;
            } else {
                // Unknown line, skip
                i++;
            }
        }
        return list;
    }
}

class LeaderboardEntry {
    String name;
    int score;
    int total;
    String timestamp;

    LeaderboardEntry(String name, int score, int total, String timestamp) {
        this.name = name; this.score = score; this.total = total; this.timestamp = timestamp;
    }

    static LeaderboardEntry parse(String line) {
        // Format: name - score/total @ yyyy-MM-dd HH:mm
        try {
            String[] parts = line.split(" - ");
            String name = parts[0].trim();
            String[] rest = parts[1].split(" @ ");
            String scorePart = rest[0].trim();
            String ts = rest.length > 1 ? rest[1].trim() : "";
            String[] st = scorePart.split("/");
            int score = Integer.parseInt(st[0].trim());
            int total = Integer.parseInt(st[1].trim());
            return new LeaderboardEntry(name, score, total, ts);
        } catch (Exception e) {
            return null;
        }
    }
}

public class QuizSwingApp extends JFrame {
    // Config
    private static final String QUESTIONS_FILE = "questions.txt";
    private static final String RESULTS_FILE = "results.txt";
    private static final int TIME_PER_QUESTION_SEC = 10; // adjust if needed
    private static final int LEADERBOARD_LIMIT = 10;

    // State
    private String userName;
    private java.util.List<Question> questions = new ArrayList<Question>();
    private int currentIndex = -1;
    private int score = 0;
    private int remainingSeconds = TIME_PER_QUESTION_SEC;
    private javax.swing.Timer swingTimer;

    // UI Components
    private CardLayout cardLayout = new CardLayout();
    private JPanel root = new JPanel(cardLayout);

    // Start Panel
    private JTextField nameField = new JTextField();
    private JLabel startError = new JLabel(" ");

    // Quiz Panel
    private JLabel questionLabel = new JLabel();
    private JPanel answerPanel = new JPanel();
    private ButtonGroup mcqGroup = new ButtonGroup();
    private JRadioButton[] mcqButtons = new JRadioButton[4];
    private JRadioButton tfTrue = new JRadioButton("True");
    private JRadioButton tfFalse = new JRadioButton("False");
    private JTextField fibField = new JTextField();
    private JLabel timerLabel = new JLabel("Time: " + TIME_PER_QUESTION_SEC + "s");
    private JLabel feedbackLabel = new JLabel(" ");
    private JButton nextButton = new JButton("Submit & Next →");

    // Result Panel
    private JLabel finalScoreLabel = new JLabel();
    private JTextArea leaderboardArea = new JTextArea(12, 40);

    public QuizSwingApp() {
        super("Quiz Application (Java 8 / Win7)");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(720, 520);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        root.add(buildStartPanel(), "start");
        root.add(buildQuizPanel(), "quiz");
        root.add(buildResultPanel(), "result");
        add(root, BorderLayout.CENTER);

        // Timer setup (ticks every second)
        swingTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                remainingSeconds--;
                timerLabel.setText("Time: " + remainingSeconds + "s");
                if (remainingSeconds <= 0) {
                    swingTimer.stop();
                    handleAnswer(null, true); // time up
                }
            }
        });
    }

    private JPanel buildStartPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Quiz Application", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        p.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0;
        center.add(new JLabel("Enter your name:"), gc);
        gc.gridx = 1; gc.weightx = 1.0;
        nameField.setColumns(20);
        center.add(nameField, gc);

        gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.weightx = 0;
        JButton startBtn = new JButton("Start Quiz");
        center.add(startBtn, gc);

        gc.gridy = 2;
        startError.setForeground(Color.RED);
        center.add(startError, gc);

        p.add(center, BorderLayout.CENTER);

        startBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                onStartClicked();
            }
        });

        return p;
    }

    private JPanel buildQuizPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new BorderLayout());
        questionLabel.setFont(questionLabel.getFont().deriveFont(Font.PLAIN, 18f));
        questionLabel.setVerticalAlignment(SwingConstants.TOP);
        questionLabel.setBorder(new EmptyBorder(0, 0, 12, 0));
        top.add(questionLabel, BorderLayout.CENTER);

        timerLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD));
        top.add(timerLabel, BorderLayout.EAST);

        p.add(top, BorderLayout.NORTH);

        answerPanel.setLayout(new GridLayout(6, 1, 6, 6));
        p.add(answerPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        feedbackLabel.setForeground(new Color(0xAA0000));
        bottom.add(feedbackLabel, BorderLayout.WEST);
        bottom.add(nextButton, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);

        // Prepare MCQ radio buttons once
        for (int i = 0; i < 4; i++) {
            mcqButtons[i] = new JRadioButton();
            mcqButtons[i].setActionCommand(String.valueOf(i + 1));
        }

        nextButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String answer = collectCurrentAnswer();
                handleAnswer(answer, false);
            }
        });

        return p;
    }

    private JPanel buildResultPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Results & Leaderboard", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        p.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        finalScoreLabel.setFont(finalScoreLabel.getFont().deriveFont(Font.BOLD, 18f));
        center.add(finalScoreLabel, BorderLayout.NORTH);

        leaderboardArea.setEditable(false);
        leaderboardArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(leaderboardArea);
        center.add(scroll, BorderLayout.CENTER);

        p.add(center, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton playAgain = new JButton("Play Again");
        JButton exit = new JButton("Exit");
        buttons.add(playAgain);
        buttons.add(exit);
        p.add(buttons, BorderLayout.SOUTH);

        playAgain.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                resetAndGoToStart();
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        return p;
    }

    private void onStartClicked() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            startError.setText("Please enter your name.");
            return;
        }
        this.userName = name;
        // Load questions
        try {
            questions = QuestionLoader.loadFromFile(QUESTIONS_FILE);
          if (questions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No questions found in " + QUESTIONS_FILE,
        "Error", JOptionPane.ERROR_MESSAGE);

    return;
}
// Shuffle all questions
Collections.shuffle(questions, new Random());

// Pick only 10 (or less if file has fewer)
if (questions.size() > 10) {
    questions = new ArrayList<>(questions.subList(0, 10));
}
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to read " + QUESTIONS_FILE + "\n" + ex.getMessage(),
                    "Read Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        cardLayout.show(root, "quiz");
        nextQuestion();
    }

    private void nextQuestion() {
        currentIndex++;
        feedbackLabel.setText(" ");
        if (currentIndex >= questions.size()) {
            endQuiz();
            return;
        }
        Question q = questions.get(currentIndex);
        renderQuestion(q);
        remainingSeconds = TIME_PER_QUESTION_SEC;
        timerLabel.setText("Time: " + remainingSeconds + "s");
        swingTimer.restart();
    }

    private void renderQuestion(Question q) {
        // Clear panel
        answerPanel.removeAll();
        mcqGroup = new ButtonGroup();

        questionLabel.setText("<html><body style='width:600px'>Q" + (currentIndex + 1) + "/" + questions.size() + ": "
                + escapeHtml(q.getQuestionText()) + "</body></html>");

        if (q.getType() == QuestionType.MCQ) {
            MCQQuestion m = (MCQQuestion) q;
            String[] opts = m.getOptions();
            for (int i = 0; i < 4; i++) {
                mcqButtons[i].setText((i + 1) + ". " + opts[i]);
                mcqButtons[i].setSelected(false);
                mcqGroup.add(mcqButtons[i]);
                answerPanel.add(mcqButtons[i]);
            }
        } else if (q.getType() == QuestionType.TF) {
            tfTrue.setSelected(false);
            tfFalse.setSelected(false);
            ButtonGroup tfGroup = new ButtonGroup();
            tfGroup.add(tfTrue);
            tfGroup.add(tfFalse);
            answerPanel.add(tfTrue);
            answerPanel.add(tfFalse);
        } else { // FIB
            fibField.setText("");
            answerPanel.add(new JLabel("Type your answer:"));
            answerPanel.add(fibField);
        }

        answerPanel.revalidate();
        answerPanel.repaint();
    }

    private String collectCurrentAnswer() {
        Question q = questions.get(currentIndex);
        if (q.getType() == QuestionType.MCQ) {
            ButtonModel sel = mcqGroup.getSelection();
            return sel == null ? null : sel.getActionCommand(); // "1".."4"
        } else if (q.getType() == QuestionType.TF) {
            if (tfTrue.isSelected()) return "True";
            if (tfFalse.isSelected()) return "False";
            return null;
        } else {
            String t = fibField.getText();
            return t == null || t.trim().isEmpty() ? null : t.trim();
        }
    }

    private void handleAnswer(String userAnswer, boolean timedOut) {
        swingTimer.stop();
        Question q = questions.get(currentIndex);
        boolean correct = (userAnswer != null) && q.isCorrect(userAnswer);
        if (correct) {
            score++;
            feedbackLabel.setForeground(new Color(0x006400));
            feedbackLabel.setText("Correct!");
        } else {
            String why = timedOut ? "Time up! " : "Wrong! ";
            feedbackLabel.setForeground(new Color(0xAA0000));
            feedbackLabel.setText(why + "Correct answer: " + q.getCorrectAnswerAsString());
        }

        // Brief delay to let the user read feedback, then go next
        javax.swing.Timer pause = new javax.swing.Timer(900, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((javax.swing.Timer) e.getSource()).stop();
                nextQuestion();
            }
        });
        pause.setRepeats(false);
        pause.start();
        
    }

    private void endQuiz() {
        // Save result
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        String line = userName + " - " + score + "/" + questions.size() + " @ " + ts;
        try (FileWriter fw = new FileWriter(RESULTS_FILE, true)) {
            fw.write(line + System.lineSeparator());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to write results: " + e.getMessage(),
                    "Write Error", JOptionPane.ERROR_MESSAGE);
        }

        finalScoreLabel.setText("Hi " + userName + ", your score: " + score + "/" + questions.size());
        renderLeaderboard();
        cardLayout.show(root, "result");
    }

    private void renderLeaderboard() {
        java.util.List<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();
        if (new File(RESULTS_FILE).exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(RESULTS_FILE))) {
                String ln;
                while ((ln = br.readLine()) != null) {
                    LeaderboardEntry e = LeaderboardEntry.parse(ln);
                    if (e != null) entries.add(e);
                }
            } catch (IOException ignore) {}
        }
        // Sort by score desc, then timestamp (recent first)
        Collections.sort(entries, new Comparator<LeaderboardEntry>() {
            @Override public int compare(LeaderboardEntry a, LeaderboardEntry b) {
                int cmp = Integer.compare(b.score, a.score);
                if (cmp != 0) return cmp;
                return b.timestamp.compareTo(a.timestamp);
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s %-20s %-10s %-16s\n", "#", "Name", "Score", "When"));
        sb.append("----------------------------------------------\n");
        int limit = Math.min(LEADERBOARD_LIMIT, entries.size());
        for (int i = 0; i < limit; i++) {
            LeaderboardEntry e = entries.get(i);
            sb.append(String.format("%-4d %-20s %-10s %-16s\n", i + 1, e.name,
                    (e.score + "/" + e.total), e.timestamp));
        }
        leaderboardArea.setText(sb.toString());
    }

    private void resetAndGoToStart() {
        userName = null;
        questions.clear();
        currentIndex = -1;
        score = 0;
        nameField.setText("");
        cardLayout.show(root, "start");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void main(String[] args) {
        // Ensure a native look on Windows 7
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                new QuizSwingApp().setVisible(true);
            }
        });
    }
}
