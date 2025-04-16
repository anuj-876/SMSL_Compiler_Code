import java.io.*;
import java.util.*;
import java.util.regex.*;

public class LexicalAnalyzer {
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

            if (!parseLine(line)) {
                String error = "Line " + lineNo + ": Syntax Error [Error]";
                syntaxErrors.add(error);
                System.out.println(RED + error + RESET);
            } else {
                System.out.println(GREEN + "Line " + lineNo + ": Valid syntax [Valid]" + RESET);
            }

            lineNo++;
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
        return parts.length == 3 && parts[0].equals("state") && parts[2].equals("{");
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
                definedStates.add(stateName);
                stateHasContent.put(stateName, false); // Initialize state content check
                stateLineMap.put(stateName, i + 1); // Store the line number for the state
            } else if (line.startsWith("transition")) {
                String[] parts = line.split("\\s*:\\s*|\\s*->\\s*");
                if (parts.length < 3) {
                    semanticErrors.add("Line " + (i + 1) + ": Semantic Error - Invalid transition format.");
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
                semanticErrors.add("Semantic Error: Undefined state referenced - " + state);
            }
        }

        // Check for states with no actions or transitions
        for (Map.Entry<String, Boolean> entry : stateHasContent.entrySet()) {
            if (!entry.getValue()) {
                int lineNumber = stateLineMap.get(entry.getKey()); // Retrieve the line number from stateLineMap
                semanticErrors.add("Semantic Error: Line " + lineNumber);
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
        // Remove duplicate consecutive actions
        for (int i = 0; i < quadruples.size() - 1; i++) {
            String[] current = quadruples.get(i);
            String[] next = quadruples.get(i + 1);
            if (current[0].equals("ACTION") && next[0].equals("ACTION") && current[1].equals(next[1])) {
                quadruples.remove(i + 1);
                i--;
            }
        }

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
                int lineA = Integer.parseInt(a.replaceAll(".*Line (\\d+).*", "$1"));
                int lineB = Integer.parseInt(b.replaceAll(".*Line (\\d+).*", "$1"));
                return Integer.compare(lineA, lineB);
            });

            // Display sorted errors
            semanticErrors.forEach(error -> System.out.println(RED + error + RESET));
        } else {
            System.out.println(GREEN + "\nNo semantic errors found." + RESET);
        }
    }

    public static void main(String[] args) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("semantic_error.txt"))) { // Updated to read from test_cases.txt
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.out.println(RED + "Error reading file!" + RESET);
            return;
        }

        lexicalAnalyzer(lines);
        semanticAnalyzer(lines);
        List<String[]> quadruples = generateIntermediateCode(lines);
        printIntermediateCodeAsTable(quadruples);
        optimizeCode(quadruples);
        printQuadruplesAsTable(quadruples);
        displayErrors();
    }
}
