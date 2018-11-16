package com.bitnei.main;

import com.bitnei.core.util.PropertiesUtil;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @Date 2018/7/27
 */
public class DeleteRealinfo {

    private static final Logger logger = LogManager.getLogger(DeleteRealinfo.class);

    private static Connection connection = null;
    private static Properties properties = null;

    static {
        try {
            properties = PropertiesUtil.getProperties("hbase.properties");

            Configuration configuration = HBaseConfiguration.create();
            configuration.set("hbase.zookeeper.quorum", properties.getProperty("hbase.zookeeper.quorum"));
            configuration.set("hbase.client.scanner.caching", "550");
            connection = ConnectionFactory.createConnection(configuration);
            getTable("test_rawdata").close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        List<String> vehInfos = IOUtils.readLines(new FileInputStream(properties.getProperty("vid2vin.file.path")), "utf-8");

        for (String vehInfo : vehInfos) {
            String[] fields = vehInfo.split(",");
            String vid = fields[1];

            Table table = getTable(args[0]);
            Scan scan = new Scan();
            scan.setStartRow((vid + "_" + properties.getProperty("start.row.timestamp")).getBytes());
            scan.setStopRow((vid + "_" + properties.getProperty("stop.row.timestamp")).getBytes());

            ResultScanner rs = table.getScanner(scan);
            List<Delete> list = getDeleteList(rs);
            if (list.size() > 0) {
                table.delete(list);
            }
            table.close();
        }
        connection.close();
    }

    private static List<Delete> getDeleteList(ResultScanner rs) {
        List<Delete> list = new ArrayList<>();
        try {
            for (Result r : rs) {
                Delete d = new Delete(r.getRow());
                logger.info(new String(d.getRow()));
                list.add(d);
            }
        } finally {
            rs.close();
        }
        return list;
    }

    private static Table getTable(String tableNames) throws IOException {
        TableName tableName = TableName.valueOf(tableNames);
        return connection.getTable(tableName);
    }
}
