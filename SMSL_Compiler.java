import java.io.*;
import java.util.*;
import java.util.regex.*;

public class SMSL_Compiler {
    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String YELLOW = "\u001B[33m";

    private static final Map<String, String> TOKEN_TYPES = new LinkedHashMap<>();
    private static final Map<String, Integer> TOKEN_INDEX = new LinkedHashMap<>();

    public static List<String> lexicalErrors = new ArrayList<>();
    public static List<String> syntaxErrors = new ArrayList<>();
    public static List<String> semanticErrors = new ArrayList<>();
    public static List<String[]> tokens = new ArrayList<>(); // To store tokens

    static {
        TOKEN_TYPES.put("STATE", "\\bstate\\b");
        TOKEN_TYPES.put("TRANSITION", "\\btransition\\b");
        TOKEN_TYPES.put("EVENT", "\\bevent\\b");
        TOKEN_TYPES.put("ACTION", "\\baction\\b");
        TOKEN_TYPES.put("START", "\\bstart\\b");
        TOKEN_TYPES.put("END", "\\bend\\b");
        TOKEN_TYPES.put("IDENTIFIER", "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
        TOKEN_TYPES.put("NUMBER", "\\b\\d+\\b");
        TOKEN_TYPES.put("STRING", "\"[^\"]*\"|'[^']*'");
        TOKEN_TYPES.put("SYMBOL", "[{}();,:]");
        TOKEN_TYPES.put("WHITESPACE", "\\s+");

        int index = 1;
        for (String key : TOKEN_TYPES.keySet()) {
            TOKEN_INDEX.put(key, index++);
        }
    }

    public static String getTokenType(String lexeme) {
        for (Map.Entry<String, String> entry : TOKEN_TYPES.entrySet()) {
            if (Pattern.matches(entry.getValue(), lexeme)) {
                return entry.getKey();
            }
        }
        return "UNKNOWN"; 
    }

    public static int getTokenIndex(String tokenType) {
        return TOKEN_INDEX.getOrDefault(tokenType, 1);
    }

    public static void lexicalAnalyzer(List<String> lines) {
        try {
            System.out.println(CYAN
                    + " -----------------------------------------------------------------------------------------------"
                    + RESET);
            System.out.println(CYAN + "|   Line   |              Lexeme              |     Token      | Token Index |" + RESET);
            System.out.println(CYAN
                    + " -----------------------------------------------------------------------------------------------"
                    + RESET);

            int lineNo = 1;

            for (String line : lines) {
                line = line.split("//")[0].trim(); // Ignore comments
                if (line.isEmpty()) continue; // Skip empty lines

                Matcher matcher = Pattern
                        .compile("[a-zA-Z_][a-zA-Z0-9_]*|\\d+|\"[^\"]*\"|'[^']*'|[{}();,:]")
                        .matcher(line);

                while (matcher.find()) {
                    String lexeme = matcher.group();
                    String tokenType = getTokenType(lexeme);
                    int tokenIndex = getTokenIndex(tokenType);

                    if (!tokenType.equals("WHITESPACE")) {
                        if (tokenType.equals("UNKNOWN")) {
                            String error = "Line " + lineNo + ": Lexical Error - Unknown lexeme '" + lexeme + "'";
                            lexicalErrors.add(error);
                            System.out.println(RED + error + RESET);
                        } else {
                            // Save the token in the tokens list
                            tokens.add(new String[]{String.valueOf(lineNo), lexeme, tokenType, String.valueOf(tokenIndex)});
                            
                            // Print the token
                            System.out.printf(
                                    CYAN + "| %-8d " + MAGENTA + "| %-32s " + GREEN + "| %-14s " + YELLOW + "| %-11d |\n"
                                            + RESET,
                                    lineNo, lexeme, tokenType, tokenIndex);
                        }
                    }
                }
                lineNo++;
            }

            System.out.println(CYAN
                    + " -----------------------------------------------------------------------------------------------"
                    + RESET);
            syntaxAnalyzer(lines);
        } catch (Exception e) {
            lexicalErrors.add("Error during lexical analysis!");
        }

        if (!lexicalErrors.isEmpty()) {
            System.out.println(RED + "\nLexical Errors:" + RESET);
            lexicalErrors.forEach(error -> System.out.println(RED + error + RESET));
        }
    }

    public static void syntaxAnalyzer(List<String> lines) {
        System.out.println(CYAN + "\n------------------ Syntax Analysis ------------------" + RESET);
        int lineNo = 1;

        for (String line : lines) {
            line = line.split("//")[0].trim(); // Ignore comments
            if (line.isEmpty()) {
                lineNo++;
                continue; // Skip blank lines
            }

            // Tokenize the line for parse tree generation
            List<String[]> lineTokens = new ArrayList<>();
            Matcher matcher = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*|\\d+|\"[^\"]*\"|'[^']*'|[{}();,:]").matcher(line);
            while (matcher.find()) {
                String lexeme = matcher.group();
                String tokenType = getTokenType(lexeme);
                int tokenIndex = getTokenIndex(tokenType);
                if (!tokenType.equals("WHITESPACE")) {
                    lineTokens.add(new String[]{String.valueOf(lineNo), lexeme, tokenType, String.valueOf(tokenIndex)});
                }
            }

            // Check if the line has valid syntax
            if (!parseLine(line)) {
                String error = "Line " + lineNo + ": Syntax Error [Error]";
                syntaxErrors.add(error);
                System.out.println(RED + error + RESET);
            } else {
                System.out.println(GREEN + "Line " + lineNo + ": Valid syntax [Valid]" + RESET);
            }

            // Display the parse tree for the line
            processLineTokens(lineNo, lineTokens);

            lineNo++;
        }
    }

