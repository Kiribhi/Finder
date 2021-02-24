package nahrim.finder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

/**
 *
 * This class finds all zipped file from a specified root directory
 * and reproduce them to a specified storage directory.
 *
 * This class includes a lot of constants follow as:
 * path of information files,
 * data for the dialogs(e.g. title, option type code, message type),
 * regular expression for checking path data inputed from users,
 * extension list to find(i.e. extensions of zipped files),
 * status code for logging and showing the status to users,
 * etc.
 *
 * @author Shin jong wook
 *
 * @since 07/09/2020
 *
 * @version 1.12.1, 27/01/2021
 * 
 */
public final class Finder {
    /**
     *
     * Every file and directory path are relative to this program
     * should be declared with the absolute path. Because The files
     * should be created on constant path, it is executed wherever.
     */
    private static final String DATA_PATH = "C:\\Program Files\\Nahrim\\Finder\\data\\";
    private static final File DATA_DIR = new File(DATA_PATH);
    private static final File ROOT_INFO_FILE = new File(DATA_PATH + "root.txt");
    private static final File STORAGE_INFO_FILE = new File(DATA_PATH + "storage.txt");
    private static final int EOF = -1; //End Of File
    private static final String ENCODING = "UTF-8"; //Default encoding method is UTF-8
    
    
    /**
     *
     * A new line character is OS-specific.
     * Windows: \r\n
     * Mackintosh: \r
     * Linux: \n
     * <code>getProperty(String key)</code> searches for the property
     * with the specified key in this property list.
     * The key <code>"line.seperator"</code> searches new line character
     * of this OS.
     */
    private static final String NEW_LINE = System.getProperties()
                                                 .getProperty("line.separator");
    
    
    /**
     * 
     * Whether initialization is completed or not.
     * The value of <code>initialized</code> is modified with true after initialization.
     * This variable is used on checking whether is initialized or not,
     * to prevent that <code>NullPointerException<code> occurs due to nonexistence of a
     * root directory, etc. 
     */
    private static boolean initialized = false;
    
    
    /**
     *
     * The path of initial value of input dialog.
     * Otherwise an user modifies this, <code>DEFAULT_ROOT_PATH</code> is
     * saved on root.txt, <code>DEFAULT_STORAGE_PATH</code> is saved on
     * "C:\Program Files\Nahrim\Finder\data\storage.txt".
     * The purpose of this program is Finding all zipped file on portable storage medium.
     * So the default root pathes are E:, F: and so on-drive name of portable storage medium. 
     */
    public static final String DEFAULT_ROOT_PATH = "E:\\";
    private static final String DEFAULT_STORAGE_PATH = "D:\\Finder\\Storage\\";

    
    /**
     *
     * The Regular expression for absolute path.
     *
     * The path that an user input, may be invalid so it sshould be verified.
     * It can be process by the test, maching with a regular expression.
     *
     * Every root directory (that is, the drive) name is uppercase alphabet and
     * colon next, e.g. C:
     * The path seperator of Windows is fixed with backslash(\),
     * but slash(/) is also can be used.
     * and *, :, /, ", <, >, \, | cannot be used on file name,
     * <code>[^^:\\*\"/<>\\|\\\\]+</code> means that,
     * i.e. a character except for *, :, /, ", <, >, \, |
     *
     * Ex)
     * D:                          : usable
     * A:\                         : usable
     * D:\abc\def                  : usable
     * D:\|chungsungchungsung^^7|\ : unusable      (|, ^ cannot be used)
     * C:/asd//asdaf               : unusable      (A path seperator cannot be duplicate)
     * D:\\AA\bbb                  : unusable      (A path seperator cannot be duplicate)
     * D:asdg                      : unusable      (Colon is not usable on directory name
     *                                              and path must be start with drive name.)
     */
    public static final String PATH_REGEXP = "[A-Z]:(((\\\\|/)[^\\^:\\*\"</>\\|\\\\]+)*(\\\\|/)?)?" +
                                             "((\r\n|\r|\n)+[A-Z]:(((\\\\|/)[^\\^:\\*\"</>\\|\\\\]+)*(\\\\|/)?)?)*" +
                                             "(\r\n|\r|\n)*";
    
