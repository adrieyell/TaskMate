package taskmate;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.ImageIcon;
import javax.swing.GrayFilter;


public class MainPage extends javax.swing.JFrame {
    
    // Data structures
    private java.util.List<Task> tasks = new ArrayList<>();
    private java.util.Stack<Task> completedStack = new Stack<>();
    
    // Rewards System (XP)
    private int currentXP = 0;
    private int level = 1;
    private final int XP_PER_TASK = 10;       // XP given per completed task
    private final int XP_PER_POMODORO = 25;   // XP given per completed Pomodoro set
    private final int XP_PER_LEVEL = 100;     // XP required to level up

    

    // Pomodoro timer state
    private javax.swing.Timer swingTimer;      // Swing timer (ticks every second)
    private int remainingSeconds = 25 * 60;    // seconds left in current interval
    private int totalElapsedSeconds = 0;       // accumulate seconds across intervals
    private int currentIntervalIndex = 0;      // 0..5 index into sequence
    private boolean sessionComplete = false;   // true when full 6-interval set finished

    // Sequence of pomodoro timer
    private final String[] intervalNames = {
        "Pomodoro", "Short Break", "Pomodoro", "Short Break", "Pomodoro", "Long Break"
    };
    private final int[] intervalDurations = {
        25*60, 5*60, 25*60, 5*60, 25*60, 15*60
    };
    
    // Selection tracking
    private Task selectedTask = null;
    private JPanel selectedRowPanel = null;
    
    // Utility method
    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
}
  
    public MainPage() {
        initComponents();
        setLocationRelativeTo(null);
        updateRewardsUI();
        panelLevelList.setLayout(new BoxLayout(panelLevelList, BoxLayout.Y_AXIS));

 
    // ensure disabled text color is gray
    editButton.setForeground(Color.GRAY);
    deleteButton.setForeground(Color.GRAY);

    // make disabled icons from existing ImageIcons (if icons are ImageIcon)
    if (editButton.getIcon() instanceof ImageIcon) {
        ImageIcon ic = (ImageIcon) editButton.getIcon();
        editButton.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage(ic.getImage())));
    }
    if (deleteButton.getIcon() instanceof ImageIcon) {
        ImageIcon ic = (ImageIcon) deleteButton.getIcon();
        deleteButton.setDisabledIcon(new ImageIcon(GrayFilter.createDisabledImage(ic.getImage())));
    }

    // ensure initial visual state matches enabled flag
    if (!editButton.isEnabled()) editButton.setForeground(Color.GRAY);
    if (!deleteButton.isEnabled()) deleteButton.setForeground(Color.GRAY);

    xpBar.setMinimum(0);
    xpBar.setMaximum(XP_PER_LEVEL);
    xpBar.setStringPainted(true); // allows showing XP text
    xpBar.setValue(currentXP);    // initialize progress visually
    xpBar.setValue(0);
    xpBar.setStringPainted(true);

    // Make table look clean: no grid lines, center headers, prevent header reordering
    jTable1.setShowGrid(false);
    jTable1.setShowHorizontalLines(false);
    jTable1.setShowVerticalLines(false);

    // center-align column headers
    javax.swing.table.DefaultTableCellRenderer headerRenderer =
        (javax.swing.table.DefaultTableCellRenderer) jTable1.getTableHeader().getDefaultRenderer();
    headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

    // optional: disallow reordering of columns (keeps header stable)
    jTable1.getTableHeader().setReorderingAllowed(false);

    // ensure the scrollpane corner isn't drawing anything odd
    jScrollPane1.setCorner(JScrollPane.UPPER_LEFT_CORNER, null);

    jTable1.getTableHeader().setPreferredSize(new Dimension(jTable1.getWidth(), 26));

     // initialize state
     resetPomodoroState(); // make sure default values applied

        
