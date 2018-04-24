package com.bitnei.main;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author zhaogd
 * @Date 2018/2/23
 */
public class TestHadoop {

    private static void downLoadFile() throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://192.168.2.70:8020");
        FileSystem fs = FileSystem.get(conf);
        Path hdfs = new Path("/tmp/hbase/operationIndexDetail");
//        Path win7 = new Path("F:/");
//        fs.copyToLocalFile(hdfs, win7);
        boolean exists = fs.exists(hdfs);
        System.out.println(exists);
    }


    public static void main(String[] args) throws IOException {
        downLoadFile();
    }
}
