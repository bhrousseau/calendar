package com.widedot.calendar.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImagePositionFinder {
    private static final double DEFAULT_TOLERANCE = 0.1;
    private static final int PIXEL_STEP = 4; // Step size for initial search
    private static final int COLOR_TOLERANCE = 30; // RGB component tolerance
    private static final double EARLY_STOP_THRESHOLD = 0.98; // Stop searching if we find a match this good

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImagePositionFinder <full_image_dir_or_file> <square_image_dir_or_file> [tolerance]");
            System.exit(1);
        }

        String fullImagePath = args[0];
        String squareImagePath = args[1];
        double tolerance = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_TOLERANCE;

        System.err.println("[ImagePositionFinder] Starting processing");
        System.err.println("[ImagePositionFinder] Full image path: " + fullImagePath);
        System.err.println("[ImagePositionFinder] Square image path: " + squareImagePath);
        System.err.println("[ImagePositionFinder] Tolerance: " + tolerance);

        try {
            List<ImageMatch> matches = findImagePositionsFlexible(fullImagePath, squareImagePath, tolerance);
            System.out.println(matches.stream()
                    .map(ImageMatch::toJson)
                    .collect(Collectors.joining(",\n", "[\n", "\n]")));
        } catch (IOException e) {
            System.err.println("[ImagePositionFinder] Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<File> getImageFilesFlexible(String path) throws IOException {
        File file = new File(path);
        List<File> files = new ArrayList<>();
        if (file.isDirectory()) {
            files = Files.list(file.toPath())
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && (f.getName().toLowerCase().endsWith(".jpg") || f.getName().toLowerCase().endsWith(".png")))
                    .collect(Collectors.toList());
        } else if (file.isFile()) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".jpg") || name.endsWith(".png")) {
                files.add(file);
            }
        }
        return files;
    }

    private static List<ImageMatch> findImagePositionsFlexible(String fullImagePath, String squareImagePath, double tolerance) throws IOException {
        System.err.println("[ImagePositionFinder] Scanning: " + fullImagePath);
        List<File> fullImages = getImageFilesFlexible(fullImagePath);
        System.err.println("[ImagePositionFinder] Found " + fullImages.size() + " full images");

        System.err.println("[ImagePositionFinder] Scanning: " + squareImagePath);
        List<File> squareImages = getImageFilesFlexible(squareImagePath);
        System.err.println("[ImagePositionFinder] Found " + squareImages.size() + " square images");

        List<ImageMatch> matches = new ArrayList<>();
        System.err.println("[ImagePositionFinder] Starting match search");

        for (File fullImage : fullImages) {
            String baseName = getBaseName(fullImage.getName());
            System.err.println("[ImagePositionFinder] Processing main image: " + fullImage.getName());

            File matchingSquareImage = squareImages.stream()
                    .filter(file -> getBaseName(file.getName()).equals(baseName))
                    .findFirst()
                    .orElse(null);

            if (matchingSquareImage != null) {
                System.err.println("[ImagePositionFinder] Found matching secondary image: " + matchingSquareImage.getName());
                BufferedImage full = ImageIO.read(fullImage);
                BufferedImage square = ImageIO.read(matchingSquareImage);

                System.err.println("[ImagePositionFinder] Searching in image " + full.getWidth() + "x" + full.getHeight() +
                        " for an image " + square.getWidth() + "x" + square.getHeight());

                ImageMatch match = findBestMatch(full, square, tolerance);
                if (match != null) {
                    match.setFullImage(fullImage.getName());
                    match.setSquareImage(matchingSquareImage.getName());
                    matches.add(match);
                    System.err.println("[ImagePositionFinder] Match found with " +
                            String.format("%.2f", match.getMatchPercentage() * 100) + "% confidence");
                } else {
                    System.err.println("[ImagePositionFinder] No match found for " + fullImage.getName());
                }
            } else {
                System.err.println("[ImagePositionFinder] No matching secondary image found for " + fullImage.getName());
            }
        }

        System.err.println("[ImagePositionFinder] Found " + matches.size() + " matches");
        return matches;
    }

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static ImageMatch findBestMatch(BufferedImage full, BufferedImage square, double tolerance) {
        int fullWidth = full.getWidth();
        int fullHeight = full.getHeight();
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();

        // First pass: Quick search with pixel stepping
        double bestMatchPercentage = 0;
        Point bestLocation = quickSearch(full, square);
        
        if (bestLocation != null) {
            // Second pass: Fine-tuning around the best match
            int searchRadius = PIXEL_STEP * 2;
            int startX = Math.max(0, bestLocation.x - searchRadius);
            int startY = Math.max(0, bestLocation.y - searchRadius);
            int endX = Math.min(fullWidth - squareWidth, bestLocation.x + searchRadius);
            int endY = Math.min(fullHeight - squareHeight, bestLocation.y + searchRadius);

            for (int y = startY; y <= endY; y++) {
                for (int x = startX; x <= endX; x++) {
                    double matchPercentage = calculateMatchPercentage(full, square, x, y);
                    if (matchPercentage > bestMatchPercentage) {
                        bestMatchPercentage = matchPercentage;
                        bestLocation.x = x;
                        bestLocation.y = y;
                        
                        if (bestMatchPercentage >= EARLY_STOP_THRESHOLD) {
                            return new ImageMatch(bestLocation.x, bestLocation.y, squareWidth, squareHeight, bestMatchPercentage);
                        }
                    }
                }
            }

            if (bestMatchPercentage >= 1.0 - tolerance) {
                return new ImageMatch(bestLocation.x, bestLocation.y, squareWidth, squareHeight, bestMatchPercentage);
            }
        }

        return null;
    }

    private static Point quickSearch(BufferedImage full, BufferedImage square) {
        int fullWidth = full.getWidth();
        int fullHeight = full.getHeight();
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();

        double bestMatchPercentage = 0;
        Point bestLocation = null;

        // Sample a subset of pixels for quick matching
        for (int y = 0; y <= fullHeight - squareHeight; y += PIXEL_STEP) {
            for (int x = 0; x <= fullWidth - squareWidth; x += PIXEL_STEP) {
                double matchPercentage = calculateQuickMatchPercentage(full, square, x, y);
                if (matchPercentage > bestMatchPercentage) {
                    bestMatchPercentage = matchPercentage;
                    bestLocation = new Point(x, y);
                    
                    if (bestMatchPercentage >= EARLY_STOP_THRESHOLD) {
                        return bestLocation;
                    }
                }
            }
        }

        return bestLocation;
    }

    private static double calculateQuickMatchPercentage(BufferedImage full, BufferedImage square, int startX, int startY) {
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();
        int sampleSize = (squareWidth * squareHeight) / (PIXEL_STEP * PIXEL_STEP);
        int matchingPixels = 0;
        int sampledPixels = 0;

        for (int y = 0; y < squareHeight; y += PIXEL_STEP) {
            for (int x = 0; x < squareWidth; x += PIXEL_STEP) {
                sampledPixels++;
                if (comparePixels(full.getRGB(startX + x, startY + y), square.getRGB(x, y))) {
                    matchingPixels++;
                }
            }
        }

        return (double) matchingPixels / sampledPixels;
    }

    private static double calculateMatchPercentage(BufferedImage full, BufferedImage square, int startX, int startY) {
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();
        int totalPixels = squareWidth * squareHeight;
        int matchingPixels = 0;

        for (int y = 0; y < squareHeight; y++) {
            for (int x = 0; x < squareWidth; x++) {
                if (comparePixels(full.getRGB(startX + x, startY + y), square.getRGB(x, y))) {
                    matchingPixels++;
                }
            }
        }

        return (double) matchingPixels / totalPixels;
    }

    private static boolean comparePixels(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;
        
        return Math.abs(r1 - r2) <= COLOR_TOLERANCE &&
               Math.abs(g1 - g2) <= COLOR_TOLERANCE &&
               Math.abs(b1 - b2) <= COLOR_TOLERANCE;
    }

    static class ImageMatch {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final double matchPercentage;
        private String fullImage;
        private String squareImage;

        public ImageMatch(int x, int y, int width, int height, double matchPercentage) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.matchPercentage = matchPercentage;
        }

        public void setFullImage(String fullImage) {
            this.fullImage = fullImage;
        }

        public void setSquareImage(String squareImage) {
            this.squareImage = squareImage;
        }

        public double getMatchPercentage() {
            return matchPercentage;
        }

        public String toJson() {
            return String.format(
                "{\n" +
                "    \"fullImage\": \"%s\",\n" +
                "    \"squareImage\": \"%s\",\n" +
                "    \"x\": %d,\n" +
                "    \"y\": %d,\n" +
                "    \"width\": %d,\n" +
                "    \"height\": %d,\n" +
                "    \"matchPercentage\": %.4f\n" +
                "}", fullImage, squareImage, x, y, width, height, matchPercentage);
        }
    }
} 