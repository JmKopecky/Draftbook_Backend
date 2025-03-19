package dev.jkopecky.draftbook_backend.data.tables;

import dev.jkopecky.draftbook_backend.Log;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class AuthToken {


    public static final int PERMITTED_ATTEMPTS_TO_GENERATE_TOKEN = 10;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @ManyToOne
    private Account account;
    private String value;


    public AuthToken() {} //default constructor required


    public AuthToken(Account account, AuthTokenRepository authTokenRepository) {
        this.account = account;
        try {
            genUniqueToken(authTokenRepository);
        } catch (Exception e) {
            Log.create("Error encountered generating an auth token.", "AuthToken.AuthToken()", "error", e);
            value = "ERROR";
        }

        authTokenRepository.save(this);
    }


    public void genUniqueToken(AuthTokenRepository authTokenRepository) throws Exception {
        for (int i = 0; i < PERMITTED_ATTEMPTS_TO_GENERATE_TOKEN; i++) {
            String potentialValue = UUID.randomUUID().toString();
            boolean unique = true;
            for (AuthToken token : authTokenRepository.findAll()) { //note potential optimization possible? learn sql and replace these.
                if (token.value.equals(potentialValue)) {
                    unique = false;
                    break;
                }
            }
            if (!unique) {
                continue;
            }
            this.value = potentialValue;
            return;
        }

        throw new Exception("Failed to generate a unique token.");
    }


    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
