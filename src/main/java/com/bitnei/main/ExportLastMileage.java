package com.bitnei.main;

import com.bitnei.core.util.PropertiesUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 导出最后有效里程，吉利使用
 *
 * @author zhaogd
 * @Date 2018/5/10
 */
public class ExportLastMileage {

    private static final Logger logger = LogManager.getLogger(ExportLastMileage.class);

    private static Connection connection = null;
    private static Properties properties = null;

    private static final String BLANK_SPACE = "\t";

    static {
        try {
            properties = PropertiesUtil.getProperties("hbase.properties");

            Configuration configuration = HBaseConfiguration.create();
            configuration.set("hbase.zookeeper.quorum", properties.getProperty("hbase.zookeeper.quorum"));
            configuration.set("hbase.client.scanner.caching", "500");
            connection = ConnectionFactory.createConnection(configuration);
            getTable("test_rawdata").close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Table getTable(String tableNames) throws IOException {
        TableName tableName = TableName.valueOf(tableNames);
        return connection.getTable(tableName);
    }

    public static void main(String[] args) throws IOException {
        List<String> vehInfos = IOUtils.readLines(new FileInputStream(properties.getProperty("vid2vin.file.path")), "utf-8");

        FileOutputStream fileOutputStream = new FileOutputStream(properties.getProperty("result.file.path"));
        for (String info : vehInfos) {
            String[] fields = info.split(" ");
            String vin = fields[0];
            String vid = fields[1];
            String licensePlate = fields[2];
            String iccid = fields.length != 4 ? "" : fields[3];

            Map<String, String> data = null;
            try {
                data = getData(vid);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(info, e);
            }

            if (data == null) {
                continue;
            }

            String result = licensePlate +
                    BLANK_SPACE +
                    vin +
                    BLANK_SPACE +
                    iccid +
                    BLANK_SPACE +
                    data.get("time") +
                    BLANK_SPACE +
                    data.get("mileage") +
                    BLANK_SPACE +
                    data.get("voltage") +
                    BLANK_SPACE +
                    data.get("voltageTime") +
                    BLANK_SPACE +
                    data.get("lon") +
                    BLANK_SPACE +
                    data.get("lat") +
                    "\n";
            System.out.print(result);
            IOUtils.write(result, fileOutputStream, "utf-8");
        }
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private static Map<String, String> getData(String vid) throws IOException {
        Table table = getTable("realinfo");
        Scan scan = new Scan();
        scan.setStartRow((vid + "_" + properties.getProperty("stop.row.timestamp")).getBytes());
        scan.setStopRow((vid + "_" + properties.getProperty("start.row.timestamp")).getBytes());
        scan.setMaxVersions(20);
        scan.setReversed(true);
        scan.setCaching(20);

        scan.addColumn("cf".getBytes(), "2000".getBytes());
        scan.addColumn("cf".getBytes(), "2202".getBytes());
        scan.addColumn("cf".getBytes(), "2613".getBytes());
        scan.addColumn("cf".getBytes(), "2502".getBytes());
        scan.addColumn("cf".getBytes(), "2503".getBytes());

        ResultScanner scanner = table.getScanner(scan);
        Map<String, String> map = new HashMap<>();
        for (Result result : scanner) {
            if (map.size() == 6) {
                break;
            }

            byte[] time = result.getValue("cf".getBytes(), "2000".getBytes());
            if (time != null && map.get("time") == null) {
                map.put("time", new String(time));
            }

            byte[] mileage = result.getValue("cf".getBytes(), "2202".getBytes());
            if (mileage != null && map.get("mileage") == null) {
                String s = new String(mileage);
                if (StringUtils.isNotBlank(s) && !"0".equals(s)) {
                    map.put("mileage", String.valueOf(Double.parseDouble(s) / 10));
                }
            }

            byte[] voltage = result.getValue("cf".getBytes(), "2613".getBytes());
            if (time != null && voltage != null && map.get("voltage") == null && map.get("voltageTime") == null) {
                String s = new String(voltage);
                if (StringUtils.isNotBlank(s)) {
                    map.put("voltage", String.valueOf(Double.parseDouble(s) / 10));
                    map.put("voltageTime", new String(time));
                }
            }

            byte[] lon = result.getValue("cf".getBytes(), "2502".getBytes());
            byte[] lat = result.getValue("cf".getBytes(), "2503".getBytes());

            boolean lonIsInvalid = lon == null || "0".equals(new String(lon));
            boolean latIsInvalid = lat == null || "0".equals(new String(lat));
            if (!lonIsInvalid && !latIsInvalid && map.get("lon") == null && map.get("lat") == null) {
                String lonString = new String(lon);
                String latString = new String(lat);
                if (!StringUtils.isAnyBlank(lonString, latString)) {
                    map.put("lon", String.valueOf(Double.parseDouble(lonString) / 1000000));
                    map.put("lat", String.valueOf(Double.parseDouble(latString) / 1000000));
                }
            }
        }
        return map;
    }
}
