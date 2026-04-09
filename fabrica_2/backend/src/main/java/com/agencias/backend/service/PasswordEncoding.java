package com.agencias.backend.service;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Hash y verificación BCrypt centralizados para usuarios. */
public final class PasswordEncoding {

    private static final int BCRYPT_COST = 12;

    private PasswordEncoding() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
    }

    public static boolean verify(String plainPassword, String hash) {
        if (plainPassword == null || hash == null) {
            return false;
        }
        return BCrypt.verifyer().verify(plainPassword.toCharArray(), hash).verified;
    }
}