// Timer ticks each second
swingTimer = new javax.swing.Timer(1000, ev -> {
    // tick
    if (remainingSeconds > 0) {
        remainingSeconds--;
        totalElapsedSeconds++;
        lblTimer.setText(formatTime(remainingSeconds));
    } else {
        // current interval finished
        // advance or finish
        if (currentIntervalIndex < intervalDurations.length - 1) {
            currentIntervalIndex++;
            remainingSeconds = intervalDurations[currentIntervalIndex];
            lblTimer.setText(formatTime(remainingSeconds));
            // continue running automatically into next interval
            // (if you prefer manual start for each interval, use swingTimer.stop() here)
        } else {
            // finished last interval -> full Pomodoro set complete
            swingTimer.stop();
            sessionComplete = true;

            // Save the completed session to the table
            try {
                DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
                int minutes = (totalElapsedSeconds + 59) / 60;
                model.insertRow(0, new Object[]{ lblSubjectValue.getText().trim(), lblTaskValue.getText().trim(), minutes + " min" });
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // reset and go back to Study view
            resetPomodoroState();
            CardLayout cl = (CardLayout)(cardPanel.getLayout());
            cl.show(cardPanel, "Study");

            JOptionPane.showMessageDialog(this, "Pomodoro set completed and saved!", "Done", JOptionPane.INFORMATION_MESSAGE);
        }
    }
});
        
        // UI initialization & wiring 
        // populate priority combo
        priorityCombo.removeAllItems();
        priorityCombo.addItem("High");
        priorityCombo.addItem("Medium");
        priorityCombo.addItem("Low");

        // prepare task list panel for vertical rows
        taskListPanel.setLayout(new BoxLayout(taskListPanel, BoxLayout.Y_AXIS));
        taskListPanel.setBorder(new EmptyBorder(8,8,8,8));
        taskListScroll.getVerticalScrollBar().setUnitIncrement(12); // smoother scroll

        // initial state: no selection
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        editButton.setForeground(Color.GRAY);
        deleteButton.setForeground(Color.GRAY);
        // button wiring
        addButton1.addActionListener(e -> addTaskFromField());
        sortbyPriorityButton.addActionListener(e -> { bubbleSortByPriority(); refreshTasks(); });
        undoButton.addActionListener(e -> undoLastCompleted());

        // allow Enter to add
        taskField.addActionListener(e -> addTaskFromField());
        
        editButton.addActionListener(e -> showEditDialogForSelected());
        deleteButton.addActionListener(e -> deleteSelectedTask());

        // ensure initially disabled
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Placeholder behavior for the txtSubject
        txtSubject.setForeground(Color.GRAY);
        txtSubject.setText("Enter Subject Here");

        txtSubject.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtSubject.getText().equals("Enter Subject Here")) {
                    txtSubject.setText("");
                    txtSubject.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtSubject.getText().isEmpty()) {
                    txtSubject.setForeground(Color.GRAY);
                    txtSubject.setText("Enter Subject Here");
                }
            }
        });
        // initial refresh
        refreshTasks();
        
        // Placeholder behavior for the txtTask 
        txtTask.setForeground(Color.GRAY);
        txtTask.setText("Enter Task Here");

        txtTask.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtTask.getText().equals("Enter Task Here")) {
                    txtTask.setText("");
                    txtTask.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtTask.getText().isEmpty()) {
                    txtTask.setForeground(Color.GRAY);
                    txtTask.setText("Enter Task Here");
                }
            }
        });
        // initial refresh
        refreshTasks();
        
        // Placeholder behavior for the taskField 
        taskField.setForeground(Color.GRAY);
        taskField.setText("Enter Task Here");

        taskField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (taskField.getText().equals("Enter Task Here")) {
                    taskField.setText("");
                    taskField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (taskField.getText().isEmpty()) {
                    taskField.setForeground(Color.GRAY);
                    taskField.setText("Enter Task Here");
                }
            }
        });
        // initial refresh
        refreshTasks();

    // Group toggle buttons 
    ButtonGroup navGroup = new ButtonGroup();
    navGroup.add(StudySessionsButton);
    navGroup.add(ToDoListButton);
    navGroup.add(RewardsButton);
    

    // no page selected at startup
   StudySessionsButton.setSelected(false);
   ToDoListButton.setSelected(false);
   RewardsButton.setSelected(false);
   
    // CardLayout setup
    cardPanel.setLayout(new CardLayout());
    cardPanel.add(studyPanel, "Study");
    cardPanel.add(todoPanel, "Todo");
    cardPanel.add(rewardsPanel, "Rewards");
    cardPanel.add(studyPanel2, "studyPanel2");
    
    // hidden pages at startup
    CardLayout cl = (CardLayout)(cardPanel.getLayout());
    cl.show(cardPanel, "Empty"); 

    // Just make sure only one stays selected
    JToggleButton[] navButtons = { StudySessionsButton, ToDoListButton, RewardsButton };
    for (JToggleButton b : navButtons) {
        b.setFocusPainted(false);  
    }

    // Switch pages when buttons clicked
    StudySessionsButton.addActionListener(e -> ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Study"));
    ToDoListButton.addActionListener(e -> ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Todo"));
    RewardsButton.addActionListener(e -> ((CardLayout) cardPanel.getLayout()).show(cardPanel, "Rewards"));
    
    // Clear selection when clicking blank space in the task area 
    taskListPanel.addMouseListener(new MouseAdapter() {
    @Override
    
    public void mousePressed(MouseEvent e) {
        // only clear if user clicked directly on the panel (not on a task component)
        if (e.getSource() == taskListPanel) {
            clearSelection();
        }
    }
    
});
    
    
    javax.swing.JLabel sessionLogLabel = new javax.swing.JLabel();
    sessionLogLabel.setFont(new java.awt.Font("DM Sans Black", 1, 18)); 
    sessionLogLabel.setForeground(new java.awt.Color(0, 0, 0));
    sessionLogLabel.setText("Session Log");

// Pomodoro timer initialization 
remainingSeconds = intervalDurations[0];
lblTimer.setText(formatTime(remainingSeconds));

// Timer ticks each second
swingTimer = new javax.swing.Timer(1000, e -> {
    // tick
    if (remainingSeconds > 0) {
        remainingSeconds--;
        totalElapsedSeconds++;
        lblTimer.setText(formatTime(remainingSeconds));
    } else {
        // interval finished
        swingTimer.stop();
        // mark completed interval in logs table (optional per-interval)
        addIntervalLog(intervalNames[currentIntervalIndex], intervalDurations[currentIntervalIndex], true);

        if (currentIntervalIndex < intervalNames.length - 1) {
            currentIntervalIndex++;
            remainingSeconds = intervalDurations[currentIntervalIndex];
            lblTimer.setText(formatTime(remainingSeconds));
            // If reached final interval completion previously, we detect later when it finishes naturally
            if (currentIntervalIndex == intervalNames.length - 1) {
                // when last interval (Long Break) finishes next, sessionComplete will be true
            }
            // Ask user to press Start to begin next interval (optional)
            JOptionPane.showMessageDialog(this, intervalNames[currentIntervalIndex-1] + " finished.\nPress START to begin the next interval.", "Interval Complete", JOptionPane.INFORMATION_MESSAGE);
        } else {
            // finished last interval: full Pomodoro set complete
            sessionComplete = true;
            JOptionPane.showMessageDialog(this, "Full Pomodoro set completed! Press STOP to save the session.", "Set Complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }
});
    } 
    
    // add per-interval rows 
    private void addIntervalLog(String name, int durationSeconds, boolean finished) {
    // NOT used for session save; used only if you want to show per-interval rows
    // Comment out calls in swingTimer if you don't want per-interval logging.
}

private void resetPomodoroState() {
    currentIntervalIndex = 0;
    remainingSeconds = intervalDurations[0];
    totalElapsedSeconds = 0;
    sessionComplete = false;
    lblTimer.setText(formatTime(remainingSeconds));
    if (swingTimer != null && swingTimer.isRunning()) swingTimer.stop();
}

private void updateLevelList() {
if (panelLevelList == null) return;

    panelLevelList.removeAll();

    String[][] levels = {
        {"Level 1", "The Beginning"},
        {"Level 2", "Steady Steps"},
        {"Level 3", "Rhythm Seeker"},
        {"Level 4", "Focus Builder"},
        {"Level 5", "Momentum Keeper"},
        {"Level 6", "Peak Performer"},
        {"Level 7", "Flow Finder"},
        {"Level 8", "Discipline Driver"},
        {"Level 9", "Growth Guardian"},
        {"Level 10", "Legendary Learner"},
    };

    for (int i = 0; i < levels.length; i++) {
        int levelNum = i + 1;

        JPanel levelPanel = new JPanel(new BorderLayout());
        levelPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        levelPanel.setBackground(new Color(245, 245, 245));

        //️ Load level icon
        String iconPath = "/icons/level" + levelNum + ".png";
        JLabel iconLabel = new JLabel();
        java.net.URL iconURL = getClass().getResource(iconPath);
        if (iconURL != null) {
            iconLabel.setIcon(new ImageIcon(iconURL));
        } else {
            iconLabel.setText("📘"); // fallback emoji if no image found
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        }
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12)); // spacing from text

        // Level Title
        JLabel title = new JLabel(levels[i][0]);
        title.setFont(new Font("DM Sans", Font.BOLD, 20));

        // Level Description
        JLabel desc = new JLabel(levels[i][1]);
        desc.setFont(new Font("DM Sans", Font.PLAIN, 18));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(desc);

        // Add icon and text to panel
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentPanel.setOpaque(false);
        contentPanel.add(iconLabel);
        contentPanel.add(textPanel);

        levelPanel.add(contentPanel, BorderLayout.WEST);

        // 🔵 Highlight current level
        if (levelNum == level) {
            levelPanel.setBackground(new Color(220, 235, 255));
            title.setForeground(new Color(0, 102, 255));
        }

        panelLevelList.add(levelPanel);
        panelLevelList.add(Box.createVerticalStrut(8)); // spacing
    }

    panelLevelList.revalidate();
    panelLevelList.repaint();
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        navGroup = new javax.swing.ButtonGroup();
        cardPanel = new javax.swing.JPanel();
        todoPanel = new javax.swing.JPanel();
        taskPriorityText = new javax.swing.JLabel();
        todoListText = new javax.swing.JLabel();
        taskField = new javax.swing.JTextField();
        priorityCombo = new javax.swing.JComboBox<>();
        taskListScroll = new javax.swing.JScrollPane();
        taskListPanel = new javax.swing.JPanel();
        jSeparator1 = new javax.swing.JSeparator();
        addButton1 = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        undoButton = new javax.swing.JButton();
        sortbyPriorityButton = new javax.swing.JButton();
        studyPanel = new javax.swing.JPanel();
        taskText = new javax.swing.JLabel();
        subjectText = new javax.swing.JLabel();
        studySessionText = new javax.swing.JLabel();
        txtTask = new javax.swing.JTextField();
        txtSubject = new javax.swing.JTextField();
        btnStartPomodoro = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        studyPanel2 = new javax.swing.JPanel();
        subjectText1 = new javax.swing.JLabel();
        taskText1 = new javax.swing.JLabel();
        studySessionText1 = new javax.swing.JLabel();
        lblTaskValue = new java.awt.TextField();
        lblSubjectValue = new java.awt.TextField();
        lblTimer = new javax.swing.JLabel();
        btnStart = new javax.swing.JButton();
        btnNext1 = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        backBtn = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        pauseBtn = new javax.swing.JButton();
        rewardsPanel = new javax.swing.JPanel();
        lblLevelDesc = new javax.swing.JLabel();
        rewardsText = new javax.swing.JLabel();
        lblXP = new javax.swing.JLabel();
        xpBar = new javax.swing.JProgressBar();
        lblLevelTitle = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        scrollLevelList = new javax.swing.JScrollPane();
        panelLevelList = new javax.swing.JPanel();
        Empty = new javax.swing.JPanel();
        ButtonsPanel = new javax.swing.JPanel();
        ToDoListButton = new javax.swing.JToggleButton();
        StudySessionsButton = new javax.swing.JToggleButton();
        RewardsButton = new javax.swing.JToggleButton();
        logo_with_text = new javax.swing.JLabel();
        Background = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        cardPanel.setLayout(new java.awt.CardLayout());

        todoPanel.setBackground(new java.awt.Color(255, 255, 255));
        todoPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        taskPriorityText.setFont(new java.awt.Font("DM Sans", 1, 14)); // NOI18N
        taskPriorityText.setForeground(new java.awt.Color(51, 51, 51));
        taskPriorityText.setText("Task Priority");
        todoPanel.add(taskPriorityText, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 150, -1, -1));

        todoListText.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        todoListText.setForeground(new java.awt.Color(0, 0, 0));
        todoListText.setText("To-do List");
        todoPanel.add(todoListText, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 30, -1, -1));

        taskField.setBackground(new java.awt.Color(255, 255, 255));
        taskField.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        taskField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        taskField.setText("Enter Task Here");
        taskField.setToolTipText("");
        taskField.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        todoPanel.add(taskField, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 80, 240, 50));

        priorityCombo.setBackground(new java.awt.Color(255, 255, 255));
        priorityCombo.setFont(new java.awt.Font("DM Sans Black", 0, 14)); // NOI18N
        priorityCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "High", "Medium", "Low" }));
        priorityCombo.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        todoPanel.add(priorityCombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 170, 240, 40));

        taskListScroll.setViewportView(taskListPanel);

        todoPanel.add(taskListScroll, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 20, 440, 400));

        jSeparator1.setBackground(new java.awt.Color(0, 0, 0));
        jSeparator1.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        todoPanel.add(jSeparator1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 270, 240, 20));

        addButton1.setBackground(new java.awt.Color(165, 213, 246));
        addButton1.setFont(new java.awt.Font("DM Sans Black", 1, 12)); // NOI18N
        addButton1.setForeground(new java.awt.Color(0, 0, 0));
        addButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/add_icon.png"))); // NOI18N
        addButton1.setText("Add Task");
        addButton1.setMargin(new java.awt.Insets(2, 14, 3, 16));
        todoPanel.add(addButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 220, 140, 40));

        editButton.setBackground(new java.awt.Color(165, 213, 246));
        editButton.setFont(new java.awt.Font("DM Sans", 1, 14)); // NOI18N
        editButton.setForeground(new java.awt.Color(0, 0, 0));
        editButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/edit_icon.png"))); // NOI18N
        editButton.setText("Edit");
        todoPanel.add(editButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 290, 110, 50));

        deleteButton.setBackground(new java.awt.Color(165, 213, 246));
        deleteButton.setFont(new java.awt.Font("DM Sans", 1, 14)); // NOI18N
        deleteButton.setForeground(new java.awt.Color(0, 0, 0));
        deleteButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/delete_icon.png"))); // NOI18N
        deleteButton.setText("Delete");
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });
        todoPanel.add(deleteButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 290, 110, 50));

        undoButton.setBackground(new java.awt.Color(165, 213, 246));
        undoButton.setFont(new java.awt.Font("DM Sans", 1, 14)); // NOI18N
        undoButton.setForeground(new java.awt.Color(0, 0, 0));
        undoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/undo_icon.png"))); // NOI18N
        undoButton.setText("Undo");
        todoPanel.add(undoButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 360, 110, 50));

        sortbyPriorityButton.setBackground(new java.awt.Color(165, 213, 246));
        sortbyPriorityButton.setFont(new java.awt.Font("DM Sans", 1, 14)); // NOI18N
        sortbyPriorityButton.setForeground(new java.awt.Color(0, 0, 0));
        sortbyPriorityButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/sort_icon.png"))); // NOI18N
        sortbyPriorityButton.setText("<html>Sort by<br>Priority</html>");
        sortbyPriorityButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        sortbyPriorityButton.setMargin(new java.awt.Insets(2, 5, 3, 13));
        todoPanel.add(sortbyPriorityButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 360, 110, 50));

        cardPanel.add(todoPanel, "Todo");
        todoPanel.getAccessibleContext().setAccessibleName("Todo");

        studyPanel.setBackground(new java.awt.Color(255, 255, 255));
        studyPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        taskText.setFont(new java.awt.Font("DM Sans", 1, 22)); // NOI18N
        taskText.setForeground(new java.awt.Color(0, 0, 0));
        taskText.setText("Task");
        studyPanel.add(taskText, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 59, -1, 40));

        subjectText.setFont(new java.awt.Font("DM Sans", 1, 22)); // NOI18N
        subjectText.setForeground(new java.awt.Color(0, 0, 0));
        subjectText.setText("Subject");
        studyPanel.add(subjectText, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 59, -1, 40));

        studySessionText.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        studySessionText.setForeground(new java.awt.Color(0, 0, 0));
        studySessionText.setText("Study Session");
        studyPanel.add(studySessionText, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        txtTask.setBackground(new java.awt.Color(255, 255, 255));
        txtTask.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txtTask.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtTask.setText("Enter Task Here");
        txtTask.setToolTipText("");
        txtTask.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        txtTask.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTaskActionPerformed(evt);
            }
        });
        studyPanel.add(txtTask, new org.netbeans.lib.awtextra.AbsoluteConstraints(280, 100, 240, 50));

        txtSubject.setBackground(new java.awt.Color(255, 255, 255));
        txtSubject.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        txtSubject.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtSubject.setText("Enter Subject Here");
        txtSubject.setToolTipText("");
        txtSubject.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        txtSubject.setSelectionColor(new java.awt.Color(255, 255, 255));
        studyPanel.add(txtSubject, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 100, 250, 50));

        btnStartPomodoro.setBackground(new java.awt.Color(165, 213, 246));
        btnStartPomodoro.setFont(new java.awt.Font("DM Sans Black", 1, 14)); // NOI18N
        btnStartPomodoro.setForeground(new java.awt.Color(0, 0, 0));
        btnStartPomodoro.setText("Start Pomodoro");
        btnStartPomodoro.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartPomodoroActionPerformed(evt);
            }
        });
        studyPanel.add(btnStartPomodoro, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 100, 220, 50));

        jTable1.setFont(new java.awt.Font("DM Sans", 1, 18)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Subject", "Task", "Duration"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setRowHeight(25);
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setResizable(false);
        }

        studyPanel.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 160, 730, 290));

        cardPanel.add(studyPanel, "Study");
        studyPanel.getAccessibleContext().setAccessibleName("Study");

        studyPanel2.setBackground(new java.awt.Color(255, 255, 255));
        studyPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        subjectText1.setFont(new java.awt.Font("DM Sans", 1, 22)); // NOI18N
        subjectText1.setForeground(new java.awt.Color(0, 0, 0));
        subjectText1.setText("Subject");
        studyPanel2.add(subjectText1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 70, -1, 40));

        taskText1.setFont(new java.awt.Font("DM Sans", 1, 22)); // NOI18N
        taskText1.setForeground(new java.awt.Color(0, 0, 0));
        taskText1.setText("Task");
        studyPanel2.add(taskText1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, -1, 40));

        studySessionText1.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        studySessionText1.setForeground(new java.awt.Color(0, 0, 0));
        studySessionText1.setText("Pomodoro");
        studyPanel2.add(studySessionText1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        lblTaskValue.setEditable(false);
        lblTaskValue.setFont(new java.awt.Font("DM Sans", 1, 18)); // NOI18N
        studyPanel2.add(lblTaskValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 120, 290, 40));

        lblSubjectValue.setEditable(false);
        lblSubjectValue.setFont(new java.awt.Font("DM Sans", 1, 18)); // NOI18N
        studyPanel2.add(lblSubjectValue, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 70, 170, 40));

        lblTimer.setFont(new java.awt.Font("DM Sans", 1, 120)); // NOI18N
        lblTimer.setForeground(new java.awt.Color(0, 0, 0));
        lblTimer.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTimer.setText("25:00");
        studyPanel2.add(lblTimer, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 230, 380, 120));

        btnStart.setBackground(new java.awt.Color(165, 213, 246));
        btnStart.setFont(new java.awt.Font("DM Sans Black", 1, 18)); // NOI18N
        btnStart.setForeground(new java.awt.Color(0, 0, 0));
        btnStart.setText("Start");
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });
        studyPanel2.add(btnStart, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 380, 110, 50));

        btnNext1.setBackground(new java.awt.Color(165, 213, 246));
        btnNext1.setFont(new java.awt.Font("DM Sans Black", 1, 18)); // NOI18N
        btnNext1.setForeground(new java.awt.Color(0, 0, 0));
        btnNext1.setText("Next");
        btnNext1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNext1ActionPerformed(evt);
            }
        });
        studyPanel2.add(btnNext1, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 380, 110, 50));

        jSeparator2.setBackground(new java.awt.Color(0, 0, 0));
        jSeparator2.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        studyPanel2.add(jSeparator2, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 190, 710, 20));

        backBtn.setBackground(new java.awt.Color(165, 213, 246));
        backBtn.setFont(new java.awt.Font("DM Sans Black", 1, 20)); // NOI18N
        backBtn.setForeground(new java.awt.Color(0, 0, 0));
        backBtn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/back_button.png"))); // NOI18N
        backBtn.setText("Back");
        backBtn.setIconTextGap(6);
        backBtn.setMargin(new java.awt.Insets(2, 5, 3, 14));
        backBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                backBtnMouseClicked(evt);
            }
        });
        backBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });
        studyPanel2.add(backBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 20, 150, 50));

        btnStop.setBackground(new java.awt.Color(165, 213, 246));
        btnStop.setFont(new java.awt.Font("DM Sans Black", 1, 18)); // NOI18N
        btnStop.setForeground(new java.awt.Color(0, 0, 0));
        btnStop.setText("Stop");
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });
        studyPanel2.add(btnStop, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 380, 110, 50));

        pauseBtn.setBackground(new java.awt.Color(165, 213, 246));
        pauseBtn.setFont(new java.awt.Font("DM Sans Black", 1, 15)); // NOI18N
        pauseBtn.setForeground(new java.awt.Color(0, 0, 0));
        pauseBtn.setText("<html><div style='text-align:center;'>Pause /<br>Resume</div></html>");
        pauseBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseBtnActionPerformed(evt);
            }
        });
        studyPanel2.add(pauseBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 380, 110, 50));

        cardPanel.add(studyPanel2, "studyPanel2");
        studyPanel2.getAccessibleContext().setAccessibleName("studyPanel2");

        rewardsPanel.setBackground(new java.awt.Color(255, 255, 255));
        rewardsPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblLevelDesc.setFont(new java.awt.Font("DM Sans", 0, 18)); // NOI18N
        lblLevelDesc.setForeground(new java.awt.Color(0, 0, 0));
        lblLevelDesc.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod ");
        rewardsPanel.add(lblLevelDesc, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 110, -1, -1));

        rewardsText.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        rewardsText.setForeground(new java.awt.Color(0, 0, 0));
        rewardsText.setText("Rewards");
        rewardsPanel.add(rewardsText, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        lblXP.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        lblXP.setForeground(new java.awt.Color(0, 0, 0));
        lblXP.setText("#/# XP");
        rewardsPanel.add(lblXP, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 140, 160, 50));

        xpBar.setForeground(new java.awt.Color(0, 0, 0));
        xpBar.setMaximumSize(new java.awt.Dimension(100, 100));
        xpBar.setMinimumSize(new java.awt.Dimension(0, 0));
        xpBar.setOpaque(true);
        xpBar.setStringPainted(true);
        rewardsPanel.add(xpBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 150, 530, 30));
        xpBar.getAccessibleContext().setAccessibleName("xpBar");

        lblLevelTitle.setFont(new java.awt.Font("DM Sans Black", 1, 24)); // NOI18N
        lblLevelTitle.setForeground(new java.awt.Color(0, 0, 0));
        lblLevelTitle.setText("Level #: Text");
        rewardsPanel.add(lblLevelTitle, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 80, -1, -1));

        jSeparator3.setForeground(new java.awt.Color(0, 0, 0));
        jSeparator3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        rewardsPanel.add(jSeparator3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 200, 720, 20));

        panelLevelList.setOpaque(false);
        panelLevelList.setLayout(new javax.swing.BoxLayout(panelLevelList, javax.swing.BoxLayout.LINE_AXIS));
        scrollLevelList.setViewportView(panelLevelList);

        rewardsPanel.add(scrollLevelList, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 220, 720, 240));

        cardPanel.add(rewardsPanel, "Rewards");
        rewardsPanel.getAccessibleContext().setAccessibleName("Rewards");

        Empty.setBackground(new java.awt.Color(255, 255, 255));
        Empty.setForeground(new java.awt.Color(255, 255, 255));
        cardPanel.add(Empty, "Empty");

        getContentPane().add(cardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 170, 780, 460));
        cardPanel.getAccessibleContext().setAccessibleName("CardLayout");

        ButtonsPanel.setBackground(new java.awt.Color(255, 255, 255));
        ButtonsPanel.setLayout(new java.awt.GridLayout(3, 1, 0, 50));

        ToDoListButton.setBackground(new java.awt.Color(204, 204, 204));
        navGroup.add(ToDoListButton);
        ToDoListButton.setFont(new java.awt.Font("DM Sans Black", 1, 20)); // NOI18N
        ToDoListButton.setForeground(new java.awt.Color(0, 0, 0));
        ToDoListButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/to_do_list_icon.png"))); // NOI18N
        ToDoListButton.setText("<html>To-do<br>List</html>");
        ToDoListButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        ToDoListButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        ToDoListButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToDoListButtonActionPerformed(evt);
            }
        });
        ButtonsPanel.add(ToDoListButton);

        StudySessionsButton.setBackground(new java.awt.Color(204, 204, 204));
        navGroup.add(StudySessionsButton);
        StudySessionsButton.setFont(new java.awt.Font("DM Sans Black", 0, 20)); // NOI18N
        StudySessionsButton.setForeground(new java.awt.Color(0, 0, 0));
        StudySessionsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/study_sessions_icon.png"))); // NOI18N
        StudySessionsButton.setText("<html>Study<br>Sessions</html>");
        StudySessionsButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        StudySessionsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        StudySessionsButton.setName(""); // NOI18N
        StudySessionsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                StudySessionsButtonActionPerformed(evt);
            }
        });
        ButtonsPanel.add(StudySessionsButton);

        RewardsButton.setBackground(new java.awt.Color(204, 204, 204));
        navGroup.add(RewardsButton);
        RewardsButton.setFont(new java.awt.Font("DM Sans Black", 1, 18)); // NOI18N
        RewardsButton.setForeground(new java.awt.Color(0, 0, 0));
        RewardsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/rewards_icon.png"))); // NOI18N
        RewardsButton.setText("Rewards");
        RewardsButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        RewardsButton.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
        RewardsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RewardsButtonActionPerformed(evt);
            }
        });
        ButtonsPanel.add(RewardsButton);

        getContentPane().add(ButtonsPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 210, 180, 370));

        logo_with_text.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/logo_with_text.png"))); // NOI18N
        getContentPane().add(logo_with_text, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 70, 350, 80));

        Background.setIcon(new javax.swing.ImageIcon(getClass().getResource("/assets/BackgroundImage.jpg"))); // NOI18N
        getContentPane().add(Background, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1098, 708));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void ToDoListButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToDoListButtonActionPerformed
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, "Todo");
    }//GEN-LAST:event_ToDoListButtonActionPerformed

    private void StudySessionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_StudySessionsButtonActionPerformed
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, "Study");
    }//GEN-LAST:event_StudySessionsButtonActionPerformed

    private void RewardsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_RewardsButtonActionPerformed
        updateRewardsUI();
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, "Rewards");
    }//GEN-LAST:event_RewardsButtonActionPerformed

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void txtTaskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTaskActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTaskActionPerformed

    private void btnStartPomodoroActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartPomodoroActionPerformed

        String subject = txtSubject.getText().trim();
        String task = txtTask.getText().trim();

    if (subject.isEmpty() || subject.equalsIgnoreCase("Enter Subject Here") ||
        task.isEmpty() || task.equalsIgnoreCase("Enter Task Here")) {
        JOptionPane.showMessageDialog(this,
            "Please enter both Subject and Task before starting.",
            "Missing Information", JOptionPane.WARNING_MESSAGE);
        return;
    }

    // set the labels on the pomodoro page
    lblSubjectValue.setText(subject);
    lblTaskValue.setText(task);

    // reset timer but do NOT start it automatically
    resetPomodoroState();        
    lblTimer.setText(formatTime(remainingSeconds));

    // switch view only
    CardLayout cl = (CardLayout)(cardPanel.getLayout());
    cl.show(cardPanel, "studyPanel2");   

    }//GEN-LAST:event_btnStartPomodoroActionPerformed

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed

    if (sessionComplete) {
        // start a fresh session
        resetPomodoroState();
    }
    if (!swingTimer.isRunning()) {
        swingTimer.start();
    }
    }//GEN-LAST:event_btnStartActionPerformed

    private void btnNext1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNext1ActionPerformed

    // stop timer
    if (swingTimer.isRunning()) swingTimer.stop();

    if (currentIntervalIndex < intervalDurations.length - 1) {
        // move to next interval
        currentIntervalIndex++;
        remainingSeconds = intervalDurations[currentIntervalIndex];
        lblTimer.setText(formatTime(remainingSeconds));
    } else {
        JOptionPane.showMessageDialog(this, "Already at the last interval.", "Info", JOptionPane.INFORMATION_MESSAGE);
    }
    }//GEN-LAST:event_btnNext1ActionPerformed

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backBtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_backBtnActionPerformed

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
    if (swingTimer != null && swingTimer.isRunning()) swingTimer.stop();

    // mark as complete if already at last interval
    if (currentIntervalIndex >= intervalDurations.length - 1) {
        sessionComplete = true;
    }

    // Ask user whether to save partial session if not complete
    if (!sessionComplete) {
        int choice = JOptionPane.showConfirmDialog(this,
            "You haven't finished the full Pomodoro set yet.\nDo you want to save the partial session?",
            "Save partial?", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            resetPomodoroState();
            CardLayout cl = (CardLayout)(cardPanel.getLayout());
            cl.show(cardPanel, "Study");
            return;
        }
    }

    // Save the session log using the accumulated seconds
    try {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        int minutes = (totalElapsedSeconds + 59) / 60;
        if (minutes <= 0) minutes = 1;

        // 👇 Insert at top instead of bottom
        model.insertRow(0, new Object[]{
            lblSubjectValue.getText().trim(),
            lblTaskValue.getText().trim(),
            minutes + " min"
        });

    } catch (Exception ex) {
        ex.printStackTrace();
    }

        
    // Return to Study page after stopping
    resetPomodoroState();
    
    // Reward XP for completing a Pomodoro session
    addXP(XP_PER_POMODORO);
    CardLayout cl = (CardLayout)(cardPanel.getLayout());
    cl.show(cardPanel, "Study");
    }//GEN-LAST:event_btnStopActionPerformed

    private void backBtnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_backBtnMouseClicked
        CardLayout cl = (CardLayout)(cardPanel.getLayout());
        cl.show(cardPanel, "Study");
    }//GEN-LAST:event_backBtnMouseClicked

    private void pauseBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseBtnActionPerformed
        if (swingTimer == null) return; // safety check

    if (swingTimer.isRunning()) {
        //  Pause the timer
        swingTimer.stop();
        pauseBtn.setText("Resume");
    } else {
        //  Resume the timer
        swingTimer.start();
        pauseBtn.setText("Pause");
    }
    }//GEN-LAST:event_pauseBtnActionPerformed
    

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainPage.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainPage().setVisible(true);
            }
        });
    }

    //  ==== Helper methods ====  


