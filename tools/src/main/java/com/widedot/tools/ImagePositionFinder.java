package com.widedot.calendar.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class ImagePositionFinder {
    private static final double DEFAULT_TOLERANCE = 0.1;
    private static final int PIXEL_STEP = 4; // Step size for initial search
    private static final int COLOR_TOLERANCE = 30; // RGB component tolerance
    private static final double EARLY_STOP_THRESHOLD = 0.98; // Stop searching if we find a match this good

    public static void main(String[] args) {
        if (args.length < 2) {
            Gdx.app.error("ImagePositionFinder", "Usage: java ImagePositionFinder <full_image_dir_or_file> <square_image_dir_or_file> [tolerance]");
            Gdx.app.exit();
            return;
        }

        String fullImagePath = args[0];
        String squareImagePath = args[1];
        double tolerance = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_TOLERANCE;

        Gdx.app.log("ImagePositionFinder", "Starting processing");
        Gdx.app.log("ImagePositionFinder", "Full image path: " + fullImagePath);
        Gdx.app.log("ImagePositionFinder", "Square image path: " + squareImagePath);
        Gdx.app.log("ImagePositionFinder", "Tolerance: " + tolerance);

        try {
            Array<ImageMatch> matches = findImagePositionsFlexible(fullImagePath, squareImagePath, tolerance);
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            Gdx.app.log("ImagePositionFinder", json.toJson(matches));
        } catch (Exception e) {
            Gdx.app.error("ImagePositionFinder", "Error: " + e.getMessage(), e);
            Gdx.app.exit();
            return;
        }
    }

    private static Array<FileHandle> getImageFilesFlexible(String path) {
        FileHandle file = Gdx.files.absolute(path);
        Array<FileHandle> files = new Array<>();
        
        if (file.isDirectory()) {
            for (FileHandle child : file.list()) {
                if (!child.isDirectory() && (child.extension().equalsIgnoreCase("jpg") || child.extension().equalsIgnoreCase("png"))) {
                    files.add(child);
                }
            }
        } else if (!file.isDirectory()) {
            String extension = file.extension().toLowerCase();
            if (extension.equals("jpg") || extension.equals("png")) {
                files.add(file);
            }
        }
        return files;
    }

    private static Array<ImageMatch> findImagePositionsFlexible(String fullImagePath, String squareImagePath, double tolerance) {
        Gdx.app.log("ImagePositionFinder", "Scanning: " + fullImagePath);
        Array<FileHandle> fullImages = getImageFilesFlexible(fullImagePath);
        Gdx.app.log("ImagePositionFinder", "Found " + fullImages.size + " full images");

        Gdx.app.log("ImagePositionFinder", "Scanning: " + squareImagePath);
        Array<FileHandle> squareImages = getImageFilesFlexible(squareImagePath);
        Gdx.app.log("ImagePositionFinder", "Found " + squareImages.size + " square images");

        Array<ImageMatch> matches = new Array<>();
        Gdx.app.log("ImagePositionFinder", "Starting match search");

        for (FileHandle fullImage : fullImages) {
            String baseName = getBaseName(fullImage.name());
            Gdx.app.log("ImagePositionFinder", "Processing main image: " + fullImage.name());

            FileHandle matchingSquareImage = null;
            for (FileHandle squareImage : squareImages) {
                if (getBaseName(squareImage.name()).equals(baseName)) {
                    matchingSquareImage = squareImage;
                    break;
                }
            }

            if (matchingSquareImage != null) {
                Gdx.app.log("ImagePositionFinder", "Found matching secondary image: " + matchingSquareImage.name());
                Pixmap full = new Pixmap(fullImage);
                Pixmap square = new Pixmap(matchingSquareImage);

                Gdx.app.log("ImagePositionFinder", "Searching in image " + full.getWidth() + "x" + full.getHeight() +
                        " for an image " + square.getWidth() + "x" + square.getHeight());

                ImageMatch match = findBestMatch(full, square, tolerance);
                if (match != null) {
                    match.setFullImage(fullImage.name());
                    match.setSquareImage(matchingSquareImage.name());
                    matches.add(match);
                    Gdx.app.log("ImagePositionFinder", "Match found with " +
                            String.format("%.2f", match.getMatchPercentage() * 100) + "% confidence");
                } else {
                    Gdx.app.log("ImagePositionFinder", "No match found for " + fullImage.name());
                }
                
                full.dispose();
                square.dispose();
            } else {
                Gdx.app.log("ImagePositionFinder", "No matching secondary image found for " + fullImage.name());
            }
        }

        Gdx.app.log("ImagePositionFinder", "Found " + matches.size + " matches");
        return matches;
    }

    private static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static ImageMatch findBestMatch(Pixmap full, Pixmap square, double tolerance) {
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

    private static Point quickSearch(Pixmap full, Pixmap square) {
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

    private static double calculateQuickMatchPercentage(Pixmap full, Pixmap square, int startX, int startY) {
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();
        int sampleSize = (squareWidth * squareHeight) / (PIXEL_STEP * PIXEL_STEP);
        int matchingPixels = 0;
        int sampledPixels = 0;

        for (int y = 0; y < squareHeight; y += PIXEL_STEP) {
            for (int x = 0; x < squareWidth; x += PIXEL_STEP) {
                sampledPixels++;
                if (comparePixels(full.getPixel(startX + x, startY + y), square.getPixel(x, y))) {
                    matchingPixels++;
                }
            }
        }

        return (double) matchingPixels / sampledPixels;
    }

    private static double calculateMatchPercentage(Pixmap full, Pixmap square, int startX, int startY) {
        int squareWidth = square.getWidth();
        int squareHeight = square.getHeight();
        int matchingPixels = 0;
        int totalPixels = squareWidth * squareHeight;

        for (int y = 0; y < squareHeight; y++) {
            for (int x = 0; x < squareWidth; x++) {
                if (comparePixels(full.getPixel(startX + x, startY + y), square.getPixel(x, y))) {
                    matchingPixels++;
                }
            }
        }

        return (double) matchingPixels / totalPixels;
    }

    private static boolean comparePixels(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 24) & 0xff;
        int g1 = (rgb1 >> 16) & 0xff;
        int b1 = (rgb1 >> 8) & 0xff;
        int a1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 24) & 0xff;
        int g2 = (rgb2 >> 16) & 0xff;
        int b2 = (rgb2 >> 8) & 0xff;
        int a2 = rgb2 & 0xff;

        return Math.abs(r1 - r2) <= COLOR_TOLERANCE &&
               Math.abs(g1 - g2) <= COLOR_TOLERANCE &&
               Math.abs(b1 - b2) <= COLOR_TOLERANCE &&
               Math.abs(a1 - a2) <= COLOR_TOLERANCE;
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
    }

    static class Point {
        int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
} 