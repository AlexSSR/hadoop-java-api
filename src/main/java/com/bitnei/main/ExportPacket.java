package com.bitnei.main;

import com.bitnei.core.util.PropertiesUtil;
import net.iharder.base64.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @Date 2018/4/23
 */
public class ExportPacket {

    private static Connection connection = null;
    private static Properties properties = null;

    private static Table getTable(String tableNames) throws IOException {
        TableName tableName = TableName.valueOf(tableNames);
        return connection.getTable(tableName);
    }

    public static void main(String[] args) throws IOException {
        properties = PropertiesUtil.getProperties("hbase.properties");

        Configuration configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", properties.getProperty("hbase.zookeeper.quorum"));
        configuration.set("hbase.client.scanner.caching", "500");
        connection = ConnectionFactory.createConnection(configuration);
        // 首次getTable会很慢， 在初始化时候get一下
        getTable("test_rawdata").close();

        List<String> vins = IOUtils.readLines(new FileInputStream(properties.getProperty("vid2vin.file.path")), "utf-8");
        write(vins);
    }

    private static void write(List<String> vins) throws IOException {

        for (String vin : vins) {
            Map<String, List<String>> res = new HashMap<>();
            String[] vidVin = vin.split(",");

            Table table = getTable("packet");
            Scan scan = new Scan();
            scan.setStartRow((vidVin[0] + "_" + properties.getProperty("start.row.timestamp")).getBytes());
            scan.setStopRow((vidVin[0] + "_" + properties.getProperty("stop.row.timestamp")).getBytes());
            scan.setMaxVersions(20);

            scan.addColumn("cf".getBytes(), "stime".getBytes());
            scan.addColumn("cf".getBytes(), "data".getBytes());

            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                byte[] sTime = result.getValue("cf".getBytes(), "stime".getBytes());
                byte[] data = result.getValue("cf".getBytes(), "data".getBytes());

                if (sTime == null || data == null) {
                    continue;
                }

                String terminalTime = new String(result.getRow()).split("_")[1];
                Date date = new Date(Long.valueOf(terminalTime));
                String terminalTimeFormat = new SimpleDateFormat("yyyyMMdd").format(date);

                StringBuilder builder = new StringBuilder();
                builder.append(new String(sTime));
                builder.append(",");
                String s = Base64.encodeBytes(data);
                s = s.replaceAll("\n", "");
                builder.append(s);

                if (res.get(terminalTimeFormat) == null) {
                    List<String> list = new ArrayList<>();
                    list.add(builder.toString());
                    res.put(terminalTimeFormat, list);
                } else {
                    res.get(terminalTimeFormat).add(builder.toString());
                }
            }

            for (Map.Entry<String, List<String>> entry : res.entrySet()) {
                String day = entry.getKey();
                String fileName = properties.getProperty("result.file.path") + "/" + day.substring(0, 4) + "/" + day.substring(4, 6) + "/" + day.substring(6, 8) + "/" + vidVin[1];
                File file = new File(fileName);
                if (!file.getParentFile().exists()) {
                    boolean b = file.getParentFile().mkdirs();
                }
                FileWriter fileWriter = new FileWriter(file);
                IOUtils.writeLines(entry.getValue(), "\n", fileWriter);
                fileWriter.flush();
                fileWriter.close();
            }
            System.out.println(vin);
        }
    }
}