private void addTaskFromField() {
    String name = taskField.getText().trim();
    if (name.isEmpty() || name.equals("Enter Task Here")) {
        JOptionPane.showMessageDialog(this, "Please enter a task name.", "Empty", JOptionPane.WARNING_MESSAGE);
        return;
    }
    String pri = (String) priorityCombo.getSelectedItem();
    int p = "High".equals(pri) ? 1 : ("Medium".equals(pri) ? 2 : 3);
    
    tasks.add(0, new Task(name, p));
    
    taskField.setText("Enter Task Here");
    taskField.setForeground(Color.GRAY);
    refreshTasks();
}

private void bubbleSortByPriority() {
    for (int i = 0; i < tasks.size() - 1; i++) {
        for (int j = 0; j < tasks.size() - i - 1; j++) {
            if (tasks.get(j).getPriority() > tasks.get(j + 1).getPriority()) {
                Task tmp = tasks.get(j);
                tasks.set(j, tasks.get(j + 1));
                tasks.set(j + 1, tmp);
            }
        }
    }
}

private void refreshTasks() {
    taskListPanel.removeAll();

    if (tasks.isEmpty()) {
        JLabel hint = new JLabel("Task list is empty. Add one to get started.");
        hint.setForeground(Color.DARK_GRAY);
        hint.setBorder(new EmptyBorder(6,6,6,6));
        taskListPanel.add(hint);
    } else {
        for (Task t : tasks) {
            JPanel row = createTaskRow(t);
            taskListPanel.add(row);
            taskListPanel.add(Box.createVerticalStrut(8));
        }
    }
    taskListPanel.revalidate();
    taskListPanel.repaint();

    SwingUtilities.invokeLater(() -> taskListScroll.getVerticalScrollBar().setValue(0));
}

