Complex banking application built in Java, showcasing modern software design principles.

Key Features :

*   **Multiple Account Types:** Handles Classic, Savings, and Business accounts, catering to a wide range of users.
*   **Card Management:**  Each account can manage multiple cards, including Classic and secure **One-Time Usage** cards.
*   **Financial Reporting:** Access detailed payment history and insightful financial reports.
*   **Cashback & Rewards:** Option to benefit from varied cashback programs on purchases from specific merchants.
*   **Service Plans:** Choose from different service plans offering benefits like reduced interest rates and bonus cashback.
*   **Group Payments:**  Facilitate custom payments between multiple users.

## Technical Highlights

This application demonstrates object-oriented design and used several design patterns:

*   **Builder:** For constructing objects that handle card and account creation in a simplified manner.
*   **Singleton:** To ensure controlled access to the bank data.
*   **Factory:**  For abstracting the creation of different commands.
*   **Visitor:** To enable different types of commands to be executed on account and card objects without modifying their classes.

To run this banking application, follow these steps:

1.  **Prerequisites:** Ensure you have the following installed:
    *   **Java Development Kit (JDK):**  Download and install the latest JDK from Oracle.
    *   **Apache Maven:** Download and install Apache Maven. This project uses Maven for dependency management and building.

2.  **Project Setup:**
    *   **Clone the Repository:** Clone the project repository to your local machine.
    *   **Open Project:** Browse to the root directory of your cloned project (the directory containing `pom.xml`) and open it.

5.  **Running the Application:**
    *   **Entry Point:** The main application logic starts in the `action` function within the `Main` class of the project.
    *   **Execution Flow:**
        *   The application reads input data from JSON files. These files includ different types of commands that show the app functionalities.
        *   After each action, the result is written to a JSON output file.
    *   **Running:**  You can run the application directly by locating the `Main` class and running the `main` method.

## License

This project is licensed under the **MIT License**.

You are free to use, modify, and distribute this software for any purpose, even commercially.  The only requirement is that you include the original copyright and permission notice in any copies of the software.

**The software is provided "as is," without any warranty.**
