# SMSL Project

## Overview
The SMSL (State Machine Specification Language) project is a compiler implementation for a domain-specific language designed to define state machines. The project includes lexical analysis, syntax analysis, semantic analysis, intermediate code generation, and code optimization.

## Features
1. **Lexical Analysis**:
   - Identifies tokens such as `state`, `transition`, `action`, symbols, and identifiers.
   - Reports unknown lexemes as lexical errors with line numbers.

2. **Syntax Analysis**:
   - Uses a top-down parsing approach to validate the structure of the SMSL code.
   - Reports syntax errors with specific messages and line numbers.

3. **Semantic Analysis**:
   - Checks for undefined state references.
   - Identifies states with no actions or transitions.
   - Reports semantic errors with clear messages.

4. **Intermediate Code Generation**:
   - Converts SMSL constructs into quadruples for easier processing.
   - Represents actions and transitions in a structured format.

5. **Code Optimization**:
   - Removes duplicate consecutive actions.
   - Eliminates common subexpressions to improve efficiency.

## Project Structure
- **LexicalAnalyzer.java**: Main file containing the implementation of lexical, syntax, and semantic analysis, as well as intermediate code generation and optimization.
- **test_cases.txt**: Contains sample SMSL code for testing.

## How to Run
1. Place your SMSL code in a file named `smsl_spec.txt` in the project directory.
2. Compile and run the `LexicalAnalyzer.java` file using a Java compiler.
   ```bash
   javac LexicalAnalyzer.java
   java LexicalAnalyzer
   ```
3. View the output, including tokens, errors, intermediate code, and optimized code, in the terminal.

## Example Input
```plaintext
state S1 {
    action A1;
    transition T1: event E1 -> S2 {
        action A2;
    }
}

state S2 {
    action A3;
}
```

## How to Contribute
1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and push them to your fork.
4. Submit a pull request with a detailed description of your changes.

## License
This project is licensed under the MIT License. Feel free to use and modify it as needed.