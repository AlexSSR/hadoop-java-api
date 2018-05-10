package com.bitnei.core.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: zhaogd
 * Date: 2017/9/25
 * @author zhaogd
 */
public class PropertiesUtil {
    private static final Logger logger = LogManager.getLogger(PropertiesUtil.class);

    public static Properties getProperties(String filename) throws IOException {
        Properties properties = new Properties();
        File file = new File(System.getProperty("user.dir") + File.separator + filename);
        InputStream inputStream;
        if (file.exists()) {
            inputStream = new FileInputStream(file);
            logger.info("加载配置文件，路径：{}", file.getAbsolutePath());
        } else {
            inputStream = PropertiesUtil.class.getResourceAsStream("/" + filename);
            logger.info("加载配置文件，路径：{}", PropertiesUtil.class.getResource("/" + filename).getPath());
        }
        properties.load(inputStream);
        properties.list(System.out);
        return properties;
    }
}
