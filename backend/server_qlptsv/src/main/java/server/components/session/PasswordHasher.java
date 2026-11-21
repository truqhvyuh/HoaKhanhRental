package server.components.session;

import org.mindrot.jbcrypt.BCrypt;
import server.components.errors.InvalidInputException;

import java.util.ArrayList;
import java.util.List;

public final class PasswordHasher {
    private static final String SYMBOLS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    public String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean matches(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception exception) {
            return false;
        }
    }

    public void validate(String password, String loginName, String email) {
        List<String> failed = new ArrayList<>();
        if (password == null || password.length() < 8 || password.length() > 72) {
            failed.add("PASSWORD_LENGTH");
        }
        boolean ascii = password != null && password.chars().allMatch(ch -> ch >= 33 && ch <= 126);
        if (!ascii) failed.add("PASSWORD_ASCII");
        if (password == null || !password.chars().anyMatch(Character::isUpperCase)) failed.add("PASSWORD_UPPERCASE");
        if (password == null || !password.chars().anyMatch(Character::isLowerCase)) failed.add("PASSWORD_LOWERCASE");
        if (password == null || !password.chars().anyMatch(Character::isDigit)) failed.add("PASSWORD_DIGIT");
        if (password == null || password.chars().noneMatch(ch -> SYMBOLS.indexOf(ch) >= 0)) failed.add("PASSWORD_SYMBOL");
        if (password != null && ((loginName != null && password.equals(loginName)) || (email != null && password.equals(email)))) {
            failed.add("PASSWORD_MATCHES_LOGIN");
        }
        if (!failed.isEmpty()) {
            throw new InvalidInputException("INVALID_PASSWORD", "Password policy failed.", failed);
        }
    }
}
