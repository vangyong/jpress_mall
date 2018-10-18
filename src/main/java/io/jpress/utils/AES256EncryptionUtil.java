package io.jpress.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import io.jpress.wechat.utils.MD5Util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Security;

/**
 * <b>Description:</b>
 * <br><b>ClassName:</b> AES256EncryptionUtil
 * <br><b>Date:</b> 2018年8月23日 下午5:20:12
 * <br>@author <b>jianb.jiang</b>
 */
public class AES256EncryptionUtil {

    public static void main(String[] args) throws IOException {
        //商户key
        String key = "fsfsafad";
        String msg = "9yyDBycNPn5AZki1smWGfu40/kJOg7DeuyFDSppYiGTgKPb4SlohDpma17EjMQq7PtUr75vIihQUCD5JB5fEtiFlB/KQ+TY6mnwujLmlCNgb3cT+24HZysarfdNkXnCwy94WPGbyrRbyp01NZsiAc5aasyMUBNT6qaGYs8sEan9iRGcgmt/7S2B3gmqiVBZ/1DQ2hA6fWJgGVoup1+vpMAoHU/jdj10E69zrkVbmljaZin2k+xXTZn8sXM5dZ3sTr1lUsXMF2j27BSL2MRwYEONO1E4iZEG5hyrY/ZheTneTVRLxNLEnyHFBfBFeB0ty1bi/EgAoDgE7z2pqSh4OlIqcn3Q3qHjJZkSoLC/C0wrjsg4kVcRbb9T/Hp2QvHIjM/BnjBB7TfnMdYub+XRFSkQvptr8OuTtc/kEQ4vFCayLphY59K2j0Fxi7wV96vJk3Hr3MCAs3DbSzGDQkfzB/WXHng85SDZ1tclXgk/5Z7vNAw+d+0JGbqxdQgRfEIgW+4eZR/VJvwGGCfqPAAAkW6z2EUbz9W0rFu0iyx75huv2UMnZMt9EEWZunJcWmIEcI/K32ZvCDfxqdD1xBp6KHDHjxo4pKGTdGKb+xwmloxDz1XT1EkDd9aClUhzKewArEWjWL1h/chqdYkAEk4fzWzNzOVp6CQ0XjlGdxC8IOFBLNEwcl2AXGy8u6eNg4ec/Rk9TS3zbHihoBtLUVpcM9BuklE49oOs66c6z+PhMhyCBXq+lWFKEyj/KvC+237CMj5RPcYLxf2TwQ33NvKzDJDhtOFRh5JPK4BG1Jd+el+NNoRtqr31j3RJKFrtMSg20hN0pkEPWTG5JqZAYXc/cryGrk8LSsJvlvDlzaPX+negfSOa3ryCpQ6kplaxdBXhX/PU3NPlLRm+MDF8gsuFF6YJW+0yYSJ4tI3jm+DROFs5u+hGsx87INmeJqYULABiXxf8HjzDOSp6AGCOXkUdFSqk36EfPp4PPHWgA6eMf8pHDYswp3P/58n55UTsrjo4KK9pBwMxw7ifYKD4qVx53YlTstUEGnE30KQdfQJ8iDic=";
        
        try {
            String aaa = decryptData(msg,key);
            System.out.println(aaa);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    static {
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String decryptData(String content, String key) throws Exception {
        byte[] keyB = MD5Util.getMD5Lower(key).getBytes();
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
        SecretKeySpec keys = new SecretKeySpec(keyB, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keys);
        byte[] b = org.bouncycastle.util.encoders.Base64.decode(content);
        return new String(cipher.doFinal(b));
    }
    
}
