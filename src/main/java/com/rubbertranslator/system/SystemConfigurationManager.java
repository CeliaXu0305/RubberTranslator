package com.rubbertranslator.system;

import com.google.gson.Gson;
import com.rubbertranslator.utils.FileUtil;
import com.rubbertranslator.utils.JsonUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemConfigurationManager {
    // 新配置文件路径 更改为用户home目录
    public String configJsonDir = System.getProperty("user.dir") + "/RubberTranslator/config";
    public String configJsonPath = "";
    private SystemConfiguration systemConfiguration;

    public SystemConfiguration getSystemConfiguration() {
        return systemConfiguration;
    }

    /**
     * 持久化配置文件到json
     * 需要 systemConfiguration 和  configJsonPath 不为空
     */
    public void saveConfigFile(){
        if (systemConfiguration == null || configJsonPath == null){
            Logger.getLogger(SystemResourceManager.class.getName()).log(Level.SEVERE,"更新设置时出错，" +
                    "配置类或配置文件路径为空");
            return;
        }
        // 静态代理还原
        String json = JsonUtil.serialize(systemConfiguration.getConfiguration());
        // 使用ui线程来写入
        try {
            FileUtil.writeStringToFile(new File(configJsonPath), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.getLogger(SystemResourceManager.class.getName()).log(Level.SEVERE,"更新设置时出错",e);
        }
    }

    /**
     * 加载配置文件
     */
    public boolean init() {
        String curVersion = getCurrentVersion();
        if(null == curVersion) return false;

        // 初始化configJson文件路径
        configJsonPath = getConfigJsonPath(curVersion);

        if(isCorrectConfigFileExist(configJsonPath)){       // 如果当前版本配置文件已经存在
            systemConfiguration = generateConfigFromExistFile(configJsonPath);
        }else{      // 不存在
            SystemConfiguration defaultConfig = generateDefaultConfig();
            // 读取配置文件路径下的最大版本号文件
            File dir = new File(configJsonPath).getParentFile();
            String maxVersionConfigFileName =  getMaxVersionConfigPath(dir);
            if(maxVersionConfigFileName == null){   // 不存在旧的config文件。
                // 直接序列化默认config即可
                systemConfiguration = defaultConfig;
                saveConfigFile();
            }else{      // 存在旧的config文件
                String maxVersionConfigFilePath = getConfigJsonPath(maxVersionConfigFileName);
                SystemConfiguration oldConfig = generateConfigFromExistFile(maxVersionConfigFilePath);
                systemConfiguration = mergeConfig(defaultConfig, oldConfig);
                saveConfigFile();
            }
        }

        return systemConfiguration == null;
    }


    /**
     * 取得当前软件版本
     * @return 版本号。 如 v2.0.0
     */
    private String getCurrentVersion(){
        String version = null;
        try {
            InputStream resourceAsStream = SystemResourceManager.class.getResourceAsStream("/version_control/version.txt");
            version = FileUtil.readInputStreamToString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,"系统版本号缺失，无法打开软件，请向开发者联系");
            return null;
        }
        return version;
    }


    /**
     * 根据软件版本，得到配置json文件路径
     * @param version
     * @return 当前配置json文件路径
     */
    private String getConfigJsonPath(String version){
        String tmpPath = configJsonDir + "/configuration-" + version +".json";
        Logger.getLogger(this.getClass().getName()).log(Level.FINE,"当前配置文件路径"+tmpPath);
        return tmpPath;
    }

    /**
     * 当前版本对应配置文件是否存在
     * @param configPath
     * @return
     */
    private boolean isCorrectConfigFileExist(String configPath){
        File file = new File(configPath);
        return file.exists();
    }


    /**
     * 从已存在的对应版本的配置文件中生成配置
     * @param configJsonPath
     */
    private SystemConfiguration generateConfigFromExistFile(String configJsonPath) {
        // 加载本地配置
        File file = new File(configJsonPath);
        String configJson = null;
        SystemConfiguration configuration = null;
        try {
            configJson = FileUtil.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.getLogger(SystemResourceManager.class.getName()).severe(e.getLocalizedMessage());
            return null;
        }
        // json --> object
        Gson gson = new Gson();
        // 原始配置记录
        configuration = gson.fromJson(configJson, SystemConfiguration.class);
        Logger.getLogger(SystemResourceManager.class.getName()).info("加载配置" + configJson);
        return configuration;
    }

    /**
     * 生成默认配置类
     * @return 成功 默认配置类
     *         失败 null
     */
    private SystemConfiguration generateDefaultConfig(){
        // 加载本地配置
        String configJson = null;
        SystemConfiguration configuration;
        try {
            InputStream resourceAsStream = SystemResourceManager.class.getResourceAsStream("/config/default_configuration.json");
            configJson = FileUtil.readInputStreamToString(resourceAsStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.getLogger(SystemResourceManager.class.getName()).severe(e.getLocalizedMessage());
            return null;
        }
        // json --> object
        Gson gson = new Gson();
        // 原始配置记录
        configuration = gson.fromJson(configJson, SystemConfiguration.class);
        Logger.getLogger(SystemResourceManager.class.getName()).info("加载配置" + configJson);
        return configuration;
    }

    /**
     * 取得当前目录下的最大版本号文件路径
     * @return 成功，返回最大版本号
     *          失败， 返回null
     */
    private String getMaxVersionConfigPath(File dir){
        File[] files = dir.listFiles(File::isFile);
        if(files.length == 0) return null;

        String[] fileNames = new String[files.length];
        for(int i = 0;i<fileNames.length;i++){
            String tmpName =  files[i].getName();
            int startOffset = tmpName.indexOf("-");
            // if(startOffset == -1)?
            fileNames[i] = tmpName.substring(startOffset+1);
        }
        // 排序
        Arrays.sort(fileNames);
        // 返回最大的版本号
        return fileNames[fileNames.length-1];
    }

    /**
     * 升级程序，核心方法，在baseConfig的基础上，合并来自oldConfig中已经有的字段
     * @param baseConfig
     * @param oldConfig
     * @return 最终配置类
     */
    private SystemConfiguration mergeConfig(SystemConfiguration baseConfig, SystemConfiguration oldConfig) {
        return baseConfig;
    }
}
