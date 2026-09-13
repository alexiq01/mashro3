package com.example.subscriptions.crypto

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SasCrypto {
 private val key="abcdefghijuklmno0123456789012345".toByteArray(StandardCharsets.UTF_8)
 fun encrypt(plaintext:String):String { val salt=ByteArray(8).also{SecureRandom().nextBytes(it)}; val d=derive(salt); val c=Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.ENCRYPT_MODE,SecretKeySpec(d.copyOfRange(0,32),"AES"),IvParameterSpec(d.copyOfRange(32,48))); return Base64.encodeToString("Salted__".toByteArray()+salt+c.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8)),Base64.NO_WRAP) }
 fun decrypt(encoded:String):String { val raw=Base64.decode(encoded,Base64.DEFAULT); require(String(raw.copyOfRange(0,8))=="Salted__"); val d=derive(raw.copyOfRange(8,16)); val c=Cipher.getInstance("AES/CBC/PKCS5Padding"); c.init(Cipher.DECRYPT_MODE,SecretKeySpec(d.copyOfRange(0,32),"AES"),IvParameterSpec(d.copyOfRange(32,48))); return String(c.doFinal(raw.copyOfRange(16,raw.size)),StandardCharsets.UTF_8) }
 private fun derive(salt:ByteArray):ByteArray { val out=ByteArray(48); var prev=ByteArray(0); var n=0; while(n<48){ val md=MessageDigest.getInstance("MD5"); md.update(prev); md.update(key); md.update(salt); prev=md.digest(); val count=minOf(16,48-n); System.arraycopy(prev,0,out,n,count); n+=count }; return out }
}
