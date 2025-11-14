![Made with Java](https://img.shields.io/badge/Made%20with-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![NetBeans](https://img.shields.io/badge/IDE-NetBeans-1B6AC6?style=for-the-badge&logo=apache-netbeans-ide&logoColor=white)

  # 📝 TaskMate
  **A study productivity companion for students and lifelong learners.**

</div>

---

## 📌 Overview

**TaskMate** is an all-in-one productivity system that helps users stay organized, focused, and motivated.  
It blends a **prioritized to-do list**, **structured Pomodoro study sessions**, and a **gamified XP leveling system** to make productivity more rewarding and sustainable.

TaskMate supports users by:  
✔ Managing academic or personal tasks through a clean to-do list  
✔ Guiding focused study sessions using a 6-interval Pomodoro cycle  
✔ Tracking progress through XP and level-ups  
✔ Providing a supportive “study buddy” experience  

---

## 🎯 Features

### **1. To-Do List**
- Add, edit, delete, complete, and undo tasks  
- Priority levels: **High / Medium / Low**  
- Tasks are **color-coded** and sorted using **Bubble Sort**  
- Completed tasks grant **+10 XP**  
- Undo uses a **Stack** to restore the latest completed task  
- Input validation for empty entries  
- Scrollable task list  

---

### **2. Pomodoro Study Sessions**
- Follows a **6-interval Pomodoro cycle**:  
  - 25 mins work × 4  
  - 5 mins short break × 3  
  - 15 mins long break  
- Start, pause, resume, skip, and stop sessions  
- Requires subject/task input for clarity  
- Logs each session with:
  - Subject  
  - Duration  
  - Completion time  
- Completing/stopping a session grants **+25 XP**  
- Uses **Timer**, **arrays**, and **DefaultTableModel**  
- Automatically transitions between intervals  

---

### **3. Rewards System**
Earn XP:  
- **+10 XP** per completed task  
- **+25 XP** per completed study session  

Every **100 XP = Level Up**  

Includes:  
- Level tracker  
- XP progress bar  
- Scrollable levels list  
- Level-up popup notification  

---

## 🎯 Project Purpose

**TaskMate** addresses common problems students face:  
📌 Procrastination  
📌 Poor time management  
📌 Forgetting tasks  
📌 Lack of focus  
📌 Low motivation  

By integrating organization, guided sessions, and gamified rewards, TaskMate helps students build better habits and stay in control of their academic journey.

Supports **SDG 4: Quality Education** through improved self-management and discipline.

---

## 🛠️ Technologies & Data Structures Used

### **Core Data Structures**

| Feature | Data Structure | Purpose |
|--------|----------------|---------|
| To-Do List | `ArrayList<Task>` | Store & manage active tasks |
| Undo Feature | `Stack<Task>` | Restore last completed task (LIFO) |
| Pomodoro Intervals | `String[]` + `int[]` | Fixed cycle names & durations |
| Session Logs | `DefaultTableModel` | Record completed study sessions |

### 📌 Why These Data Structures?
- **ArrayList** — flexible and perfect for dynamic lists  
- **Stack** — ideal for LIFO undo functions  
- **Arrays** — efficient for fixed Pomodoro sequences  
- **DefaultTableModel** — clean structure for logging sessions  

---

## 🧪 Test Cases Summary

TaskMate has been tested for:

✔ Launching the system  
✔ Adding, editing, deleting, and sorting tasks  
✔ Completing and undoing tasks  
✔ Handling invalid input  
✔ Starting, pausing, resuming, and stopping Pomodoro sessions  
✔ Session logging and XP gains  
✔ Level tracking, XP updates, and level-up events  
✔ Scrolling and UI navigation  

All features perform as expected based on actual output.

---

## 👤 Who Can Use TaskMate?
- Students  
- Remote workers  
- Professionals  
- Anyone who wants a structured, motivational productivity tool  

---

## 👩🏻‍💻 Author
Developed by **Adrielle Agbayani**<br>
Final requirement for **Data Structures and Algorithm Course at National University – Manila**
