package nostalgia.framework.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;

public class SDCardUtil {

    public static final String SD_CARD = "sdCard";
    public static final String EXTERNAL_SD_CARD = "externalSdCard";
    private static final String TAG = "utils.SDCardUtil";

    /**
     * @return True if the external storage is available. False otherwise.
     */
    public static boolean isAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getPath() + "/";
    }

    /**
     * @return True if the external storage is writable. False otherwise.
     */
    public static boolean isWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);

    }

    /**
     * @return A map of all storage locations available
     */
    public static HashSet<File> getAllStorageLocations(Context context) {
        HashSet<File> storageRoots = new HashSet<>();

        // 1. Primary External Storage (Internal Memory)
        File primaryStorage = Environment.getExternalStorageDirectory();
        if (primaryStorage != null && primaryStorage.exists()) {
            storageRoots.add(primaryStorage);
        }

        // 2. Secondary Storages via API (SD Cards, USB drives)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && context != null) {
            File[] externalDirs = context.getExternalFilesDirs(null);
            if (externalDirs != null) {
                for (File file : externalDirs) {
                    if (file != null) {
                        String path = file.getAbsolutePath();
                        // path is usually /storage/XXXX-XXXX/Android/data/package/files
                        // We want the root: /storage/XXXX-XXXX
                        int index = path.indexOf("/Android");
                        if (index != -1) {
                            String rootPath = path.substring(0, index);
                            storageRoots.add(new File(rootPath));
                        } else {
                            // Fallback if structure is different
                            storageRoots.add(file);
                        }
                    }
                }
            }
        }

        // 3. Fallback: Scan /proc/mounts for other mount points
        // Useful for some older devices or custom ROMs, but filtered to avoid system paths
        try {
            File mountFile = new File("/proc/mounts");
            if (mountFile.exists()) {
                Scanner scanner = new Scanner(mountFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    // Basic heuristic to find storage mounts
                    if (line.contains("/mnt") || line.contains("/storage") || line.contains("/sdcard")) {
                        String[] lineElements = line.split("\\s+");
                        if (lineElements.length >= 2) {
                            String mountPoint = lineElements[1];
                            // Exclude common system/virtual paths
                            if (!mountPoint.equals(Environment.getExternalStorageDirectory().getAbsolutePath())
                                    && !mountPoint.startsWith("/data")
                                    && !mountPoint.startsWith("/system")
                                    && !mountPoint.startsWith("/proc")
                                    && !mountPoint.startsWith("/sys")
                                    && !mountPoint.startsWith("/dev")
                                    && !mountPoint.contains("asec")
                                    && !mountPoint.contains("obb")
                                    && (line.contains("vfat") || line.contains("exfat") || line.contains("ntfs") || line.contains("fuse") || line.contains("sdcardfs"))) {
                                storageRoots.add(new File(mountPoint));
                            }
                        }
                    }
                }
                scanner.close();
            }
        } catch (Exception e) {
            NLog.e(TAG, "Error parsing /proc/mounts", e);
        }

        return storageRoots;
    }

    /**
     * http://svn.apache.org/viewvc/commons/proper/io/trunk/src/main/java/org/
     * apache/commons/io/FileUtils.java?view=markup
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("File must not be null");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
