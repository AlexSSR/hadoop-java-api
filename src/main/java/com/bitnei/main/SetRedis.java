package com.bitnei.main;

import com.ctfo.datacenter.cache.exception.DataCenterException;
import com.ctfo.datacenter.cache.handle.CTFOCacheDB;
import com.ctfo.datacenter.cache.handle.CTFOCacheTable;
import com.ctfo.datacenter.cache.handle.CTFODBManager;
import com.ctfo.datacenter.cache.handle.DataCenter;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created on 2018/8/28.
 *
 * @author zhaogd
 */
public class SetRedis {
    /**
     * ctforedis集群管理对象
     */
    private static CTFODBManager ctfoDBManager = null;
    private static CTFOCacheDB ctfoCacheDB = null;
    private static CTFOCacheTable ctfoCacheTable = null;

    static {
        try {
            ctfoDBManager = DataCenter.newCTFOInstance("cache", "10.128.1.4:6379");
            ctfoCacheDB = ctfoDBManager.openCacheDB("xny");
            ctfoCacheTable = ctfoCacheDB.getTable("realInfo");
        } catch (DataCenterException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException, DataCenterException {
        List<String> vehInfos = IOUtils.readLines(new FileInputStream("res.txt"), "utf-8");
        for (String vehInfo : vehInfos) {
            final String[] split = vehInfo.split("\t");
            String vid = split[1];
            String mileage = split[3];

//            ctfoCacheTable.addHash(vid,"2202",mileage);

            final String stringStringMap = ctfoCacheTable.queryHash(vid, "2202");
            System.out.println(split[0] + "," + vid + "," + stringStringMap);
        }

    }
}
