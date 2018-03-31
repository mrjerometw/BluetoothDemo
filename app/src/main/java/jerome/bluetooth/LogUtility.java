package jerome.bluetooth;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtility {

    private boolean mEnableRecord;
    private boolean mEnableShowLogcat;
    private String mFileName;
    private String mFolderPath;
    private LogLevel mLogLevel;
    private SimpleDateFormat mSimpleDateFormatter;
    private String mTagName;

    public enum LogLevel {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6);

        public int type;

        private LogLevel(int p) {
            this.type = p;
        }
    }

    private static class SingletonHolder {
        private static final LogUtility sSingleton = new LogUtility();

        private SingletonHolder() {
        }
    }

    public static LogUtility getInstance() {
        return SingletonHolder.sSingleton;
    }

    private LogUtility() {
        this.mFileName = "log";
        this.mFolderPath = "D2000_Setting_LOG";
        this.mTagName = "Setting";
        this.mSimpleDateFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.mLogLevel = LogLevel.DEBUG;
        this.mEnableShowLogcat = true;
        this.mEnableRecord = false;
    }

    public void setTagName(String tagName) {
        this.mTagName = tagName;
    }

    public void enableRecord(boolean enable) {
        this.mEnableRecord = enable;
    }

    public boolean isEnableRecord() {
        return this.mEnableRecord;
    }

    public void enableShowLogCat(boolean enable) {
        this.mEnableShowLogcat = enable;
    }

    public boolean isShowLogcat() {
        return this.mEnableShowLogcat;
    }

    public void setFolderName(String folderName) {
        this.mFolderPath = folderName;
    }

    public void setFileName(String fileName) {
        this.mFileName = fileName;
    }

    public void setLogLevel(LogLevel level) {
        this.mLogLevel = level;
    }

    public static boolean d(String className, String method) {
        return getInstance().writeLog(className, method, "", LogLevel.DEBUG);
    }

    public static boolean v(String className, String method, String msg) {
        return getInstance().writeLog(className, method, msg, LogLevel.VERBOSE);
    }

    public static boolean d(String className, String method, String msg) {
        return getInstance().writeLog(className, method, msg, LogLevel.DEBUG);
    }

    public static boolean i(String className, String method, String msg) {
        return getInstance().writeLog(className, method, msg, LogLevel.INFO);
    }

    public static boolean w(String className, String method, String msg) {
        return getInstance().writeLog(className, method, msg, LogLevel.WARN);
    }

    public static boolean e(String className, String method, String msg) {
        return getInstance().writeLog(className, method, msg, LogLevel.ERROR);
    }

    public boolean writeLog(String className, String methodName) {
        return write(mEnableRecord, mEnableShowLogcat, className, methodName, "", mLogLevel);
    }

    public boolean writeLog(String className, String methodName, String msg) {
        return write(mEnableRecord, mEnableShowLogcat, className, methodName, msg, mLogLevel);
    }

    public boolean writeLog(String className, String methodName, byte[] msg, LogLevel logLevel) {
        return write(mEnableRecord, mEnableShowLogcat, className, methodName, msg, logLevel);
    }

    public boolean writeLog(String className, String methodName, String msg, LogLevel logLevel) {
        return write(mEnableRecord, mEnableShowLogcat, className, methodName, msg, logLevel);
    }

    public boolean writeLog(String className, String methodName, String msg, LogLevel logLevel, boolean enforceRecord) {
        return write(enforceRecord, mEnableShowLogcat, className, methodName, msg, logLevel);
    }

    private boolean write(boolean bRecord, boolean isShowLogcat, String className, String methodName, byte[] msg, LogLevel logLevel) {
        StringBuilder strMsg = new StringBuilder();
        for (int i = 0; i < msg.length - 1; i++) {
            strMsg.append(String.format("%d,", new Object[]{Byte.valueOf(msg[i])}));
        }
        return write(bRecord, isShowLogcat, className, methodName, strMsg.toString(), logLevel);
    }

    private boolean write(boolean enableRecord, boolean enableShowLogcat, String className, String methodName, String msg, LogLevel logLevel) {
        if (enableShowLogcat) {
            String mergeMsg = className + ": [" + methodName + "]: " + msg;
            if (logLevel.ordinal() >= LogLevel.VERBOSE.ordinal()) {
                Log.v(mTagName, mergeMsg);
            } else if (logLevel.ordinal() >= LogLevel.DEBUG.ordinal()) {
                Log.d(mTagName, mergeMsg);
            } else if (logLevel.ordinal() >= LogLevel.INFO.ordinal()) {
                Log.i(mTagName, mergeMsg);
            } else if (logLevel.ordinal() >= LogLevel.WARN.ordinal()) {
                Log.w(mTagName, mergeMsg);
            } else if (logLevel.ordinal() >= LogLevel.ERROR.ordinal()) {
                Log.e(mTagName, mergeMsg);
            }
        }
        if (enableRecord) {
            File outDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + this.mFolderPath);
            if (!outDir.isDirectory()) {
                outDir.mkdir();
            }
            String fileName = mFileName;
            if (logLevel == LogLevel.ERROR) {
                fileName = fileName + ".error";
            } else if (logLevel == LogLevel.DEBUG) {
                fileName = fileName + ".debug";
            } else if (logLevel == LogLevel.INFO) {
                fileName = fileName + ".info";
            }
            try {
                Writer writer = new BufferedWriter(new FileWriter(new File(outDir, fileName), true));
                writer.write("[" + mSimpleDateFormatter.format(new Date()) + "-> " + className + "->" + methodName + "]: " + msg + "\n");
                writer.flush();
                writer.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}