    /**
     *
     * The status codes for logging.
     * On log file, the log message corresponding to status code will be logged.
     * In <code>STATUS_MESSAGES</code>, index is mapped with status code,
     * so <code>STATUS_MESSAGES[statusCode]</code> presents the log message
     * corresponding to given status code.
     */
    private static final int NORMAL = 0;
    private static final int NO_ROOT_DIR = 1;
    private static final int NO_ROOT_INFO = 2;
    private static final int NOT_INITED = 3;
    private static final int DUPLICATE_INIT = 4;
    private static final int IO_FAIL = 5;
    private static final int INVALID_PATH = 6;
    private static final int MKSTRG_FAIL = 7;
    private static final int NO_FILE_IN_ROOT = 8;
    private static final int NONEXISTENCE = 9;
    private static final int GET_PATH_FAIL = 10;
    private static final int CLRSTRG_FAIL = 11;
    private static final int PATH_COLLISION = 12;
    private static final Map<Integer, String> STATUS_MESSAGES = new HashMap<Integer, String>(0);

    
    /**
     *
     * This program uses the dialogues in next three type. So the
     * titles, message types, option types should be defined.
     * Titles are not declared on <code>javax.swing.JOptionPane</code>
     * so should be defined on this class.
     * But message types and options types are declared in
     * <code>javax.swing.JOptionPane</code> as constant.
     * Please see {@link javax.swing.JOptionPane} to get information of declaration.
     * For convenience, they has declared with same name,
     * equal value.
     *
     * @see javax.swing.JOptionPane
     */
    private static final String INFORMATION_TITLE = "Information";
    private static final String WARNING_TITLE = "Warning";
    private static final String ERROR_TITLE = "Error";
    private static final String INPUT_TITLE = "Input";
    private static final String NO_TITLE = "";

    /**
     *
     * The integer values corresponding to message types.
     * Please see {@link javax.swing.JOptionPane} to get information of exact value.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    private static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    private static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    private static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

    /**
     *
     * The integer values corresponding to types of option
     * to be chosen of confirm dialog.
     * Please see {@link javax.swing.JOptionPane} to get information of exact value.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;

    /**
     *
     * The integer values corresponding to option that has been chosen.
     * Please see {@link javax.swing.JOptionPane} to get information of exact value.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int YES_OPTION = JOptionPane.YES_OPTION;
    private static final int NO_OPTION = JOptionPane.NO_OPTION;
    private static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    private static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

    /**
     *
     * A zipped file extensions list, except for split compression file's.
     * A split zipped file extension is distinct by regular expressions,
     * follow as:
     *  <code>"[AaZz]{1}0[0-9]{1}"</code>   Ex) Z01, A02
     *  <code>"[A-Za-z0-9]+_Z"</code>       Ex) tgz_Z,
     *  <code>"00[1-9]{1}"</code>           Ex) 001, 002
     */
    private static final Set<String> EXTENSION_SET = new HashSet<String>(0);
    /**
     *
     * After searching the searched files are loaded on <code>TASK_LIST</code>.
     * And then, clone process will be executed by the sequence that added on the list.
     * 
     * If reproduction failed that file will be added on <code>FAILED_TASK_LIST</code>
     * and try again those. 
     */
    private static final List<File> TASK_LIST = new ArrayList<File>(0);
    private static final List<File> FAILED_TASK_LIST = new ArrayList<File>(0);

    /**
     *
     * Logging is started when the program is executed and
     * is terminated when the program is terminated.
     * So the logging stream must be opened during running.
     */
    private static final Logger LOGGER = Logger.logger();


    /**
     * This method roles of a kind of main method.
     *
     * @return status code.
     *         zero if the program is normally terminatated, not zero otherwise.
     */
    public static int start() {
        log("Program starts.");
        int statusCode = ((init()) ? (run()) : (NOT_INITED));

        String message = STATUS_MESSAGES.get(statusCode);
        if(message == null) { message = "Unknown status."; }

        logPrint(message);
        terminate(message);

        return statusCode;
    }

