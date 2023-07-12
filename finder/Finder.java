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
 * 지정된 루트 디렉토리 및 하위 디렉토리에 존재하는 압축파일을 찾아
 * 지정된 저장소 디렉토리로 복제한다.
 * 일단 빨리 쓰려고 작성한 코드이다보니 필요한 모든 상수, 메소드 등이 
 * 이 클래스에 작성되어 있다.
 * 모든 출력은 콘솔을 통해 이루어지므로 cmd 등 CLI에서 실행하는 게 좋(다고 생각한)다.
 * 
 * @since 07/09/2020
 */
public final class Finder {
    /**
     * 파일 실행에 필요한 데이터들을 읽어올 경로를 지정한다.
     */
    private static final String DATA_PATH = "{{YOUR_PATH}}";
    private static final File DATA_DIR = new File(DATA_PATH);
    private static final File ROOT_INFO_FILE = new File(DATA_PATH + "root.txt");
    private static final File STORAGE_INFO_FILE = new File(DATA_PATH + "storage.txt");
    private static final int EOF = -1; //End Of File
    private static final String ENCODING = "UTF-8"; //Default encoding is UTF-8
    
    
    /**
     *
     * 개행 문자는 운영체제마다 다르다.
     * Windows: \r\n
     * MacOS: \r
     * Linux: \n
     * 
     * System 클래스의 Property 객체로 읽어온다.
     */
    private static final String NEW_LINE = System.getProperties()
                                                 .getProperty("line.separator");
    
    
    /**
     *
     * 초기화 작업의 수행 여부를 나타낸다.
     * True면 초기화가 수행되었음을 의미한다.
     * root directory 등이 존재하지 않음에 따른
     * NullPointerException 등의 예외를 막기 위해 쓰인다.
     */
    private static boolean initialized = false;
    
    
    /**
     *
     * 보통 CD에서 압축파일을 찾았기 때문에 기본 경로는 E:\로 설정했다.
     * 찾아낸 압축파일을 저장할 경로는 마음대로.
     */
    public static final String DEFAULT_ROOT_PATH = "E:\\";
    private static final String DEFAULT_STORAGE_PATH = "{{YOUR_PATH}}";

    
    /**
     *
     * 루트 디렉토리는 사용자로부터 입력받기 때문에 검증 절차가 필요하다.
     * Windows의 모든 절대경로는 알파벳 대문자와 :로 시작하며,
     * 파일이나 디렉토리 이름에 *, :, /, ", <, >, \, |는 쓰일 수 없다.
     * 이는 정규표현식에서 [^^:\\*\"/<>\\|\\\\]+로 나타내어진다.
     * 
     * Ex)
     * D:                          : usable
     * A:\                         : usable
     * D:\abc\def                  : usable
     * D:\|chungsungchungsung^^7|\ : unusable      (|, ^ 때문에 안 됨)
     * C:/asd//asdaf               : unusable      (경로 구분자는 하나만!)
     * D:\\AA\bbb                  : unusable      (경로 구분자는 하나만!(2))
     * D:asdg                      : unusable      (:은 드라이브 이름에만 쓰일 수 있다.)
     */
    public static final String PATH_REGEXP = "[A-Z]:(((\\\\|/)[^\\^:\\*\"</>\\|\\\\]+)*(\\\\|/)?)?" +
                                             "((\r\n|\r|\n)+[A-Z]:(((\\\\|/)[^\\^:\\*\"</>\\|\\\\]+)*(\\\\|/)?)?)*" +
                                             "(\r\n|\r|\n)*";
    
    /**
     * 
     * 상태 코드.
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
     * 대화상자에 쓰일 제목을 나타내는 상수.
     * 여기서 쓰이는 대화상자는 정보, 경고, 에러, 입력의 
     * 네 가지 유형으로 나뉜다.
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
     * 대화상자로 표시할 메시지 유형을 나타내는 정수 값이다.
     * 자세한 내용은 {@link javax.swing.JOptionPane} 참고.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
    private static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
    private static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
    private static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

    /**
     *
     * 대화상자로 사용자에게 보여줄 선택지를 나타내는 상수.
     * <code>JOptionPane.YES_NO_OPTION</code>는 예/아니오를 표시한다.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;

    /**
     *
     * The integer values corresponding to option that has been chosen.
     * Please see {@link javax.swing.JOptionPane} to get information of exact value.
     * 
     * 사용자가 선택한 옵션을 나타내는 상수.
     * 각각 예, 아니오, 취소, 창 닫기 버튼을 눌렀을 때 반환되는 값이다.
     *
     * @see javax.swing.JOptionPane
     */
    private static final int YES_OPTION = JOptionPane.YES_OPTION;
    private static final int NO_OPTION = JOptionPane.NO_OPTION;
    private static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
    private static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

