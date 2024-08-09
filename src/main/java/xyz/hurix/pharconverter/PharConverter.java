package xyz.hurix.pharconverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PharConverter {

    private final JFrame frame;
    private final JTextField pathField;
    private final JButton browseButton;
    private final JButton convertButton;
    private final File appDataDir;

    public PharConverter() {
        appDataDir = new File(System.getenv("APPDATA") + File.separator + ".hurixpharconverter");
        initializeAppDataDirectory();

        frame = new JFrame("Hurix - PharConverter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(490, 150);
        frame.setLayout(null);
        frame.setResizable(false);

        JLabel pathLabel = new JLabel("Chemin du dossier:");
        pathLabel.setBounds(10, 20, 120, 25);
        frame.add(pathLabel);

        pathField = new JTextField();
        pathField.setBounds(140, 20, 200, 25);
        frame.add(pathField);

        browseButton = new JButton("Parcourir");
        browseButton.setBounds(350, 20, 90, 25);
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedDir = fileChooser.getSelectedFile();
                    pathField.setText(selectedDir.getAbsolutePath());
                }
            }
        });
        frame.add(browseButton);

        convertButton = new JButton("Convertir");
        convertButton.setBounds(140, 60, 150, 25);
        convertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = pathField.getText();
                if (!path.isEmpty()) {
                    File folder = new File(path);
                    if (folder.exists() && folder.isDirectory()) {
                        try {
                            ensureDirectoryExists(folder.getParentFile());
                            copyFilesToAppData();
                            executePharConversion(folder);
                            JOptionPane.showMessageDialog(frame, "Conversion réussie !");
                        } catch (IOException ioException) {
                            JOptionPane.showMessageDialog(frame, "Erreur lors de la conversion : " + ioException.getMessage());
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "Chemin invalide !");
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un dossier.");
                }
            }
        });
        frame.add(convertButton);

        frame.setVisible(true);
    }

    private void initializeAppDataDirectory() {
        if (!appDataDir.exists()) {
            if (appDataDir.mkdirs()) {
                System.out.println("Répertoire créé : " + appDataDir.getAbsolutePath());
            } else {
                System.err.println("Impossible de créer le répertoire : " + appDataDir.getAbsolutePath());
            }
        }
    }

    private void copyFilesToAppData() throws IOException {
        copyResourceToFile("CreatePhar.php", new File(appDataDir, "CreatePhar.php"));
    }

    private void copyResourceToFile(String resourceName, File targetFile) throws IOException {
        try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (resourceStream == null) {
                throw new IOException("Le fichier de ressource " + resourceName + " n'a pas été trouvé dans le JAR.");
            }
            Files.copy(resourceStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void ensureDirectoryExists(File directory) {
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Répertoire créé : " + directory.getAbsolutePath());
            } else {
                System.err.println("Impossible de créer le répertoire : " + directory.getAbsolutePath());
            }
        }
    }

    private void openDirectory(File folder) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(folder);
                } else {
                    System.err.println("L'action d'ouverture de répertoire n'est pas supportée.");
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Erreur lors de l'ouverture du répertoire : " + e.getMessage());
            }
        } else {
            System.err.println("Le bureau n'est pas supporté.");
        }
    }

    private void executePharConversion(File folder) throws IOException {
        Process process = getProcess(folder);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("La commande a échoué avec le code de sortie " + exitCode);
            }
            openDirectory(folder);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Le processus a été interrompu", e);
        }
    }

    private Process getProcess(File folder) throws IOException {
        String createPharScriptPath = getCreatePharScriptPath();
        if (createPharScriptPath == null) {
            throw new IOException("Le script CreatePhar.php n'a pas été trouvé.");
        }

        String pharFileName = "plugin.phar";
        String pharFilePath = folder.getAbsolutePath() + File.separator + pharFileName;

        String[] command = {
                "php",
                "-d", "phar.readonly=0",
                createPharScriptPath,
                pharFilePath,
                folder.getAbsolutePath()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        return process;
    }

    private String getCreatePharScriptPath() {
        File scriptFile = new File(appDataDir, "CreatePhar.php");
        return scriptFile.getAbsolutePath();
    }

    public static void main(String[] args) {
        new PharConverter();
    }
}