    /**
     *
     * Stuff that MUST be initialized: storage directory, data directory,
     *                                 rootpath.txt, log message set
     *                                 
     * Set a environment that Finder runs. 
     * If a directory that log and various info files are saved is nonexistent, make that.
     * And save the information of <code>storagePath</code> and <code>rootPath</code> in there. 
     * 
     *
     * @return {@value #NORMAL} if initialization is succeeded,
     *         {@value #NOT_INITED} otherwise.
     */
    private static final boolean init() {
        if(initialized) { return true; }

        try {
            String message = "Initializing . . .";
            logPrint(message);

            if(STATUS_MESSAGES.size() < 1) {
                STATUS_MESSAGES.put(NORMAL, "Normally terminated.");
                STATUS_MESSAGES.put(NO_ROOT_DIR, "A root directory to search does not exist.");
                STATUS_MESSAGES.put(NOT_INITED, "Initialization failure.");
                STATUS_MESSAGES.put(DUPLICATE_INIT, "Initialized already.");
                STATUS_MESSAGES.put(IO_FAIL, "I/O exception occured.");
                STATUS_MESSAGES.put(INVALID_PATH, "The given path is invalid.");
                STATUS_MESSAGES.put(MKSTRG_FAIL, "Storage is not created.");
                STATUS_MESSAGES.put(NO_FILE_IN_ROOT, "The root directory does not contains any files.");
                STATUS_MESSAGES.put(NONEXISTENCE, "Nonexistent file or directory.");
                STATUS_MESSAGES.put(GET_PATH_FAIL, "Getting canonical path failed.");
                STATUS_MESSAGES.put(CLRSTRG_FAIL, "An error occured in the middle of cleaning.");
                STATUS_MESSAGES.put(PATH_COLLISION, "Path is duplicate.");
            }

            //Initialize set of extension that will be filtered
            if(EXTENSION_SET.size() < 1) {
                EXTENSION_SET.add("﻿ace");
                EXTENSION_SET.add("alz");
                EXTENSION_SET.add("arc");
                EXTENSION_SET.add("arj");
                EXTENSION_SET.add("b64");
                EXTENSION_SET.add("bh");
                EXTENSION_SET.add("bhx");
                EXTENSION_SET.add("bz2");
                EXTENSION_SET.add("cab");
                EXTENSION_SET.add("dcg");
                EXTENSION_SET.add("ddi");
                EXTENSION_SET.add("dds");
                EXTENSION_SET.add("dwc");
                EXTENSION_SET.add("ear");
                EXTENSION_SET.add("enc");
                EXTENSION_SET.add("egg");
                EXTENSION_SET.add("gz");
                EXTENSION_SET.add("ha");
                EXTENSION_SET.add("hqx");
                EXTENSION_SET.add("ice");
                EXTENSION_SET.add("ima");
                EXTENSION_SET.add("img");
                EXTENSION_SET.add("j");
                EXTENSION_SET.add("lha");
                EXTENSION_SET.add("lzh");
                EXTENSION_SET.add("mim");
                EXTENSION_SET.add("owp");
                EXTENSION_SET.add("pak");
                EXTENSION_SET.add("pit");
                EXTENSION_SET.add("rar");
                EXTENSION_SET.add("sit");
                EXTENSION_SET.add("tar");
                EXTENSION_SET.add("taz");
                EXTENSION_SET.add("tgz");
                EXTENSION_SET.add("tz");
                EXTENSION_SET.add("uu");
                EXTENSION_SET.add("uue");
                EXTENSION_SET.add("war");
                EXTENSION_SET.add("xxe");
                EXTENSION_SET.add("z");
                EXTENSION_SET.add("zip");
                EXTENSION_SET.add("zoo");
                EXTENSION_SET.add("7z");
            }

            //Check whether data directory exists or not
            if(DATA_DIR.exists()) {
                message = "The data directory exists.";
                logPrint(message);
            } else {
                message = "The data directory is nonexistent.";
                logPrintInfoDialog(message);

                if(makeDataDir()) {
                    message = "Data directory was made.";
                    logPrintInfoDialog(message);
                } else {
                    message = "Data directory creation fail.";
                    logPrintErrorDialog(message);

                    return false;
                }
            }

            //Initialize storage.txt
            if(STORAGE_INFO_FILE.exists()) {
                message = "storage.txt exists.";
                logPrint(message);

                if(getStoragePath() == null) {
                    message = "The storage path is invalid.\n" +
                              "Check " + STORAGE_INFO_FILE.getCanonicalPath();
                    return false;
                } else {
                	String storagePath = getStoragePath();
                	if((storagePath == null)
                	|| !storagePath.matches(PATH_REGEXP)) {
                		log("Invalid storage path");
                		errorDialog("Invalid path.\nPlease check " + STORAGE_INFO_FILE.getCanonicalPath());
                		return false;
                	}

                    File storageDir = new File(storagePath);
                    if(!storageDir.exists()) {
                        if(!storageDir.mkdirs()) {
                            return false;
                        }
                    }
                }
            } else {
                if(makeStorage()) {
                    message = STORAGE_INFO_FILE.getCanonicalPath() + " was made.";
                    logPrintInfoDialog(message);
                } else {
                    message = "storage.txt creation fail";
                    logPrintErrorDialog(message);

                    return false;
                }
            }

            //Initialize root.txt
            if(ROOT_INFO_FILE.exists()) {
                message = "root.txt exists.";

                List<String> rpl = getRootPathList();
                if(rpl.isEmpty()) {
                    message = "No root path to read";
                    logPrintInfoDialog(message);

                    return false;
                } else {
                    for(int i = 0; i < rpl.size(); i++) {
                        if(!rpl.get(i).matches(PATH_REGEXP)) {
                            rpl.remove(i--);
                        }
                    }

                    if(rpl.isEmpty()) {
                        message = "No root path to read";
                        logPrintInfoDialog(message);
                        return false;
                    } else {
                        message = "The invalid pathes are removed" +
                                  "\nfrom the root path list.";
                        logPrint(message);
                    }
                }
            } else {
                if(createRootDirInfoFile()) {
                    message = ROOT_INFO_FILE.getCanonicalPath() + " was made.";
                    logPrintInfoDialog(message);
                } else {
                    message = "root.txt creation fail";
                    logPrintErrorDialog(message);

                    return false;
                }
            }

            message = "Initialization ends.";
            logPrint(message);
            initialized = true;

            return true;
        } catch (Exception e) {
            e.printStackTrace();

            String message = "Initialization ends with error: " + e.getMessage();
            logPrintErrorDialog(message);

            return false;
        }
    }