private JPanel createTaskRow(Task t) {
JPanel row = new JPanel(new GridBagLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15); // Rounded corners
        }
    };
    row.setOpaque(false);
    row.setBackground(Color.WHITE);
    row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 74, 123), 1, true),
            new EmptyBorder(10, 10, 10, 10)
    ));

    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(0, 0, 0, 8);
    c.anchor = GridBagConstraints.WEST;

    // Checkbox for task functiionality
    JCheckBox doneBox = new JCheckBox();
    doneBox.setToolTipText("Mark as done (removes task)");
    doneBox.addActionListener(e -> {
        if (doneBox.isSelected()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Mark this task as done and remove it?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                completedStack.push(t);
                tasks.remove(t);
                clearSelection();
                refreshTasks();
                addXP(XP_PER_TASK);
            } else {
                doneBox.setSelected(false);
            }
        }
    });
    c.gridx = 0;
    c.gridy = 0;
    row.add(doneBox, c);

    // Priority Color and Text
    String priorityText;
    Color priorityColor;
    switch (t.getPriority()) {
        case 1 -> { priorityText = "High"; priorityColor = new Color(220, 53, 69); }   // Red
        case 2 -> { priorityText = "Medium"; priorityColor = new Color(255,102,0); } // Orange
        default -> { priorityText = "Low"; priorityColor = new Color(40, 167, 69); }   // Green
    }

    // Labels for priority
    JLabel nameLabel = new JLabel(t.getName());
    nameLabel.setFont(new Font("DM Sans SemiBold", Font.PLAIN, 18));
    nameLabel.setForeground(priorityColor); // name uses priority color

    JLabel priorityLabel = new JLabel(priorityText);
    priorityLabel.setFont(new Font("DM Sans", Font.PLAIN, 12));
    priorityLabel.setForeground(Color.GRAY); // small gray priority text initially

    // pack into a vertical panel (name above, priority below)
    JPanel textPanel = new JPanel(new GridLayout(2, 1));
    textPanel.setOpaque(false);
    textPanel.add(nameLabel);
    textPanel.add(priorityLabel);

    c.gridx = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    row.add(textPanel, c);

    // store references so selection code can find them later
    row.putClientProperty("nameLabel", nameLabel);
    row.putClientProperty("priorityLabel", priorityLabel);
    row.putClientProperty("priorityColor", priorityColor);

    //  Hover & Click 
    row.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            row.setBackground(new Color(240, 248, 255));
            row.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (selectedRowPanel != row) {
                row.setBackground(Color.WHITE);
                row.repaint();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            setSelectedRow(row, t);
        }
    });

    return row;
}

