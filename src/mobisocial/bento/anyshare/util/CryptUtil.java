package mobisocial.bento.anyshare.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import mobisocial.bento.anyshare.ui.ViewActivity.PrepareForCorral;

import org.jivesoftware.smack.util.Base64;

import android.util.Log;

public class CryptUtil {

	int blocksize = 16;
    Cipher encCipher = null;
    Cipher decCipher = null;
    byte[] buf = new byte[blocksize];
    byte[] obuf = new byte[512];
    byte[] key = null;
    byte[] IV = null;
    String keystr = null;

    public CryptUtil() throws NoSuchAlgorithmException{
		KeyGenerator kgen = KeyGenerator.getInstance("AES");
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
	    kgen.init(128, sr);
	    SecretKey skey = kgen.generateKey();
	    key = skey.getEncoded();
	    keystr = Base64.encodeBytes(key);
        IV = new byte[blocksize];
    }
    
    public CryptUtil(String mykey){
        key = Base64.decode(mykey);
        IV = new byte[blocksize];
    }
    
    public String getKey(){
    	Log.e("KEY", keystr);
    	Log.e("HASH", ""+keystr.hashCode());
    	return keystr;
    }

    public void InitCiphers()
            throws NoSuchAlgorithmException,
            NoSuchProviderException,
            NoSuchProviderException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException{
       encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
       SecretKey keyValue = new SecretKeySpec(key,"AES");
       AlgorithmParameterSpec IVspec = new IvParameterSpec(IV);
       encCipher.init(Cipher.ENCRYPT_MODE, keyValue, IVspec);

       decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
       decCipher.init(Cipher.DECRYPT_MODE, keyValue, IVspec);
    }

    public void ResetCiphers()
    {
        encCipher=null;
        decCipher=null;
    }

    public void Encrypt(InputStream fis, OutputStream fos)
            throws IOException,
            ShortBufferException,
            IllegalBlockSizeException,
            BadPaddingException
    {
       byte[] buf = new byte[blocksize];
       int len = 0;
       byte[] cipherBlock = new byte[encCipher.getOutputSize(buf.length)];
       int cipherBytes;
       while((len = fis.read(buf))!=-1)
       {
           cipherBytes =
                   encCipher.update(buf, 0, len, cipherBlock);
           fos.write(cipherBlock, 0, cipherBytes);
       }
       cipherBytes = encCipher.doFinal(cipherBlock,0);
       fos.write(cipherBlock,0,cipherBytes);
       fos.close();
       fis.close();
    }
    
    public void Decrypt(InputStream fis, OutputStream fos, PrepareForCorral prg)
            throws IOException,
            ShortBufferException,
            IllegalBlockSizeException,
            BadPaddingException
    {
       byte[] buf = new byte[blocksize];
       int len = 0;
       byte[] cipherBlock = new byte[decCipher.getOutputSize(buf.length)];
       int cipherBytes;
       int size = 0;
       int thresh = (int) Math.floor(prg.filesize/50);
       int cnt = 0;
       while((len = fis.read(buf))!=-1)
       {
    	   // progress dialog update
           size += len;
           cnt += len;
           if(cnt>thresh){
        	   cnt -= thresh;
               prg.setProgress((int) Math.floor(50 * size/prg.filesize)+50);
           }

           cipherBytes = decCipher.update(buf, 0, len, cipherBlock);
           fos.write(cipherBlock, 0, cipherBytes);
       }
       cipherBytes = decCipher.doFinal(cipherBlock,0);
       fos.write(cipherBlock,0,cipherBytes);
       fos.close();
       fis.close();
    }
}