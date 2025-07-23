package com.widedot.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class BlackRectangleFinder {
    private static final int BLACK_THRESHOLD = 30; // RGB values below this are considered black
    private static final int MIN_RECT_WIDTH = 10;  // Minimum width to consider as a rectangle
    private static final int MIN_RECT_HEIGHT = 10; // Minimum height to consider as a rectangle
    private static final double COLUMN_GROUPING_THRESHOLD = 20.0; // Pixels tolerance for grouping into columns

    static class Rectangle {
        int x;
        int y;
        int width;
        int height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return String.format("{\"x\":%d,\"y\":%d,\"width\":%d,\"height\":%d}", x, y, width, height);
        }
    }

    static class Column {
        int x;
        int index; // Column index (0-based)
        List<Rectangle> rectangles;

        Column(int x) {
            this.x = x;
            this.rectangles = new ArrayList<>();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"col\":").append(index).append(",\"rectangles\":[");
            for (int i = 0; i < rectangles.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(rectangles.get(i).toString());
            }
            sb.append("]}");
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java BlackRectangleFinder <input_image.png>");
            System.exit(1);
        }

        try {
            String inputPath = args[0];
            List<Column> columns = findBlackRectangles(inputPath);
            String jsonOutput = generateJson(columns);
            
            // Generate output file path by replacing .png with .json
            String outputPath = inputPath.substring(0, inputPath.lastIndexOf('.')) + ".json";
            saveToFile(jsonOutput, outputPath);
            
            System.out.println("JSON data saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
            System.exit(1);
        }
    }

    private static List<Column> findBlackRectangles(String imagePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(imagePath));
        List<Rectangle> rectangles = new ArrayList<>();
        boolean[][] visited = new boolean[image.getHeight()][image.getWidth()];

        // Find all black rectangles
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (!visited[y][x] && isBlack(image.getRGB(x, y))) {
                    Rectangle rect = expandRectangle(image, x, y, visited);
                    if (rect.width >= MIN_RECT_WIDTH && rect.height >= MIN_RECT_HEIGHT) {
                        rectangles.add(rect);
                    }
                }
            }
        }

        // Group rectangles into columns
        List<Column> columns = groupIntoColumns(rectangles);
        
        // Assign column indices from left to right
        for (int i = 0; i < columns.size(); i++) {
            columns.get(i).index = i;
        }
        
        return columns;
    }

    private static boolean isBlack(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r < BLACK_THRESHOLD && g < BLACK_THRESHOLD && b < BLACK_THRESHOLD;
    }

    private static Rectangle expandRectangle(BufferedImage image, int startX, int startY, boolean[][] visited) {
        int width = 0;
        int height = 0;

        // Find width
        int x = startX;
        while (x < image.getWidth() && isBlack(image.getRGB(x, startY))) {
            width++;
            x++;
        }

        // Find height
        int y = startY;
        while (y < image.getHeight() && isBlack(image.getRGB(startX, y))) {
            height++;
            y++;
        }

        // Mark as visited
        for (y = startY; y < startY + height; y++) {
            for (x = startX; x < startX + width; x++) {
                visited[y][x] = true;
            }
        }

        return new Rectangle(startX, startY, width, height);
    }

    private static List<Column> groupIntoColumns(List<Rectangle> rectangles) {
        Map<Integer, Column> columnMap = new TreeMap<>();

        // Group rectangles by their x coordinate (with tolerance)
        for (Rectangle rect : rectangles) {
            boolean added = false;
            for (Column column : columnMap.values()) {
                if (Math.abs(column.x - rect.x) <= COLUMN_GROUPING_THRESHOLD) {
                    column.rectangles.add(rect);
                    added = true;
                    break;
                }
            }
            if (!added) {
                Column newColumn = new Column(rect.x);
                newColumn.rectangles.add(rect);
                columnMap.put(rect.x, newColumn);
            }
        }

        // Sort rectangles in each column by y coordinate (bottom to top)
        for (Column column : columnMap.values()) {
            column.rectangles.sort((r1, r2) -> Integer.compare(r2.y, r1.y));
        }

        return new ArrayList<>(columnMap.values());
    }

    private static String generateJson(List<Column> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append("  ").append(columns.get(i).toString());
        }
        sb.append("\n]");
        return sb.toString();
    }

    private static void saveToFile(String content, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        }
    }
} 