private void setSelectedRow(JPanel rowPanel, Task t) {
// Reset previous selection (only if exists)
    if (selectedRowPanel != null) {
        selectedRowPanel.setBackground(Color.WHITE);
        selectedRowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30,74,123), 1, true),
                new EmptyBorder(8,8,8,8)
        ));

        // restore previous labels' colors from client properties
        Object prevNameObj = selectedRowPanel.getClientProperty("nameLabel");
        Object prevPriorityObj = selectedRowPanel.getClientProperty("priorityLabel");
        Object prevColorObj = selectedRowPanel.getClientProperty("priorityColor");
        if (prevNameObj instanceof JLabel prevName) {
            if (prevColorObj instanceof Color pc) prevName.setForeground(pc);
            else prevName.setForeground(Color.BLACK);
        }
        if (prevPriorityObj instanceof JLabel prevPri) {
            prevPri.setForeground(Color.GRAY);
        }
    }

    // assign new selection
    selectedRowPanel = rowPanel;
    selectedTask = t;

    if (selectedRowPanel != null) {
        selectedRowPanel.setBackground(new Color(220,235,255));
        selectedRowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30,74,123), 2, true),
                new EmptyBorder(8,8,8,8)
        ));

        // set selected colors: name -> priorityColor (or black), priority -> dark
        Object nameObj = selectedRowPanel.getClientProperty("nameLabel");
        Object priObj = selectedRowPanel.getClientProperty("priorityLabel");
        Object colorObj = selectedRowPanel.getClientProperty("priorityColor");

        if (nameObj instanceof JLabel nameLbl) {
            if (colorObj instanceof Color pc) {
                nameLbl.setForeground(pc); // or Color.BLACK if you prefer
            } else {
                nameLbl.setForeground(Color.BLACK);
            }
        }
        if (priObj instanceof JLabel priLbl) {
            priLbl.setForeground(Color.DARK_GRAY);
        }
    }

    // enable edit/delete and set enabled foregrounds
    editButton.setEnabled(true);
    deleteButton.setEnabled(true);
    editButton.setForeground(Color.BLACK);
    deleteButton.setForeground(Color.BLACK);
    editButton.repaint();
    deleteButton.repaint();
}

