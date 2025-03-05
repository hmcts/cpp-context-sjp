package uk.gov.moj.sjp.it;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FailSafeReportsParserDetails {
    private static final String directory = "/Users/rohitprashar/Downloads/failsafe-reports";
    private static final String outputFilePath = directory + "/test-results.csv";

    public static void main(String[] args) {
        try (var filesStream = Files.walk(Paths.get(directory))) {
            List<Path> files = filesStream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".xml"))
                    .toList();

            List<TestResult> allResults = new ArrayList<>();
            for (Path file : files) {
                List<TestResult> results = parseXmlFile(file.toString());
                allResults.addAll(results);
            }


            allResults.sort(Comparator.comparingDouble(r -> -Double.parseDouble(r.time)));


            System.out.println("\n--- Ranked Test Results ---");
            printRankedResults(allResults);


            try (PrintWriter pw = new PrintWriter(outputFilePath)) {
                pw.println("\"Rank\",\"Class Name\",\"Test Name\",\"Time\"");
                writeToCSV(allResults, pw);
            }

            generateSummary(allResults);

        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
        }
    }

    public static void writeToCSV(List<TestResult> results, PrintWriter pw) {
        int rank = 1;
        for (TestResult result : results) {
            pw.printf("\"%d\",\"%s\",\"%s\",\"%s\"%n", rank++, result.className, result.name, result.time);
        }
    }

    private static void printRankedResults(List<TestResult> results) {
        int rank = 1;
        for (TestResult result : results) {
            System.out.printf("Rank: %d, Class: %s, Test: %s, Time: %s seconds%n",
                    rank++, result.className, result.name, result.time);
        }
    }

    public static List<TestResult> parseXmlFile(String filename) {
        List<TestResult> results = new ArrayList<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList testcases = doc.getElementsByTagName("testcase");
            for (int i = 0; i < testcases.getLength(); i++) {
                Node testcaseNode = testcases.item(i);
                if (testcaseNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element testcaseElement = (Element) testcaseNode;

                    String className = testcaseElement.getAttribute("classname");
                    String name = testcaseElement.getAttribute("name");
                    String time = testcaseElement.getAttribute("time");

                    TestResult result = new TestResult(className, name, time);
                    results.add(result);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing file " + filename + ": " + e.getMessage());
        }
        return results;
    }

    private static void generateSummary(List<TestResult> results) {
        double totalTimeInSeconds = results.stream().mapToDouble(r -> Double.parseDouble(r.time)).sum();
        double totalTimeInMinutes = totalTimeInSeconds / 60;
        double averageTimeInSeconds = results.stream().mapToDouble(r -> Double.parseDouble(r.time)).average().orElse(0.0);
        double averageTimeInMinutes = averageTimeInSeconds / 60;
        TestResult slowestTest = results.stream().max(Comparator.comparingDouble(r -> Double.parseDouble(r.time))).orElse(null);
        TestResult fastestTest = results.stream().min(Comparator.comparingDouble(r -> Double.parseDouble(r.time))).orElse(null);

        System.out.println("\n--- Summary ---");
        System.out.println("Total Tests: " + results.size());
        System.out.printf("Total Time: %.2f minutes\n", totalTimeInMinutes);
        System.out.printf("Average Time: %.2f minutes\n", averageTimeInMinutes);
        if (slowestTest != null) {
            System.out.printf("Slowest Test: %s.%s (%.2f seconds)\n", slowestTest.className, slowestTest.name, Double.parseDouble(slowestTest.time));
        }
        if (fastestTest != null) {
            System.out.printf("Fastest Test: %s.%s (%.2f seconds)\n", fastestTest.className, fastestTest.name, Double.parseDouble(fastestTest.time));
        }
    }

    public static class TestResult {
        String className;
        String name;
        String time;

        public TestResult(String className, String name, String time) {
            this.className = className;
            this.name = name;
            this.time = time;
        }
    }
}