    public static void syntaxAnalyzerUsingTokens() {
        System.out.println(CYAN + "\n------------------ Syntax Analysis ------------------" + RESET);

        int currentLine = 1;
        List<String[]> currentLineTokens = new ArrayList<>();

        for (String[] token : tokens) {
            int tokenLine = Integer.parseInt(token[0]);

            // Process tokens for the previous line when the line changes
            if (tokenLine != currentLine) {
                processLineTokens(currentLine, currentLineTokens);
                currentLine = tokenLine;
                currentLineTokens.clear();
            }

            currentLineTokens.add(token);
        }

        // Process the last line's tokens
        if (!currentLineTokens.isEmpty()) {
            processLineTokens(currentLine, currentLineTokens);
        }
    }

    private static void processLineTokens(int lineNo, List<String[]> lineTokens) {
        System.out.println(CYAN + "Line " + lineNo + ":" + RESET);

        if (lineTokens.isEmpty()) {
            System.out.println(RED + "  Syntax Error: Empty line" + RESET);
            return;
        }

        // Example hierarchical output for parse tree
        System.out.println("  Parse Tree:");
        System.out.println("  Root");

        for (String[] token : lineTokens) {
            String lexeme = token[1];
            String tokenType = token[2];

            // Indent based on token type for hierarchical structure
            if (tokenType.equals("STATE") || tokenType.equals("TRANSITION") || tokenType.equals("ACTION")) {
                System.out.println("    ├── " + tokenType + " -> " + lexeme);
            } else if (tokenType.equals("IDENTIFIER")) {
                System.out.println("    │   ├── Identifier -> " + lexeme);
            } else {
                System.out.println("    │   ├── Symbol -> " + lexeme);
            }
        }
    }

    public static boolean parseLine(String line) {
        if (line.startsWith("state ")) {
            return parseState(line);
        } else if (line.startsWith("transition")) {
            return parseTransition(line);
        } else if (line.startsWith("action")) {
            return parseAction(line);
        } else if (line.equals("start;") || line.equals("end;")) {
            return true; // Valid start or end statement
        } else if (line.equals("{") || line.equals("}")) {
            return true; // Valid braces
        }
        return false; // Unrecognized line
    }

    public static boolean parseState(String line) {
        // Grammar: state <identifier> {
        String[] parts = line.split("\\s+");
        if (parts.length == 3 && parts[0].equals("state") && parts[2].equals("{")) {
            return true; // Valid state declaration
        }
        return false; // Invalid state declaration
    }

