package com.imaeses.keyring.remote;

import com.imaeses.keyring.remote.DecryptResponse;
import com.imaeses.keyring.remote.EncryptResponse;
import com.imaeses.keyring.remote.ImportResponse;

interface CryptoService {

	DecryptResponse decrypt(in String msg, in String charset, in boolean isSigned);
	DecryptResponse decryptWithPassword(in String msg, in String charset, in String password);
	DecryptResponse decryptFile(in String sourceFile, in String destFile);
	DecryptResponse decryptFileWithPassword(in String sourceFile, in String destFile, in String password);
	DecryptResponse verify(in String sourceFile, in String signature);
	
	EncryptResponse sign(in String sourceFile, in long keyId, in String sigAlg);
	EncryptResponse signWithPassword(in String sourceFile, in long keyId, in String sigAlg, in String password);
	EncryptResponse encrypt(in String msg, in long[] encKeyIds, in long sigKeyId, in String sigAlg);
	EncryptResponse encryptWithPassword(in String msg, in long[] encKeyIds, in long sigKeyId, in String sigAlg, in String password);
	EncryptResponse encryptFile(in String sourceFile, in long[] encKeyIds, in long sigKeyId, in String sigAlg);
	EncryptResponse encryptFileWithPassword(in String sourceFile, in long[] encKeyIds, in long sigKeyId, in String sigAlg, in String password);
	
	ImportResponse importCertificate(in String filename);
	ImportResponse generateKeyring(in String userId, in String algMaster, in boolean masterCanSign, in boolean masterCanEncrypt, in long masterExpiry, in String algSubkey, in boolean subkeyCanSign, in boolean subkeyCanEncrypt, in long subkeyExpiry, in String password);

	int addUserId(in long keyId, in String userId, in String password);
	int addSubkey(in long masterKeyId, in String algSubkey, in boolean canSign, in boolean canEncrypt, in long subkeyExpiry, in String password);
	int changeKeyExpiration(in long keyId, in long expiry, in String password);
	int revokeKey(in long keyId, in boolean keyCompromised, in String password);
	int certify(in long certifyingKeyId, in long certifiedKeyId, in String password);
	int revokeCertification(in long certifyingKeyId, in long certifiedKeyId, in String password);
	int exportToKeyserver(in long keyId);
		
	long[] getSecretKeyIdsFromEmail(in String email);
	long[] getPublicKeyIdsFromEmail(in String email);
	String getUserId(in long keyId);
	
	long getOpenpgpSmartcardSigningKeyId();

}