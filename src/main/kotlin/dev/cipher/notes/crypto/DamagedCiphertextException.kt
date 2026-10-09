package dev.cipher.notes.crypto

/**
 * Thrown when a stored ciphertext cannot be interpreted at all: not valid
 * Base64, or too short to contain the salt and IV that precede the payload.
 *
 * This is distinct from [javax.crypto.AEADBadTagException], which is raised for
 * both a wrong password and altered bytes and so cannot be treated as damage.
 */
class DamagedCiphertextException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