    /**
     *
     * 압축파일 확장자 집합.
     * 순서와 무관하므로 Set으로 선언했다.
     */
    private static final Set<String> EXTENSION_SET = new HashSet<String>(0);

    /**
     * 
     * 탐색이 끝난 후 복제할 압축파일의 경로는 <code>TASK_LIST</code>에 추가된다.
     * 이후 <code>TASK_LIST</code>를 순회하며 복제를 진행한다.
     * 복제에 실패하면 <code>FAILED_TASK_LIST</code>에 추가해 다시 시도한다.
     */
    private static final List<File> TASK_LIST = new ArrayList<File>(0);
    private static final List<File> FAILED_TASK_LIST = new ArrayList<File>(0);

    /**
     *
     * 로그를 기록하기 위한 Logger 클래스.
     * inner class로 선언되어 있으며 singleton pattern이 쓰였다.
     */
    private static final Logger LOGGER = Logger.logger();


    /**
     * <code>main()</code> 메소드 역할을 한다.
     *
     * @return 상태 코드.
     *         정상적으로 종료되면 0을, 아니면 다른 값이 반환된다.
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
     * root directory 경로, 확장자 목록 등
     * Finder가 실행되기 위한 전제조건을 설정한다.
     * 필요한 게 없으면 생성하는 절차다.
     *
     * @return {@value #NORMAL} if initialization is succeeded,
     *         {@value #NOT_INITED} otherwise.
     */
    private static final boolean init() {
        if(initialized) { return true; }

        try {
            String message = "Initializing . . .";
            logPrint(message);

            //상태 코드에 대응되는 메시지를 지정한다.
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

            //압축파일 확장자명을 집합에 추가한다.
            if(EXTENSION_SET.size() < 1) {
                EXTENSION_SET.add("ace");
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

            //데이터 디렉토리가 없으면 만든다.
            //데이터 디렉토리에는
            // root 경로, storage 경로 등에 대한 정보가 적힌 파일이 저장된다.
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

            //저장소에 대한 정보가 적힐 파일이 없으면 생성한다.
            //있으면 유효한 저장소가 지정되어 있는지 확인한다.
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

            //root에 대한 정보가 적힐 파일이 없으면 생성한다.
            //있으면 유효한 root가 지정되어 있는지 확인한다.
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
     * 전체적인 작업 수행이 이루어진다.
     *
     * @return 상태 코드
     */
    private static int run() {
    	//초기화가 되지 않았으면 종료한다.
        if(!initialized) {
            if(!init()) {
                return NOT_INITED;
            }
        }
        
        //root path 정보를 읽어온다.
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

        //root path에 대한 정보가 없으면 종료한다.
        if((rootPathList == null)
        || (rootPathList.size() < 1)) {
            return NO_ROOT_INFO;
        }

        /*실행부*/
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
                    if((storageDir == null)
                    || !storageDir.exists()) {
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
     * root에 대한 데이터가 저장될 파일을 생성한다.
     * 
     * @return root.txt가 생성되면 <code>true</code> 아니면 <code>false</code>
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

    /**
     * 저장소 디렉토리를 생성한다.
     * 
     * @return 저장소 디렉토리가 생성됐으면 <code>true</code>, 아니면 <code>false</code>
     */
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
     * 압축 파일을 찾아 <code>TASK_LIST</code>에 추가한다.
     * 
     * @param  rootPath the directory path to start searching
     *
     * @return 상태 코드
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
     * 압축파일을 저장소로 복제한다.
     * 바이트 단위로 복제하며, 같은 이름의 파일은 sequence number를 붙여 모두 복제한다.
     *
     * @param src  복사될 파일
     * @param dest 붙여넣을 파일
     *
     * @return true if reproducing is normally completed, false otherwise.
     */
    private static final boolean reproduce(File dest, File src) {
        try {
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
            bis.close();

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
     * 저장소를 비우기 위한 메소드.
     * 이전에 복제한 파일이 남지 않게 하려면 수동으로 지워야 한다.
     *
     * @param rootDir
     *        <code>rootDir</code>과 하위 디렉토리의 모든 파일을 삭제한다.
     *
     * @return 작업 성공 여부. 성공시 <code>true</code>
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
     * 압축파일인지 판별하는 메소드.
     * 분할압축도 포함된다.
     *
     * @param file 
     *        판별할 파일(경로)
     *
     * @return <code>file</code>가 압축파일이면 <code>true</code>
     *         아니면 <code>false</code>
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
     * 주어진 경로에서 확장자명만 떼어내 반환한다.
     *
     * @param file
     *        확장자명을 얻을 파일명
     *
     * @return 소문자로 변환된 확장자명
     */
    private static final String getExtensionOf(File file) {
        String s = file.getName();
        return s.substring(s.lastIndexOf('.') + 1).toLowerCase();
    }

    
    /**
     *
     * 처음에는 root path를 하나만 지정할 수 있게 했는데, 이제 여러 개로 지정할 수 있게 했다.
     * 그래서 사실 이제 필요없는데 일단 혹시 모르니까..
     * 
     * @return root info file에서 얻어낸 경로를 반환한다.
     *         경로가 형식에 맞지 않는 경우 null을 반환한다.
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
     * root path를 읽어 리스트로 반환한다.
     * 여러 개로 지정하면 여러 개의 경로 모드를 리스트에 추가해 반환한다.
     * 유효하지 않은 경로는 리스트에 추가되지 않는다.
     * 
     * @return root info file에서 얻어낸 경로를 리스트로 반환한다.
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
     * 저장소 경로를 읽어온다.
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
     * 파일이름이 중복되는 경우 sequence number를 붙이기 위한 메소드.
     * 이렇게 하는 이유는 모든 파일을 복제하기 위함인데, 다음과 같은 시나리오를 생각할 수 있다.
     * 
     * hacking\a.zip이라는 악성 파일과 normal\a.zip이라는 정상 파일이 있다고 하자.
     * 중복 파일에 대해 할 수 있는 작업은 건너뛰거나, 덮어쓰거나, 번호를 붙이는 방법이 있다.
     * 
     * i) 건너뛰기
     * 이 경우 normal\a.zip이 먼저 복제되면 hacking\a.zip은 건너뛰게 된다.
     * 즉 외부에서 들여온 저장매체에 악성 파일이 있는데, 저장소에는 이 파일이 없으므로
     * 검사하지 않게 되는 것이다.
     * 
     * ii) 덮어쓰기
     * 이 경우 hacking\a.zip이 먼저 복제되면 normal\a.zip으로 덮어씌워진다.
     * 즉 악성 파일이 저장소에서 탐지되지 않는다.
     * 
     * 따라서 중복파일을 포함해 모든 파일이 복제되어야 하며, 이를 위해서는 sequence number를 붙여야 한다.
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
     * 주어진 디렉토리가 비어있거나 디렉토리 자체가 없는지 검사한다.
     * 
     * @param dir
     *        검사할 디렉토리
     *
     * @return 비어있거나 존재하지 않는 디렉토리면 <code>true</code>, 아니면 <code>false</code>
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
     * 복제가 끝나면 저장소 디렉토리를 파일 탐색기로 열어 보여준다.
     * 
     * @param path
     *        파일 탐색기로 열 경로
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
         * 
         * Finder만을 위한 로거 클래스이므로 Singleton 객체로 만든다.
         * 
         */
        private static Logger logger = new Logger();

        private Logger() {
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
         * 로그를 저장할 파일을 생성한다.
         * 파일명은 yyMMdd다.
         * 로그 파일은 쓰기가 가능하게 설정한 후 종료 시 읽기전용으로 변경한다.
         * 
         * @return 정상적으로 작업이 종료되면 <code>true</code>, 아니면 <code>false</code>
         *
         * @throws IOException
         *         로그 파일이나 디렉토리가 생성되지 않은 경우에 해당한다.
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
         * @param s
         *        로그 파일에 기록될 데이터
         *
         * @return 로깅이 정상적으로 수행됐으면 <code>true</code>, 아니면 <code>false</code>\

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
         * logger에 쓰인 모든 리소스를 정리한다.
         * 로그 파일은 읽기전용으로 설정한다.
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
