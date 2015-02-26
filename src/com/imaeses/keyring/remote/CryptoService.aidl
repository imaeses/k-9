package com.imaeses.keyring.remote;

import com.imaeses.keyring.remote.DecryptResponse;

interface CryptoService {

	DecryptResponse decrypt(String msg, String charset);
	DecryptResponse decryptWithPassword(String sourceFile, String destFile, String password);
	DecryptResponse decryptFile(String sourceFile, String destFile);
	DecryptResponse decryptFileWithPassword(String sourceFile, String destFile, String password);
	DecryptResponse verify(String sourceFile, String signature);
	
}