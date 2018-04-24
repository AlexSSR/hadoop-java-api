package com.zhaogd.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: zhaogd
 * Date: 2017/9/20
 */
public class Test {

    public static void main(String[] args) {
        Date date = new Date();
        System.out.println(date);
        String s = JSON.toJSONString(date, SerializerFeature.UseISO8601DateFormat);
        System.out.println(s);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");

        System.out.println(simpleDateFormat.format(date));
    }
}