    public static boolean parseTransition(String line) {
        // Grammar: transition <identifier>: event <identifier> -> <identifier> {
        String[] parts = line.split("\\s*:\\s*|\\s*->\\s*");
        if (parts.length < 3) {
            return false;
        }
        String[] transitionParts = parts[0].split("\\s+");
        return transitionParts.length == 2 && transitionParts[0].equals("transition") && parts[2].endsWith("{");
    }

    public static boolean parseAction(String line) {
        // Grammar: action <identifier>;
        String[] parts = line.split("\\s+");
        return parts.length == 2 && parts[0].equals("action") && parts[1].endsWith(";");
    }

    public static void semanticAnalyzer(List<String> lines) {
        System.out.println(CYAN + "\n------------------ Semantic Analysis ------------------" + RESET);
        Set<String> definedStates = new HashSet<>();
        Set<String> referencedStates = new HashSet<>();
        Map<String, Boolean> stateHasContent = new HashMap<>();
        Map<String, Integer> stateLineMap = new HashMap<>(); // Track line numbers for states

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).split("//")[0].trim(); // Ignore comments
            if (line.isEmpty()) {
                continue; // Skip blank lines
            }

            if (line.startsWith("state ")) {
                String stateName = line.split("\\s+")[1];
                if (definedStates.contains(stateName)) {
                    semanticErrors.add("Line " + (i + 1) + ": Semantic Error - Duplicate State Declaration: '" + stateName + "'");
                } else {
                    definedStates.add(stateName);
                    stateHasContent.put(stateName, false); // Initialize state content check
                    stateLineMap.put(stateName, i + 1); // Store the line number for the state
                }
            } else if (line.startsWith("transition")) {
                String[] parts = line.split("\\s*:\\s*|\\s*->\\s*");
                if (parts.length < 3) {
                    semanticErrors.add("Line " + (i + 1) + ": Semantic Error - Invalid Transition Format");
                } else {
                    String targetState = parts[2].replace("{", "").trim();
                    referencedStates.add(targetState);
                }
            } else if (line.startsWith("action")) {
                for (String state : definedStates) {
                    if (lines.get(i - 1).trim().equals("state " + state + " {")) {
                        stateHasContent.put(state, true); // Mark state as having content
                    }
                }
            }
        }

        // Check for undefined states
        for (String state : referencedStates) {
            if (!definedStates.contains(state)) {
                semanticErrors.add("Semantic Error - Undefined State Reference: '" + state + "'");
            }
        }

        // Check for states with no actions or transitions
        for (Map.Entry<String, Boolean> entry : stateHasContent.entrySet()) {
            if (!entry.getValue()) {
                int lineNumber = stateLineMap.get(entry.getKey()); // Retrieve the line number from stateLineMap
                semanticErrors.add("Line " + lineNumber + ": Semantic Error - State with No Content: '" + entry.getKey() + "'");
            }
        }
    }

    public static void semanticAnalyzerUsingTokens() {
        System.out.println(CYAN + "\n------------------ Semantic Analysis ------------------" + RESET);

        Set<String> definedStates = new HashSet<>();
        Set<String> referencedStates = new HashSet<>();
        Map<String, Boolean> stateHasContent = new HashMap<>();
        Map<String, Integer> stateLineMap = new HashMap<>(); // Track line numbers for states

        int currentLine = 1;
        List<String[]> currentLineTokens = new ArrayList<>();

        for (String[] token : tokens) {
            int tokenLine = Integer.parseInt(token[0]);

            // Process tokens for the previous line when the line changes
            if (tokenLine != currentLine) {
                processSemanticTokens(currentLine, currentLineTokens, definedStates, referencedStates, stateHasContent, stateLineMap);
                currentLine = tokenLine;
                currentLineTokens.clear();
            }

            currentLineTokens.add(token);
        }

        // Process the last line's tokens
        if (!currentLineTokens.isEmpty()) {
            processSemanticTokens(currentLine, currentLineTokens, definedStates, referencedStates, stateHasContent, stateLineMap);
        }

        // Check for undefined states
        for (String state : referencedStates) {
            if (!definedStates.contains(state)) {
                semanticErrors.add("Semantic Error - Undefined State Reference: '" + state + "'");
            }
        }

        // Check for states with no actions or transitions
        for (Map.Entry<String, Boolean> entry : stateHasContent.entrySet()) {
            if (!entry.getValue()) {
                int lineNumber = stateLineMap.get(entry.getKey()); // Retrieve the line number from stateLineMap
                semanticErrors.add("Line " + lineNumber + ": Semantic Error - State with No Content: '" + entry.getKey() + "'");
            }
        }
    }

    private static void processSemanticTokens(
        int lineNo,
        List<String[]> lineTokens,
        Set<String> definedStates,
        Set<String> referencedStates,
        Map<String, Boolean> stateHasContent,
        Map<String, Integer> stateLineMap
    ) {
        if (lineTokens.isEmpty()) {
            return; // Skip empty lines
        }

        String firstTokenType = lineTokens.get(0)[2]; // Get the type of the first token

        if (firstTokenType.equals("STATE")) {
            // Handle state declaration
            if (lineTokens.size() >= 2 && lineTokens.get(1)[2].equals("IDENTIFIER")) {
                String stateName = lineTokens.get(1)[1]; // Get the state name
                if (definedStates.contains(stateName)) {
                    // Duplicate state declaration
                    semanticErrors.add("Line " + lineNo + ": Semantic Error - Duplicate State Declaration: '" + stateName + "'");
                } else {
                    definedStates.add(stateName);
                    stateHasContent.put(stateName, false); // Initialize state content check
                    stateLineMap.put(stateName, lineNo); // Store the line number for the state
                }
            }
        } else if (firstTokenType.equals("TRANSITION")) {
            // Handle transition declaration
            if (lineTokens.size() >= 6 && lineTokens.get(3)[2].equals("EVENT") && lineTokens.get(5)[2].equals("IDENTIFIER")) {
                String targetState = lineTokens.get(5)[1]; // Get the target state
                referencedStates.add(targetState);
            } else {
                semanticErrors.add("Line " + lineNo + ": Semantic Error - Invalid Transition Format");
            }
        } else if (firstTokenType.equals("ACTION")) {
            // Handle action declaration
            for (String state : definedStates) {
                if (stateHasContent.containsKey(state)) {
                    stateHasContent.put(state, true); // Mark state as having content
                }
            }
        }
    }

    public static List<String[]> generateIntermediateCode(List<String> lines) {
        List<String[]> quadruples = new ArrayList<>();

        for (String line : lines) {
            line = line.split("//")[0].trim(); // Ignore comments
            if (line.isEmpty()) {
                continue; // Skip blank lines
            }
            if (line.startsWith("action")) {
                String actionName = line.split("\\s+", 2)[1].replace(";", "").trim();
                quadruples.add(new String[]{"ACTION", actionName, "", ""});
            } else if (line.startsWith("transition")) {
                String[] parts = line.split("\\s*:\\s*|\\s*->\\s*");
                if (parts.length >= 3) {
                    String transitionName = parts[0].split("\\s+")[1].trim();
                    String eventName = parts[1].trim();
                    String targetState = parts[2].replace("{", "").trim();
                    quadruples.add(new String[]{"TRANSITION", transitionName, eventName, targetState});
                } else {
                    System.out.println(RED + "Error: Invalid transition format in line: " + line + RESET);
                }
            }
        }

        return quadruples;
    }

    public static void optimizeCode(List<String[]> quadruples) {
        // Eliminate common subexpressions
        Set<String> seenExpressions = new HashSet<>();
        for (int i = 0; i < quadruples.size(); i++) {
            String[] quad = quadruples.get(i);
            String expression = String.join(",", quad);
            if (seenExpressions.contains(expression)) {
                quadruples.remove(i);
                i--;
            } else {
                seenExpressions.add(expression);
            }
        }

        // Transition merging
        Map<String, String> mergedTransitions = new HashMap<>();
        for (int i = 0; i < quadruples.size(); i++) {
            String[] quad = quadruples.get(i);
            if (quad[0].equals("TRANSITION")) {
                String key = quad[2] + "," + quad[3]; // Event and target state
                if (mergedTransitions.containsKey(key)) {
                    // Merge transitions
                    quad[1] = mergedTransitions.get(key) + "_" + quad[1];
                    quadruples.set(i, quad);
                } else {
                    mergedTransitions.put(key, quad[1]);
                }
            }
        }
    }

    public static void printQuadruplesAsTable(List<String[]> quadruples) {
        System.out.println(CYAN + "\n------------------Code Optimized ------------------" + RESET);
        System.out.printf(CYAN + "| %-15s | %-15s | %-15s | %-15s |\n" + RESET, "Operation", "Arg1", "Arg2", "Result");
        System.out.println(CYAN + "-------------------------------------------------------------" + RESET);
        for (String[] quad : quadruples) {
            System.out.printf(CYAN + "| %-15s | %-15s | %-15s | %-15s |\n" + RESET, quad[0], quad[1], quad[2], quad[3]);
        }
        System.out.println(CYAN + "-------------------------------------------------------------" + RESET);
    }

    public static void printIntermediateCodeAsTable(List<String[]> quadruples) {
        System.out.println(CYAN + "\n------------------ Intermediate Code ------------------" + RESET);
        System.out.printf(CYAN + "| %-15s | %-15s | %-15s | %-15s |\n" + RESET, "Operation", "Arg1", "Arg2", "Result");
        System.out.println(CYAN + "-------------------------------------------------------------" + RESET);
        for (String[] quad : quadruples) {
            System.out.printf(CYAN + "| %-15s | %-15s | %-15s | %-15s |\n" + RESET, quad[0], quad[1], quad[2], quad[3]);
        }
        System.out.println(CYAN + "-------------------------------------------------------------" + RESET);
    }

    public static void displayErrors() {
        if (!semanticErrors.isEmpty()) {
            System.out.println(RED + "\nSemantic Errors:" + RESET);

            // Sort semantic errors by line number
            semanticErrors.sort((a, b) -> {
                try {
                    int lineA = Integer.parseInt(a.replaceAll(".*Line (\\d+).*", "$1"));
                    int lineB = Integer.parseInt(b.replaceAll(".*Line (\\d+).*", "$1"));
                    return Integer.compare(lineA, lineB);
                } catch (NumberFormatException e) {
                    // If no line number is found, keep the error at the end
                    return a.contains("Line") ? -1 : 1;
                }
            });

            // Display sorted errors
            semanticErrors.forEach(error -> System.out.println(RED + error + RESET));
        } else {
            System.out.println(GREEN + "\nNo semantic errors found." + RESET);
        }
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("noerror_smsl.txt"))) { // Updated to read from test_cases.txt
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println(RED + "Error reading file!" + RESET);
            return;
        }

        // Perform lexical analysis
        lexicalAnalyzer(lines);

        // Stop further processing if lexical errors are found
        if (!lexicalErrors.isEmpty()) {
            System.out.println(RED + "\nCompilation stopped due to lexical errors." + RESET);
            return;
        }

        // Perform syntax analysis
        syntaxAnalyzer(lines);

        // Stop further processing if syntax errors are found
        if (!syntaxErrors.isEmpty()) {
            System.out.println(RED + "\nCompilation stopped due to syntax errors." + RESET);
            return;
        }

        // Perform semantic analysis
        semanticAnalyzer(lines);

        // Stop further processing if semantic errors are found
        if (!semanticErrors.isEmpty()) {
            displayErrors();
            System.out.println(RED + "\nCompilation stopped due to semantic errors." + RESET);
            return;
        }

        // Generate intermediate code
        List<String[]> quadruples = generateIntermediateCode(lines);
        printIntermediateCodeAsTable(quadruples);

        // Optimize the code
        optimizeCode(quadruples);
        printQuadruplesAsTable(quadruples);

        // Display errors (if any)
        displayErrors();
    }
}