package com.widedot.tools;

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
    private double tolerance = 0.1; // Tolérance par défaut (10%)

    public ImagePositionFinder() {}

    public ImagePositionFinder(double tolerance) {
        this.tolerance = tolerance;
    }

    private double getColorDifference(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF;
        int g1 = (rgb1 >> 8) & 0xFF;
        int b1 = rgb1 & 0xFF;
        
        int r2 = (rgb2 >> 16) & 0xFF;
        int g2 = (rgb2 >> 8) & 0xFF;
        int b2 = rgb2 & 0xFF;
        
        // Calculer la différence pour chaque composante
        double diffR = Math.abs(r1 - r2) / 255.0;
        double diffG = Math.abs(g1 - g2) / 255.0;
        double diffB = Math.abs(b1 - b2) / 255.0;
        
        // Retourner la moyenne des différences
        return (diffR + diffG + diffB) / 3.0;
    }

    private Point findPositionInImage(BufferedImage fullImage, BufferedImage secondaryImage) {
        int fullWidth = fullImage.getWidth();
        int fullHeight = fullImage.getHeight();
        int secondaryWidth = secondaryImage.getWidth();
        int secondaryHeight = secondaryImage.getHeight();

        // Parcourir chaque pixel possible de l'image principale
        for (int y = 0; y <= fullHeight - secondaryHeight; y++) {
            for (int x = 0; x <= fullWidth - secondaryWidth; x++) {
                if (isMatch(fullImage, secondaryImage, x, y)) {
                    return new Point(x, y);
                }
            }
        }
        return null; // Aucune correspondance trouvée
    }

    private boolean isMatch(BufferedImage fullImage, BufferedImage secondaryImage, int startX, int startY) {
        int totalPixels = secondaryImage.getWidth() * secondaryImage.getHeight();
        int matchingPixels = 0;
        double requiredMatchPercentage = 0.95; // 95% des pixels doivent correspondre

        // Vérifier chaque pixel de l'image secondaire
        for (int y = 0; y < secondaryImage.getHeight(); y++) {
            for (int x = 0; x < secondaryImage.getWidth(); x++) {
                int fullRGB = fullImage.getRGB(startX + x, startY + y);
                int secondaryRGB = secondaryImage.getRGB(x, y);
                
                // Comparer les pixels avec tolérance
                if (getColorDifference(fullRGB, secondaryRGB) <= tolerance) {
                    matchingPixels++;
                }
            }
        }

        // Vérifier si le pourcentage de correspondance est suffisant
        return (double) matchingPixels / totalPixels >= requiredMatchPercentage;
    }

    private String getFileName(String path) {
        Path filePath = Paths.get(path);
        return filePath.getFileName().toString();
    }

    private String getFileNameWithoutExtension(String path) {
        String fileName = getFileName(path);
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(0, lastDotIndex) : fileName;
    }

    private List<String> getImageFiles(String directory) throws IOException {
        return Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> path.toLowerCase().endsWith(".jpg") || 
                              path.toLowerCase().endsWith(".jpeg") || 
                              path.toLowerCase().endsWith(".png"))
                .collect(Collectors.toList());
    }

    private String generateJsonResult(List<ImageMatch> matches) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"matches\": [\n");
        
        for (int i = 0; i < matches.size(); i++) {
            ImageMatch match = matches.get(i);
            json.append("    {\n");
            json.append("      \"fullImage\": \"").append(getFileName(match.fullImagePath)).append("\",\n");
            json.append("      \"secondaryImage\": \"").append(getFileName(match.secondaryImagePath)).append("\",\n");
            
            if (match.position != null) {
                json.append("      \"position\": {\n");
                json.append("        \"x\": ").append(match.position.x).append(",\n");
                json.append("        \"y\": ").append(match.position.y).append("\n");
                json.append("      },\n");
                json.append("      \"found\": true\n");
            } else {
                json.append("      \"found\": false\n");
            }
            
            json.append("    }").append(i < matches.size() - 1 ? "," : "").append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private static class ImageMatch {
        String fullImagePath;
        String secondaryImagePath;
        Point position;

        ImageMatch(String fullImagePath, String secondaryImagePath, Point position) {
            this.fullImagePath = fullImagePath;
            this.secondaryImagePath = secondaryImagePath;
            this.position = position;
        }
    }

    public void processDirectories(String fullImagesDir, String secondaryImagesDir) throws IOException {
        List<String> fullImages = getImageFiles(fullImagesDir);
        List<String> secondaryImages = getImageFiles(secondaryImagesDir);
        List<ImageMatch> matches = new ArrayList<>();

        for (String fullImagePath : fullImages) {
            String fullImageName = getFileNameWithoutExtension(fullImagePath);
            
            // Chercher l'image secondaire correspondante
            String matchingSecondaryImage = secondaryImages.stream()
                    .filter(path -> getFileNameWithoutExtension(path).equals(fullImageName))
                    .findFirst()
                    .orElse(null);

            if (matchingSecondaryImage != null) {
                try {
                    BufferedImage fullImage = ImageIO.read(new File(fullImagePath));
                    BufferedImage secondaryImage = ImageIO.read(new File(matchingSecondaryImage));
                    
                    Point position = findPositionInImage(fullImage, secondaryImage);
                    matches.add(new ImageMatch(fullImagePath, matchingSecondaryImage, position));
                } catch (IOException e) {
                    System.err.println("Erreur lors du traitement de " + fullImagePath + ": " + e.getMessage());
                }
            }
        }

        System.out.println(generateJsonResult(matches));
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Usage: java ImagePositionFinder <fullImagesDirectory> <secondaryImagesDirectory> [tolerance]");
            System.out.println("tolerance: valeur entre 0 et 1 (défaut: 0.1)");
            System.exit(1);
        }

        try {
            ImagePositionFinder finder;
            if (args.length == 3) {
                double tolerance = Double.parseDouble(args[2]);
                finder = new ImagePositionFinder(tolerance);
            } else {
                finder = new ImagePositionFinder();
            }

            finder.processDirectories(args[0], args[1]);
            
        } catch (IOException e) {
            System.err.println("{\"error\": \"Erreur lors du traitement des images\", \"message\": \"" + e.getMessage() + "\"}");
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("{\"error\": \"La tolérance doit être un nombre entre 0 et 1\"}");
            e.printStackTrace();
        }
    }
} 