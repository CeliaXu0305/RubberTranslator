package com.rubbertranslator.mvp.modules.log;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Raven
 * @version 1.0
 * date 2020/5/8 10:49
 */
public class LoggerManager {
    private static final String logFilePath = System.getProperty("user.dir") + "/RubberTranslator/log";

    private static Logger fileLogger = Logger.getLogger("com.rubbertranslator");

    // 修改simpleformater格式
    static
    {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%2$s] [%4$s] %5$s %6$s %n");
    }


    public static void configLog() {
        StringBuilder logPath = new StringBuilder();
        //设置文件名
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        //设置保存路径
        logPath.append(logFilePath).append("/").append(sdf.format(new Date())).append(".log");
        File dir = new File(logPath.toString());
        if(!dir.getParentFile().exists()){
            dir.getParentFile().mkdirs();
        }

        //将输出handler加入logger
        try {
            FileHandler fileHandler = new FileHandler(logPath.toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileLogger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
