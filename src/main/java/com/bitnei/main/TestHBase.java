package com.bitnei.main;

import com.bitnei.core.util.PropertiesUtil;
import net.iharder.base64.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @Date 2018/4/23
 */
public class TestHBase {

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
            String[] vidVin = vin.split(",");

            FileWriter fileWriter = new FileWriter(properties.getProperty("result.file.path") + "/" + vidVin[1]);
            Table table = getTable("packet");
            Scan scan = new Scan();
            scan.setStartRow((vidVin[0] + "_" + properties.getProperty("start.row.timestamp")).getBytes());
            scan.setStopRow((vidVin[0] + "_" + properties.getProperty("stop.row.timestamp")).getBytes());

            scan.addColumn("cf".getBytes(), "stime".getBytes());
            scan.addColumn("cf".getBytes(), "data".getBytes());

            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                Cell[] cells = result.rawCells();

                StringBuilder res = new StringBuilder();
                for (Cell cell : cells) {
                    String qualifier = new String(CellUtil.cloneQualifier(cell));
                    byte[] value = CellUtil.cloneValue(cell);
                    if ("data".equals(qualifier)) {
                        String s = Base64.encodeBytes(value);
                        s = s.replaceAll("\n", "");
                        res.append(s);
                    } else {
                        res.append(new String(value));
                    }
                    res.append(",");
                }
                if (res.length() > 0) {
                    res.deleteCharAt(res.length() - 1);
                }
                IOUtils.write(res.toString() + "\n", fileWriter);
            }
            System.out.println(vin);
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