private void clearSelection() {
 if (selectedRowPanel != null) {
        selectedRowPanel.setBackground(Color.WHITE);
        selectedRowPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(30,74,123), 1, true),
                new EmptyBorder(8,8,8,8)
        ));

        // reset the labels on the previously selected row
        Object nameObj = selectedRowPanel.getClientProperty("nameLabel");
        Object priObj = selectedRowPanel.getClientProperty("priorityLabel");
        Object colorObj = selectedRowPanel.getClientProperty("priorityColor");
        if (nameObj instanceof JLabel nameLbl) {
            if (colorObj instanceof Color pc) nameLbl.setForeground(pc);
            else nameLbl.setForeground(Color.BLACK);
        }
        if (priObj instanceof JLabel priLbl) {
            priLbl.setForeground(Color.GRAY);
        }
    }
    selectedRowPanel = null;
    selectedTask = null;

    // disable edit/delete and set disabled foregrounds
    editButton.setEnabled(false);
    deleteButton.setEnabled(false);
    editButton.setForeground(Color.GRAY);
    deleteButton.setForeground(Color.GRAY);
    editButton.repaint();
    deleteButton.repaint();
}

private void undoLastCompleted() {
    if (!completedStack.isEmpty()) {
        Task last = completedStack.pop();
        tasks.add(0, last);
        refreshTasks();
    } else {
        JOptionPane.showMessageDialog(this, "No completed tasks to undo.", "Undo", JOptionPane.INFORMATION_MESSAGE);
    }
}