    /**
     *
     * Operates the substantial functions,
     * e.g. initialization, searching, reproduction, etc.
     *
     * @return Various status code
     */
    private static int run() {
    	//If initialzation is failed program cannot be executed.
        if(!initialized) {
            if(!init()) {
                return NOT_INITED;
            }
        }
        
        //Get rootpath
        List<String> rootPathList;
        try {
            rootPathList = getRootPathList();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return NO_ROOT_INFO;
        } catch (IOException e) {
            e.printStackTrace();
            return IO_FAIL;
        }

        //Execution dialogue
        if((rootPathList == null)
        || (rootPathList.size() < 1)) {
            return NO_ROOT_INFO;
        }

        /*////Execution////*/
        String message = "Execute the program?";
        int option = confirmDialog(message);
        switch(option) {
            case YES_OPTION:
                try {
                	//Get a path of storage directory
                    String storagePath = getStoragePath();
                    if((storagePath == null)
                    || (!storagePath.matches(PATH_REGEXP))) {
                        return INVALID_PATH;
                    }
                    
                    //Get File object of storage directory
                    File storageDir = new File(storagePath);
                    if(storageDir == null) {
                        return MKSTRG_FAIL;
                    }

                    long startTime = System.currentTimeMillis();
                    for(int i = 0; i < rootPathList.size(); i++) {
                        String rootPath = rootPathList.get(i);
                        System.out.print("Search from \"" + rootPath + "\". . .");
                        boolean result = search(rootPath);
                        System.out.println((result ? (rootPath + "Finished.")
                                                   : ("Nonexistent or empty.")));
                    }
                    long estimatedTime = System.currentTimeMillis() - startTime;
                    message = "Searching time: " +
                              (((double)estimatedTime) / 1000.000D) + " seconds.\n";
                    infoDialog(message);

                    int size = TASK_LIST.size();
                    if(size > 0) {
                        message = size + ((size == 1) ? " zipped file is found."
                                                      : " zipped files are found.");
                        if(!isEmptyDir(storageDir)) {
                            infoDialog(message);
                            File[] filesOfStorage = storageDir.listFiles();
                            int len = filesOfStorage.length;
                            message = "Storage is not empty." +
                                      "\nThere " + ((len > 1) ? ("are " + len + " files")
                                                              : ("is " + len +  " file")) +
                                      "\nincluding " + storageDir.listFiles()[0].getName();
                            infoDialog(message);

                            message = "Clear the storage?";
                            option = confirmDialog(message);
                            if(option == JOptionPane.YES_OPTION) {
                                message = "Clear the storage . . .";
                                logPrint(message);

                                for(File file : filesOfStorage) {
                                    if(!clear(file)) {
                                        return CLRSTRG_FAIL;
                                    }
                                }

                                message = "The storage has been cleared.";
                                logPrint(message);
                            }
                        }

                        message = message.concat("\nPress OK to start reproduction.");
                        infoDialog(message);

                        System.out.println("Path to will be reproduced: " + storagePath);
                        System.out.println("Reproduction will be executed.");
                        for(int i = 0; i < size; ) {
                            File file = TASK_LIST.get(i++);
                            System.out.printf("(%d/%d) Reproduce \"%s\". . . ",
                                              i, size, file.getAbsolutePath());
                            boolean isReproduced = reproduce(new File(storagePath + file.getName()), file);
                            System.out.println(((isReproduced) ? ("has") : ("hasn\'t")) +  "been reproduced.");
                            if(!isReproduced) { FAILED_TASK_LIST.add(file); }
                        }

                        size = FAILED_TASK_LIST.size();
                        message = "Reproduction is finished with " + size + " failures.";
                        if(size > 0) {
                            message = "Try again the failed file?";
                            switch(confirmDialog(message)) {
                                case YES_OPTION:
                                    int countFails = 0;
                                    message = "Start reproduction for failed files.";
                                    for(int i = 0; i < size; ) {
                                        File file = FAILED_TASK_LIST.get(i++);
                                        System.out.printf("(%d/%d) Reproduce \"%s\". . . ", i, size, file.getAbsolutePath());
                                        boolean isReproduced = reproduce(new File(storagePath + file.getName()), file);
                                        if(isReproduced) {
                                        	System.out.println("has been reproduced.");
                                        } else {
                                        	++countFails;
                                        	System.out.println("hasn\'t been reproduced.");
                                        }
                                    }

                                    if(countFails > 0) {
                                        message = "Total: " + size + "\n" +
                                        	      "Reproduced: " + (size - countFails) + "\n" +
                                                  "Failed: " + countFails;
                                        System.out.println(message);
                                        infoDialog(message +
                                                   "\nTry again later for failed files please.");
                                    } else {
                                        message = "Reproduction is successfully finished.";
                                        logPrint(message);
                                    }
                                    break;
                                default:
                                    message = "Skip the failed files.\n" +
                                              "Don\'t forget that a hacking file may exist in skipped files.";
                                    warningDialog(message);
                                    break;
                            }
                        }

                        logPrintInfoDialog(message);
                        open(storagePath);
                    } else {
                        message = "No zipped file.";
                        logPrintInfoDialog(message);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return NO_ROOT_DIR;
                } catch (IOException e) {
                    e.printStackTrace();
                    return IO_FAIL;
                }

                break;
            case NO_OPTION:
            case CANCEL_OPTION:
            case CLOSED_OPTION:
                log("Program execution is canceled.");
                break;
            default:
                errorDialog("Invalid option");
        }

        return NORMAL;
    }


    private static boolean makeDataDir() {
        return DATA_DIR.mkdirs();
    }

    /**
     *
     * Creates root.txt and write inputted path on root.txt file.
     * root.txt is the file that the information of path to search is saved.
     * 
     * @return true if root.txt is created, false otherwise.
     */
    private static boolean createRootDirInfoFile() {
        String message = "root.txt does not exist.";
        warningDialog(message);

        message = "Create the file?";
        switch(confirmDialog(message)) {
            case YES_OPTION:
                message = "Input root directory path: ";
                String path = inputDialog(message, DEFAULT_ROOT_PATH);

                if(path.matches(PATH_REGEXP)) {
                    if(path.charAt(path.length() - 1) != File.separatorChar) {
                        path = path.concat(File.separator);
                    }
                } else {
                    message = "Invalid path.\n" +
                              "Default path is \"" + DEFAULT_ROOT_PATH + "\"";
                    path = DEFAULT_ROOT_PATH;
                    warningDialog(message);
                }

                try {
                    if(!ROOT_INFO_FILE.createNewFile()) {
                        log("root.txt creation failure.");
                        return false;
                    } else {
                        log(ROOT_INFO_FILE.getCanonicalPath() + " has been made.");
                    }

                    FileWriter fw = new FileWriter(ROOT_INFO_FILE);
                    fw.write(path);
                    fw.flush();
                    fw.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    message = "root.txt creation failure.";
                    logPrintErrorDialog(message);
                    return false;
                }
            case NO_OPTION:
                message = "root.txt is not created.";
                logPrintInfoDialog(message);
            case CLOSED_OPTION:
                return false;
            default:
                message = "Unknown option in root.txt creation";
                logPrintErrorDialog(message);
                return false;
        }
    }


    private static boolean makeStorage() {
        String message = "The storage directory does not exist.";
        warningDialog(message);

        message = "Create the file?";
        switch(confirmDialog(message)) {
            case YES_OPTION:
                try {
                    String path = null;
                    File[] roots = File.listRoots();
                    FileSystemView fsv = FileSystemView.getFileSystemView();
                    File file = roots[((roots.length >= 2) ? (1) : (0))];

                    if(!fsv.getSystemTypeDescription(file).matches("[\\s\\r\\n\\t]*")
                    && !fsv.getSystemTypeDescription(file).contains("CD")
                    && !fsv.getSystemTypeDescription(file).contains("DVD")) {
                        String driveName = fsv.getSystemDisplayName(file);
                        driveName = driveName.substring(
                                        driveName.indexOf('(') + 1,
                                        driveName.lastIndexOf(')')
                                    );
                        path = driveName + File.separator;
                    }

                    message = "Input the directory to store zipped files: ";
                    JFileChooser jfc = new JFileChooser(path);
                    jfc.setDialogTitle("Choose the location to save data files.");
                    jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    if(jfc.showDialog(null, "Choose") == JFileChooser.APPROVE_OPTION) { 
                        path = jfc.getSelectedFile().getAbsolutePath();
                    } else {
                        path = null;
                    }

                    if((path == null)
                    || !path.matches(PATH_REGEXP)) {
                        message = "Invalid path.\n" +
                        "Default path is " + DEFAULT_STORAGE_PATH + ".";
                        path = DEFAULT_STORAGE_PATH;
                        warningDialog(message);
                    } else {
                        if(path.charAt(path.length() - 1) != File.separatorChar) {
                            path = path.concat(File.separator);
                        }
                    }

                    //path: storage path
                    file = new File(path);
                    if(!file.exists() || file.isFile()) {
                        if(!file.mkdirs()) {
                            return false;
                        }
                    }

                    if(!STORAGE_INFO_FILE.createNewFile()) {
                        message = STATUS_MESSAGES.get(MKSTRG_FAIL);
                        logPrintErrorDialog(message);
                        return false;
                    } else {
                        message = STORAGE_INFO_FILE.getCanonicalPath() + " has been made.";
                        logPrint(message);
                    }

                    FileWriter fw = new FileWriter(STORAGE_INFO_FILE);
                    fw.write(path);
                    fw.flush();
                    fw.close();

                } catch (Exception e) {
                    e.printStackTrace();
                    message = STATUS_MESSAGES.get(MKSTRG_FAIL);
                    logPrintErrorDialog(message);
                    return false;
                }

                return true;

            case NO_OPTION:
            case CLOSED_OPTION:
                message = "storage.txt is not created.";
                logPrintInfoDialog(message);
                return false;

            default:
                message = "Storage creation failed.";
                logPrintErrorDialog(message);
                return false;
        }
    }

    /**
     *
     * Finds the zipped files from the given path, <code>rootPath</code>.
     * If the path is not the directory path or does not exists,
     * the method returns false.
     *
     * The path should be given as the absolute path.
     * or unexpectable result is outputed.
     *
     * If no subdirectory of file at the gived root directory path
     * returns corresponding status code, or check it is whether
     * file or directory.
     * If it is directory, call this method recursively,
     * or add it to <code>PACKING_FILE_LIST</code> after checking
     * whether it is a zipped file.
     *
     * @param  rootPath the directory path to start searching
     *
     * @return various status code
     */
    private static boolean search(String rootPath) {
        File rootDir = new File(rootPath);

        if(!rootDir.exists()
        || !rootDir.isDirectory()) {
            return false;
        }

        File[] files = rootDir.listFiles();
        if((files == null)
        || (files.length <= 0)) {
            return false;
        }

        for(File file : files) {
            System.out.print("\nSearch: \"" + file.getAbsolutePath() + "\"");
            if(file.isDirectory()) {
                search(file.getAbsolutePath());
            } else if(file.isFile()) {
                if(isZipped(file)) {
                    TASK_LIST.add(file);
                }
            } else {
                String message = STATUS_MESSAGES.get(NONEXISTENCE) +
                                 ": " + file.getAbsolutePath();
                logPrintErrorDialog(message);

                return false;
            }
        }

        return true;
    }

    
    /**
     *
     * Copies all the byte datas from <code>src</code> to <code>dest</code>.
     * Before executing the process, check if there is <code>dest</code> or not.
     * If <code>dest</code> is already exists, duplicate files are numbered.
     *
     * @param src  file to be copied
     * @param dest file to be pasted
     *
     * @return true if reproducing is normally completed, false otherwise.
     */
    private static final boolean reproduce(File dest, File src) {
        try {
            /**
             * If this process (i.e. numbering process) is not executed
             * file will be overwrited.
             * */
            if(dest.exists()) {
                String idx = getNextIndexOf(dest.getName(), dest.getParentFile());
                String newDestPath = dest.getCanonicalPath();
                newDestPath = newDestPath.substring(0, newDestPath.lastIndexOf('.'));
                newDestPath = newDestPath + "_(" + idx + ")." + getExtensionOf(dest);

                dest = new File(newDestPath);
            } else {
                dest.createNewFile();
            }

            BufferedInputStream bis = new BufferedInputStream(
                                          new FileInputStream(src)
                                      );

            BufferedOutputStream bos = new BufferedOutputStream(
                                           new FileOutputStream(dest)
                                       );

            int b;
            while((b = bis.read()) != EOF) { bos.write(b);}

            log(src.getCanonicalPath());
            bos.flush();
            bos.close();
        } catch (IOException e) {
            dest.delete();
            return false;
        }

        return true;
    }

    
    /**
     *
     * The method <code>delete()</code> only delete a file
     * or an empty directory. So, to clear all the files
     * and subdirectory, deleting files under subdirectory
     * must be preprocessed.
     * In the file list from the <code>listFiles()</code>,
     * an directory has the priority. So calling this method
     * recursively all the file and subdirectory is deleted,
     * contains the root directory.
     *
     * @param rootDir
     *        A root directory to start clearing all the file and subdirectries.
     *
     * @return true if clearing is end with no error, false otherwise.
     */
    private static boolean clear(File rootDir) {
        if(rootDir.exists()) {
            if(rootDir.isDirectory()) {
                File[] files = rootDir.listFiles();

                if(files != null) {
                    for(File f : files) {
                        clear(f);
                    }
                }
            }

            return rootDir.delete();
        }

        return false;
    }
    

    /**
     *
     * Checks the given file is zipped file, basis of the extensions
     * like zip, jar, tar, A00, Z01, tar_Z, 001
     *
     * @param file The file to check whether zipped file or not
     *
     * @return true if <code>file</code> is zipped file,
     *         false otherwise.
     */
    private static final boolean isZipped(File file) {
        String ext = getExtensionOf(file);
        return (ext.matches("[az]{1}0[0-9]{1}")
                || ext.matches("[\\w\\d]{1,3}_Z")
                || ext.matches("0[\\d]{1}[1-9]{1}")
                || EXTENSION_SET.contains(ext));
    }

    
    /**
     *
     * Returns extension from a given <code>File</code> object.
     *
     * @param file <code>File</code> object of file to get extension
     *
     * @return extension that is converted to lower case.
     */
    private static final String getExtensionOf(File file) {
        String s = file.getName();
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

    
    /**
     *
     * @return A <code>String</code> data of path that read from <code>ROOT_INFO_FILE</code>
     *         if the path is matched with <code>PATH_REGEXP</code>
     *         null otherwise
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static String getRootPath()
    throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    new FileInputStream(ROOT_INFO_FILE),
                                    ENCODING
                                )
                             );
        String rootPath = br.readLine();
        br.close();

        return (rootPath.matches(PATH_REGEXP) ? rootPath : null);
    }

    
    /**
     *
     * @return An <code>ArrayList<code> object which contains
     *         <code>String</code> datas of path that has been read
     *         from <code>ROOT_INFO_FILE</code> if the data exist,
     *         <code>null</code> otherwise
     */
    private static List<String> getRootPathList()
    throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    new FileInputStream(ROOT_INFO_FILE),
                                    ENCODING
                                )
                            );

