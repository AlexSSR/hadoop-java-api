package com.zhaogd.test;

import org.apache.commons.codec.binary.Base64;

/**
 * Created by IntelliJ IDEA.
 * User: zhaogd
 * Date: 2017/9/20
 */
public class Test {

    public static void main(String[] args) {
//        Date date = new Date();
//        System.out.println(date);
//        String s = JSON.toJSONString(date, SerializerFeature.UseISO8601DateFormat);
//        System.out.println(s);
//
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
//
//        System.out.println(simpleDateFormat.format(date));

        String s = "IyMB/kxHSEM0VjFENUhFMjAxMDIwAQAeEgENCAAVAAg4OTg2MDExNzc5NTkxMDE0ODkyOQEAwA==";
        String s1 = convert16HexStr(Base64.decodeBase64(s));
        System.out.println(s1);
    }

    /**
     * byte数组转换为十六进制的字符串
     * **/
    public static String convert16HexStr(byte [] b)
    {
        StringBuilder result = new StringBuilder();
        for (byte aB : b) {
            if ((aB & 0xff) < 0x10)
                result.append("0");
            result.append(Long.toString(aB & 0xff, 16));
        }
        return result.toString().toUpperCase();
    }
}