private void showEditDialogForSelected() {
    if (selectedTask == null) {
        JOptionPane.showMessageDialog(this, "Select a task first.", "Edit", JOptionPane.WARNING_MESSAGE);
        return;
    }
    JTextField nameField = new JTextField(selectedTask.getName());
    JComboBox<String> pri = new JComboBox<>(new String[] {"High","Medium","Low"});
    pri.setSelectedIndex(selectedTask.getPriority() - 1);

    JPanel p = new JPanel(new GridLayout(0,1,4,4));
    p.add(new JLabel("Task name:"));
    p.add(nameField);
    p.add(new JLabel("Priority:"));
    p.add(pri);

    int res = JOptionPane.showConfirmDialog(this, p, "Edit Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (res == JOptionPane.OK_OPTION) {
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedTask.setName(newName);
        selectedTask.setPriority(pri.getSelectedIndex() + 1);
        refreshTasks();
        clearSelection();
    }
}

private void deleteSelectedTask() {
    if (selectedTask == null) {
        JOptionPane.showMessageDialog(this, "Select a task first.", "Delete", JOptionPane.WARNING_MESSAGE);
        return;
    }
    int confirm = JOptionPane.showConfirmDialog(this, "Delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
    if (confirm == JOptionPane.YES_OPTION) {
        tasks.remove(selectedTask);
        clearSelection();
        refreshTasks();
    }
}

private String escapeHtml(String s) {
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
}

// XP and Level System 
private void addXP(int amount) {
    currentXP += amount;
    
    // Check for level up
    while (currentXP >= XP_PER_LEVEL) {
        currentXP -= XP_PER_LEVEL;
        level++;
        JOptionPane.showMessageDialog(this, 
            "Level Up! You are now Level " + level + "!", 
            "Level Up", JOptionPane.INFORMATION_MESSAGE);
    }

    updateRewardsUI();
}

private void updateRewardsUI() {
// Update XP Bar
    if (xpBar != null) {
        xpBar.setMinimum(0);
        xpBar.setMaximum(XP_PER_LEVEL);
        xpBar.setValue(currentXP);
        xpBar.setStringPainted(true);
        xpBar.setString(currentXP + " / " + XP_PER_LEVEL + " XP");

        // Refresh visuals
        xpBar.revalidate();
        xpBar.repaint();
        SwingUtilities.invokeLater(() -> xpBar.repaint());
    }

    // Update XP label text
    if (lblXP != null) {
        lblXP.setText(currentXP + "/" + XP_PER_LEVEL + " XP");
    }

    // Update Level Title and Description
    if (lblLevelTitle != null && lblLevelDesc != null) {
        String levelTitle = getLevelTitle(level);
        String levelDesc = getLevelDescription(level);
        lblLevelTitle.setText("Level " + level + ": " + levelTitle);
        lblLevelDesc.setText(levelDesc);
    }

    // Update level list section
    updateLevelList();
}

// Level Title and Description 
private String getLevelTitle(int level) {
    switch (level) {
        case 1: return "The Beginning";
        case 2: return "Steady Steps";
        case 3: return "Rhythm Seeker";
        case 4: return "Focus Builder";
        case 5: return "Momentum Keeper";
        case 6: return "Peak Performer";
        case 7: return "Flow Finder";
        case 8: return "Discipline Driver";
        case 9: return "Growth Guardian";
        case 10: return "Legendary Learner";
        default: return "Beyond Limits";
    }
}

private String getLevelDescription(int level) {
    switch (level) {
        case 1: return "Every great journey begins with a single step.";
        case 2: return "You’re finding your pace and learning to stay consistent.";
        case 3: return "You’re turning routines into rhythm. Progress feels smoother.";
        case 4: return "Your focus sharpens; distractions start losing their power.";
        case 5: return "Momentum is on your side. You’re moving with intent.";
        case 6: return "You perform at your best, even when challenges arise.";
        case 7: return "You’ve found your flow. Effort feels effortless now.";
        case 8: return "Discipline fuels you; your drive stays steady and strong.";
        case 9: return "You’ve become a model of growth and persistence.";
        case 10: return "You’ve mastered your craft. Dedication has become your nature.";
        default: return "You’ve gone beyond. There are no limits left to reach.";
    }
}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Background;
    private javax.swing.JPanel ButtonsPanel;
    private javax.swing.JPanel Empty;
    private javax.swing.JToggleButton RewardsButton;
    private javax.swing.JToggleButton StudySessionsButton;
    private javax.swing.JToggleButton ToDoListButton;
    private javax.swing.JButton addButton1;
    private javax.swing.JButton backBtn;
    private javax.swing.JButton btnNext1;
    private javax.swing.JButton btnStart;
    private javax.swing.JButton btnStartPomodoro;
    private javax.swing.JButton btnStop;
    private javax.swing.JPanel cardPanel;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblLevelDesc;
    private javax.swing.JLabel lblLevelTitle;
    private java.awt.TextField lblSubjectValue;
    private java.awt.TextField lblTaskValue;
    private javax.swing.JLabel lblTimer;
    private javax.swing.JLabel lblXP;
    private javax.swing.JLabel logo_with_text;
    private javax.swing.ButtonGroup navGroup;
    private javax.swing.JPanel panelLevelList;
    private javax.swing.JButton pauseBtn;
    private javax.swing.JComboBox<String> priorityCombo;
    private javax.swing.JPanel rewardsPanel;
    private javax.swing.JLabel rewardsText;
    private javax.swing.JScrollPane scrollLevelList;
    private javax.swing.JButton sortbyPriorityButton;
    private javax.swing.JPanel studyPanel;
    private javax.swing.JPanel studyPanel2;
    private javax.swing.JLabel studySessionText;
    private javax.swing.JLabel studySessionText1;
    private javax.swing.JLabel subjectText;
    private javax.swing.JLabel subjectText1;
    private javax.swing.JTextField taskField;
    private javax.swing.JPanel taskListPanel;
    private javax.swing.JScrollPane taskListScroll;
    private javax.swing.JLabel taskPriorityText;
    private javax.swing.JLabel taskText;
    private javax.swing.JLabel taskText1;
    private javax.swing.JLabel todoListText;
    private javax.swing.JPanel todoPanel;
    private javax.swing.JTextField txtSubject;
    private javax.swing.JTextField txtTask;
    private javax.swing.JButton undoButton;
    private javax.swing.JProgressBar xpBar;
    // End of variables declaration//GEN-END:variables
}
