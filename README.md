# Quiz Application (Java Swing)

A simple **Quiz Application** built using **Java 8, Swing, OOP, and File Handling**, compatible with **Windows 7 (32-bit)**.  
The app allows users to take quizzes, view their score, and check a leaderboard.

---

## 🚀 Features
- 📝 Multiple Choice Questions (MCQ) loaded from external `questions.txt` file.
- 👤 Username input before starting the quiz.
- 🎯 Score calculation with instant feedback (Correct / Wrong).
- ⏱️ Timer for each question.
- 🏆 Leaderboard showing usernames and scores (saved in `leaderboard.txt`).
- 📂 File handling for reading questions and storing results.
- 🔄 Clean UI using Java Swing (CardLayout).

---

## 📂 Project Structure
```

QuizSwingApp.java      # Main Application Source Code
questions.txt          # Contains 100+ quiz questions
leaderboard.txt        # Stores leaderboard data (auto-generated after quiz)
README.md              # Project Documentation

````

---

## ⚙️ Requirements
- Java 8 (JDK 1.8)
- Works on **Windows 7 (32-bit)** or higher
- Any IDE (VS Code, NetBeans, IntelliJ) OR simple command line

---

## ▶️ How to Run

### 1. Clone / Copy project files
Place `QuizSwingApp.java` and `questions.txt` in the same folder.

### 2. Compile the Java file
```bash
javac QuizSwingApp.java
````

### 3. Run the application

```bash
java QuizSwingApp
```

### 4. Enter Username

* Enter your name before starting the quiz.
* Questions will appear one by one with options (A, B, C, D).

### 5. After Quiz

* Your score will be displayed.
* Result will be saved in `leaderboard.txt`.

---

## 📝 Question File Format

`questions.txt` must follow this format (semicolon-separated):

```
Question;OptionA;OptionB;OptionC;OptionD;CorrectOptionIndex
```

👉 Example:

```
What is the capital of India?;New Delhi;Mumbai;Kolkata;Chennai;0
Which planet is known as the Red Planet?;Venus;Mars;Jupiter;Saturn;1
```

* `CorrectOptionIndex` = 0 (A), 1 (B), 2 (C), 3 (D)

---

## 🏆 Leaderboard File

`leaderboard.txt` is automatically created after each quiz.
It stores data like:

```
Ayushi : 8/10
Rahul : 6/10
```

---

## 💡 Future Enhancements

* Add difficulty levels (Easy, Medium, Hard)
* Add categories (Science, GK, Computers, Sports)
* Export leaderboard to CSV/Excel
* Add sounds & animations for better UI experience

---

## 👩‍💻 Author

Developed by **Ayushi Tomar** 
```
