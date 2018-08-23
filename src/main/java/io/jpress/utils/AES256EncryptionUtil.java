package io.jpress.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Security;

/**
 * Created by dong on 2017/8/7.
 */
public class AES256EncryptionUtil {
    public static boolean initialized = false;

    public static final String ALGORITHM = "AES/ECB/PKCS7Padding";

    /**
     * @param str String   要被加密的字符串
     * @param key String   加/解密要用的长度为32的字符串（256位字节）密钥
     * @return String  加密后的字符串
     */
    public static String Aes256Encode(String str, String key) {
        initialize();
        String result = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES"); //生成加密解密需要的Key
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] bytes = cipher.doFinal(str.getBytes("UTF-8"));
            result = new BASE64Encoder().encode(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param content String   要被解密的字符串
     * @param key     String     加/解密要用的长度为32的字符串（256位字节）密钥
     * @return String  解密后的字符串
     */
    public static String Aes256Decode(String content, String key) {
        initialize();
        String result = null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES"); //生成加密解密需要的Key
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] bytes = new BASE64Decoder().decodeBuffer(content);
            byte[] decoded = cipher.doFinal(bytes);
            result = new String(decoded, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void initialize() {
        if (initialized) return;
        Security.addProvider(new BouncyCastleProvider());
        initialized = true;
    }

    public static void main(String[] args) throws IOException {
        //商户key
        String key = "1b75caff90f978b5f1b2beb936fc540e";
        key = key.toLowerCase();
        String msg = "<root>\n" +
                "<out_refund_no><![CDATA[xx]]></out_refund_no>\n" +
                "<out_trade_no><![CDATA[xx]]></out_trade_no>\n" +
                "<refund_account><![CDATA[REFUND_SOURCE_RECHARGE_FUNDS]]></refund_account>\n" +
                "<refund_fee><![CDATA[1]]></refund_fee>\n" +
                "<refund_id><![CDATA[xx]]></refund_id>\n" +
                "<refund_recv_accout><![CDATA[用户零钱]]></refund_recv_accout>\n" +
                "<refund_request_source><![CDATA[API]]></refund_request_source>\n" +
                "<refund_status><![CDATA[SUCCESS]]></refund_status>\n" +
                "<settlement_refund_fee><![CDATA[1]]></settlement_refund_fee>\n" +
                "<settlement_total_fee><![CDATA[1]]></settlement_total_fee>\n" +
                "<success_time><![CDATA[2017-08-07 17:40:21]]></success_time>\n" +
                "<total_fee><![CDATA[1]]></total_fee>\n" +
                "<transaction_id><![CDATA[xxx]]></transaction_id>\n" +
                "</root>";

        String  enc = Aes256Encode(msg, key);
        System.out.printf("enc:%s\n",enc);

        String dec = Aes256Decode(enc, key);

        System.out.printf("dec:%s\n" , dec);
    }
}
