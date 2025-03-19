package dev.jkopecky.draftbook_backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.Log;
import dev.jkopecky.draftbook_backend.data.tables.Account;
import dev.jkopecky.draftbook_backend.data.tables.AccountRepository;
import dev.jkopecky.draftbook_backend.data.tables.AuthToken;
import dev.jkopecky.draftbook_backend.data.tables.AuthTokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.UUID;

@Controller
@CrossOrigin
public class AuthenticationController {



    AccountRepository accountRepository;
    AuthTokenRepository authTokenRepository;
    public AuthenticationController(AccountRepository accountRepository, AuthTokenRepository authTokenRepository) {
        this.accountRepository = accountRepository;
        this.authTokenRepository = authTokenRepository;
    }



    public static Account getByToken(String token, AuthTokenRepository authTokenRepository) throws Exception {
        for (AuthToken authToken : authTokenRepository.findAll()) {
            if (authToken.getValue().equals(token)) {
                //token found.
                return authToken.getAccount();
            }
        }
        //no matching token exists.
        throw new Exception("No matching token exists.");
    }



    @PostMapping("/api/auth/authenticate")
    public ResponseEntity<HashMap<String, Object>> authenticate(@RequestBody String data) {
        HashMap<String, Object> response = new HashMap<>();

        String username;
        String password;

        ObjectMapper mapper = new ObjectMapper();
        try { //read data from request
            JsonNode node = mapper.readTree(data);
            username = node.get("username").asText();
            password = node.get("password").asText();
        } catch (Exception e) {
            Log.create(e.getMessage(), "AuthenticationController.authenticate()", "info", e);
            response.put("error", "authenticate_parse");
            return new ResponseEntity<>(response, HttpStatus.valueOf(500));
        }

        //ensure account exists
        if (!Account.exists(username, accountRepository)) {
            Log.create("Attempted to access account " + username + ", but it does not exist.",
                    "AuthenticationController.authenticate()", "info", null);
            response.put("error", "account_nonexistent");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        //attempt sign on
        //todo secure authentication
        Account account = Account.authenticate(username, password, accountRepository);
        if (account != null) { //password was correct, successfully authenticated
            //delete the old token associated with this account, if any.
            for (AuthToken token : authTokenRepository.findAll()) {
                if (token.getAccount().getUsername().equals(account.getUsername())) {
                    authTokenRepository.delete(token);
                }
            }
            //create a new token for the account.
            AuthToken token = new AuthToken(account, authTokenRepository);

            HttpHeaders cookieHeaders = new HttpHeaders();
            String tokenCookie = "token=" + token.getValue() + "; Max-Age=3600;";
            cookieHeaders.add("Set-Cookie", tokenCookie);

            response.put("error", "none");
            response.put("authenticated", true);
            response.put("token", token.getValue());
            return new ResponseEntity<>(response, cookieHeaders, HttpStatus.OK);
        } else { //incorrect password, reject
            Log.create("Failed to authenticate account " + username + " with password " + password + ".",
                    "AuthenticationController.authenticate()", "info", null);
            response.put("error", "invalid_password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }



    @PostMapping("/api/auth/exists")
    public ResponseEntity<HashMap<String, Object>> exists(@RequestBody String data) {
        HashMap<String, Object> response = new HashMap<>();

        String username;

        ObjectMapper mapper = new ObjectMapper();
        try { //read data from request
            JsonNode node = mapper.readTree(data);
            username = node.get("username").asText();
        } catch (Exception e) {
            Log.create(e.getMessage(), "AuthenticationController.exists()", "info", e);
            response.put("error", "authenticate_parse");
            return new ResponseEntity<>(response, HttpStatus.valueOf(500));
        }

        response.put("exists", Account.exists(username, accountRepository));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    @PostMapping("/api/auth/create")
    public ResponseEntity<HashMap<String, Object>> create(@RequestBody String data) {
        HashMap<String, Object> response = new HashMap<>();

        String username;
        String password;

        ObjectMapper mapper = new ObjectMapper();
        try { //read data from request
            JsonNode node = mapper.readTree(data);
            username = node.get("username").asText();
            password = node.get("password").asText();
        } catch (Exception e) {
            Log.create(e.getMessage(), "AuthenticationController.exists()", "info", e);
            response.put("error", "authenticate_parse");
            return new ResponseEntity<>(response, HttpStatus.valueOf(500));
        }

        //make sure the account doesn't already exist
        if (Account.exists(username, accountRepository)) {
            Log.create("Attempted to create account, but the username " + username + " already exists.",
                    "AuthenticationController.create()", "info", null);
            response.put("error", "username_taken");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        //create account
        Account account = Account.create(username, password, accountRepository);
        AuthToken token = new AuthToken(account, authTokenRepository);

        //reply
        response.put("error", "none");
        response.put("authenticated", true);
        response.put("token", token.getValue());
        HttpHeaders cookieHeaders = new HttpHeaders();
        String tokenCookie = "token=" + token.getValue() + "; Max-Age=3600;";
        cookieHeaders.add("Set-Cookie", tokenCookie);
        return new ResponseEntity<>(response, cookieHeaders, HttpStatus.OK);
    }
}