        List<String> list = new ArrayList<String>(0);
        String rootPath = null;
        while((rootPath = br.readLine()) != null) {
            if(rootPath.matches(PATH_REGEXP)
            && !list.contains(rootPath)) {
                list.add(rootPath);
            }
        }
        br.close();

        return (list.size() > 0) ? list : null;
    }


    /**
     *
     * @return A <code>String</code> data of path that read from <code>STORAGE_INFO_FILE</code>
     *         if the path is matched with <code>PATH_REGEXP</code>
     *         null otherwise
     */
    private static String getStoragePath()
    throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(
                                new InputStreamReader(
                                    new FileInputStream(STORAGE_INFO_FILE),
                                    ENCODING
                                )
                            );
        String storagePath = br.readLine();
        br.close();

        return (storagePath.matches(PATH_REGEXP) ? storagePath : null);
    }
    

    /**
     *
     * This program finds all zipped files from different directory each other.
     * and stores the specified directory(D:\\Finder\\Storage).
     * So if duplicate file is skipped or overwrited a vulnerability may occur.
     *
     * Let the root directory is ROOT.
     * And let ROOT\hacking\a.zip is hacking file, ROOT\normal\a.zip is normal file.
     * then hacking file and normal file exists under different directory with same name.
     * So next two cases can be considered.
     *
     * i)  Let this program skips the duplicate, and
     *     ROOT\normal\a.zip is found beforehand, and ROOT\hacking\a.zip is found afterhand.
     *     then ROOT\normal\a.zip is reproduced and ROOT\hacking\a.zip is skipped, so only
     *     normal file exists in storage.
     *     Hence a hacking file remains under the root directory but users cannot recognize that
     *     unless they search all subdirectories and files under ROOT by handwork.
     *
     * ii) Let this program overwrites the duplicate, and
     *     ROOT\hacking\a.zip is found beforehand, and ROOT\normal\a.zip is found afterhand.
     *     then ROOT\hacking\a.zip is overwrited with ROOT\normal\b.zip, so only normal file
     *     exists in storage.
     *     Hence a hacking file remains under the root directory but users cannot recognize that
     *     unless they search all subdirectories and files under ROOT by handwork.
     *
     * Therefore the duplicate files are all MUST be reproduced by numbering.
     *
     * @param fileName file name to check whether it is duplcated or not
     *
     * @return the index to be numbered on the duplicate next fileName
     */
    private static final String getNextIndexOf(String fileName, File dir) {
        String name = fileName.substring(0, fileName.lastIndexOf('.'));
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);

        int count = 1;
        File[] files = dir.listFiles();

        for(File file : files) {
            String s = file.getName();
            if(s.matches(name + "_\\([0-9]+\\)" + ext)) {
                ++count;
            }
        }

        return Integer.toString(count);
    }

    /**
     *
     * If <code>dir</code> is an empty directory, <code>dir.listFiles()</code>
     * returns <code>null</code>
     * or <code>dir.listFiles().length</code> is under 1.
     *
     * @param dir directory to check whether it has nothing(file, directory, etc.)
     *
     * @return true if empty directory, false otherwise.
     */
    private static boolean isEmptyDir(File dir) {
        return ((dir.listFiles() == null)
                || (dir.listFiles().length < 1));
    }


    private static void terminate(String message) {
        JOptionPane.showMessageDialog(null,
                                      message + "\nProgram will be exit.",
                                      "Exit",
                                      INFORMATION_MESSAGE);
        log("Program is exit." + NEW_LINE);
        close();
    }


    private static void close() {
        try {
            LOGGER.close();
        } catch (IOException e) {
            e.printStackTrace();
            logPrintErrorDialog(e.getMessage());
        }
    }


    /**
     * 
     * This methods are used when logging on a file or printing on a console,
     * showing a dialogue, or both or all of them, with a same message.   
     */
    private static void logPrintInfoDialog(String message) {
        logPrint(message);
        infoDialog(message);
    }

    private static void logPrintErrorDialog(String message) {
        logPrint(message);
        errorDialog(message);
    }

    private static void logPrint(String message) {
        log(message);
        System.out.println(message);
    }

    private static void log(String s) {
        LOGGER.log(s);
    }

    /**
     *
     * all (blah blah)Dialog(String message) methods call the dialogue methods
     * of <code>javax.swing.JOptionPane</code>.
     *
     * see {@link javax.swing.JOptionPane}
     *
     * @see javax.swing.JOptionPane
     *
     * @param message <code>String</code> object to display on a dialogue
     */
    private static void infoDialog(String message) {
        JOptionPane.showMessageDialog(null,
                                      message,
                                      INFORMATION_TITLE,
                                      INFORMATION_MESSAGE);
    }

    private static void errorDialog(String message) {
        JOptionPane.showMessageDialog(null,
                                      "An error occured: " + message,
                                      ERROR_TITLE,
                                      ERROR_MESSAGE);
    }

    private static void warningDialog(String message) {
        JOptionPane.showMessageDialog(null,
                                      message,
                                      WARNING_TITLE,
                                      WARNING_MESSAGE);
    }

    private static int confirmDialog(String message) {
        return JOptionPane.showConfirmDialog(null,
                                             message,
                                             NO_TITLE,
                                             YES_NO_OPTION);
    }

    private static String inputDialog(String message, String initialValue) {
        return ((String) JOptionPane.showInputDialog(null,
                                                     message,
                                                     INPUT_TITLE,
                                                     PLAIN_MESSAGE,
                                                     null,
                                                     null,
                                                     initialValue));
    }

    
    /**
     * 
     * This method opens a file which is located on a given path.
     * In this program, this method used for show the reproduced files
     * to users. 
     * 
     * @param path <code>String</code> data of path to open.
     * 
     * @throws IOException
     */
    private static void open(String path)
    throws IOException {
        Runtime.getRuntime().exec("explorer " + path);
    }


    static class Logger {
        private static final String LOG_PATH = DATA_PATH + "log\\";
        private static final File LOG_DIR = new File(LOG_PATH);
        private static final File LOG_FILE = new File(LOG_PATH + currentDate() + ".txt");
        private static FileWriter writer = null;

        
        /**
         *
         * Since This class is a logger only for <code>Finder<code>,
         * and a stream of the logger is opened when the program is started,
         * closed when the program is terminnted.
         * So there is no need to construct multiple logger object,
         * and it is desirable that logger object is declared with singleton.
         */
        private static Logger logger = new Logger();

        public Logger() {
            try {
                this.init();
            } catch (IOException e) {
                e.printStackTrace();
                logPrintErrorDialog("Failed to initialze logger");
            }
        }
        
        
        //Getter of the instance of the singleton object
        private static Logger logger() {
            return logger;
        }

        
        /**
         *
         * Make a nonexistent directory and log files.
         * A log file name is date e.g. 201003(yyMMdd)
         *
         * @return true if initializaion is normally finished
         *         false otherwise.
         *
         * @throws IOException
         *         If log directory or log file is not created
         */
        private boolean init()
        throws IOException {
            boolean result;

            if(!LOG_FILE.exists()) {
                LOG_DIR.mkdirs();
                LOG_FILE.createNewFile();
            }

            result = LOG_FILE.setWritable(true);
            writer = new FileWriter(LOG_FILE, true);

            return result;
        }

        
        /**
         *
         * @param s string data to be logged
         *
         * @return true if data is normally logged
         *         false otherwise
         */
        private boolean log(String s){
            try {
                writer.write(currentTime() + " " + s + NEW_LINE);
                writer.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                errorDialog(e.getMessage());
                return false;
            }
        }

        
        /**
         *
         * Cleans logger(i.e. <code>FileWriter</code>) resources.
         * In other words, this method flushes output buffer and closes the stream.
         * Users should not modify the log files. So after cleaning resources should
         * set log file read only.
         *
         * @throws IOException
         *         If output buffer is not flushed or if output stream is not closed or both.
         */
        private void close()
        throws IOException {
            writer.flush();
            writer.close();
            LOG_FILE.setReadOnly();
        }


        static String currentTime() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                       .format(new Date())
                       .toString();
        }


        static String currentDate() {
            return new SimpleDateFormat("yyMMdd")
                       .format(new Date())
                       .toString();
        }
    }

}