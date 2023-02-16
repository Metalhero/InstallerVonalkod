package ct.core;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static void main(String[] args) throws InterruptedException, IOException {

        // VARIABLES
        String javaHomePath = System.getenv().get("JAVA_HOME");
        if (javaHomePath.endsWith("bin"))
            javaHomePath = javaHomePath.replace("bin", "");

        String javaVersion = System.getProperty("java.version");
        String dataModel = System.getProperty("sun.arch.data.model");

        String ctTempDirPath = "C:/cttemp/";
        String currentDirPath = Paths.get(".").toAbsolutePath().normalize().toString();

        String cnytFilePath = currentDirPath + "/ctnyt_registryadd.bat";
        String firstFile = "CTNYT.bat";
        String secondFile = "CTNYTProtocolHandler.jar";
        String firstCertiFile = "vknyomtcert.der";
        String secondCertiFile = "importvknyomtcert.bat";

        String[] filesToCopy = new String[]{currentDirPath + "/" + firstFile, currentDirPath + "/HANDLER/HTTP/" + secondFile};
        String certiFileToCopy = currentDirPath + "/CERTIFICATE/" + firstCertiFile;
        String firstFileTargetPath = ctTempDirPath + "/" + firstFile;
        String secondFileTargetPath = ctTempDirPath + "/" + secondFile;
        String firstCertiFileTargetPath = javaHomePath + "lib\\security\\" + firstCertiFile;

        // ACTIONS
        boolean folderCreationAction = false;
        boolean javaInstallAction = true;
        boolean runCnytFileAction = false;
        boolean certificationAction = false;

        // START
        Object[] choices = {"HTTP hálózati telepítés", "HTTPS hálózati telepítés", "Tanúsítvány frissítése"};
        int result = JOptionPane.showOptionDialog(null,
                "Kedves ügyfelünk! \n\nÖn a CompuTREND nyomtató telepítőjét futtatja. Kérem válassza ki a kívánt műveletet!", "Telepítő",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, choices, choices[0]);

        // SET ACTIONS
        if (result == 0) {
            folderCreationAction = true;
            runCnytFileAction = true;
        } else if (result == 1) {
            folderCreationAction = true;
            runCnytFileAction = true;
            certificationAction = true;
        } else if (result == 2) {
            certificationAction = true;
        }

        // IF CTTEMP NOT EXISTS, CREATE ONE. THAN COPY FILES TO THE CTTEMP FOLDER
        if (folderCreationAction) {
            createFolderIfNotExists(ctTempDirPath);

            // first file
            copyFileToDestination(filesToCopy[0], firstFileTargetPath);

            // second file
            copyFileToDestination(filesToCopy[1], secondFileTargetPath);
        }

        // !Egyenlore redundans de a jovoben hasznalhato lehet!
        // CHECK THE JAVA VERSION AND RUN INSTALL WHEN NOT FOUND
        if (javaInstallAction) {
            if (javaVersion == null) {
                if (dataModel.equals("64")) {
                    File root64 = new File(currentDirPath + "/JRE/jre-8u331-windows-x64.exe");
                    if (!root64.exists()) {
                        JOptionPane.showMessageDialog(null, "Java telepítés szükséges! Az installációs mappában nem találom a /JRE/jre-8u331-windows-x64.exe installációs fájlt.");
                        System.exit(0);
                    }
                    if (root64.isFile()) {
                        try {
                            Process p = Runtime.getRuntime()
                                    .exec("cmd /c start " + root64);
                            p.waitFor();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Hiba a java telepítés során: /JRE/jre-8u331-windows-x64.exe");
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
                } else if (dataModel.equals("32")) {
                    File root32 = new File(currentDirPath + "/JRE/jre-8u331-windows-x32.exe");
                    if (!root32.exists()) {
                        JOptionPane.showMessageDialog(null, "Java telepítés szükséges! Az installációs mappában nem találom a /JRE/jre-8u331-windows-x32.exe installációs fájlt.");
                        System.exit(0);
                    }
                    if (root32.isFile()) {
                        try {
                            Process p = Runtime.getRuntime()
                                    .exec("cmd /c start " + root32);
                            p.waitFor();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, "Hiba a java telepítés során: /JRE/jre-8u331-windows-x32.exe");
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
                }
            }
        }

        // RUN .BAT FILE
        if (runCnytFileAction) {
            try {
                Process p = Runtime.getRuntime()
                        .exec("cmd /c start " + cnytFilePath);
                p.waitFor();
            } catch (IOException | InterruptedException ex) {
                // error message
                JOptionPane.showMessageDialog(null, "Hiba történt a nyomtató telepítésekor!", "Hiba!", JOptionPane.PLAIN_MESSAGE);
                ex.printStackTrace();
            }
        }

        // CERTIFICATION UPDATE
        if (certificationAction) {

            // COPY FILE WHEN HAS THE PERMISSION
            boolean isCopyed = copyFileToDestination(certiFileToCopy, firstCertiFileTargetPath);

            String filePath = currentDirPath + "\\importvknyomtcert.bat";

            String line = "\"JAVAKONYVTAR\\bin\\keytool\" -import -alias VKNYOMTCERT -keystore \"JAVAKONYVTAR\\lib\\security\\cacerts\" -file \"JAVAKONYVTAR\\lib\\security\\vknyomtcert.der\"";

            // Write file with the correct modification
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
                line = line.replace("JAVAKONYVTAR", javaHomePath.substring(0, javaHomePath.length() - 1));

                // IF it is already registered, generate a new aliasname
                if (!isCopyed)
                    line = line.replace("VKNYOMTCERT", "vknyomtcert" + generateTimestamp());

                bw.write(line);
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("The line in the file is: " + line);

            // start
            String[] commands = {"cmd", "/c", "importvknyomtcert.bat"};
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();

            // Answer the first prompt with "changeit"
            String answer1 = "changeit\n";
            stdin.write(answer1.getBytes());
            stdin.flush();

            // Read the output of the command (optional)
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String cmdline;
            while ((cmdline = reader.readLine()) != null) {
                System.out.println(cmdline);
                if (cmdline.contains("Certificate already exists in keystore under alias")) {
                    JOptionPane.showConfirmDialog(null, "A tanúsítvány már telepítve van ezzel a verziószámmal.",
                            "Már telepítve", JOptionPane.DEFAULT_OPTION);
                    System.exit(0);
                }
            }

            // Answer the second prompt with "y"
            String answer2 = "y\n";
            stdin.write(answer2.getBytes());
            stdin.flush();

            // Wait for the command to finish
            process.waitFor();

        }

        JOptionPane.showConfirmDialog(null, "Telepítés sikeresen végetért!", "End", JOptionPane.DEFAULT_OPTION);
        System.exit(0);
    }


    private static boolean copyFileToDestination(String pathOfTheCopyedFile, String destination) {
        File filePath = new File(destination);
        try {
            if (!filePath.isFile()) {
                Files.copy(Paths.get(pathOfTheCopyedFile), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("The following file was copyed: " + pathOfTheCopyedFile);
            } else {
                System.out.println("The copy was not created: " + pathOfTheCopyedFile);
                return false;
            }
        } catch (AccessDeniedException ade) {
            ade.printStackTrace();
            JOptionPane.showConfirmDialog(null, "Nincs jogosultságom a fájlokat bemásolni a szükséges mappába: " + destination +
                    "\nKérem indítsa újra a futtatást rendszergazdaként!", "Másolás hiba", JOptionPane.DEFAULT_OPTION);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, "Nem tudtam a szükséges fájl(okat) bemásolni a szükséges mappába: " + destination,
                    "Másolás hiba", JOptionPane.DEFAULT_OPTION);
            System.exit(0);
        }
        return true;
    }

    private static void createFolderIfNotExists(String path) {
        File targetDir = new File(path);
        if (!targetDir.exists()) {
            targetDir.mkdir();
            System.out.println("Folder created: " + path);
        } else
            System.out.println("Folder already exists: " + path);
    }

    private static String browseFolderByUser() {
        File selectedFolder = null;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int resullt = fileChooser.showOpenDialog(null);
        if (resullt == JFileChooser.APPROVE_OPTION) {
            selectedFolder = fileChooser.getSelectedFile();
        }
        return selectedFolder.getAbsolutePath();
    }

    private static String generateTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return now.format(formatter);
    }

}