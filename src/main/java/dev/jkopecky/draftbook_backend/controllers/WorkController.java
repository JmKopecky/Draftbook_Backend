package dev.jkopecky.draftbook_backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.Log;
import dev.jkopecky.draftbook_backend.data.Util;
import dev.jkopecky.draftbook_backend.data.tables.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class WorkController {

    //all the database repositories load here, add a new entry if another table is needed for this controller
    AccountRepository accountRepository;
    WorkRepository workRepository;
    ChapterRepository chapterRepository;
    NoteCategoryRepository noteCategoryRepository;
    public WorkController(AccountRepository accountRepository, WorkRepository workRepository, ChapterRepository chapterRepository, NoteCategoryRepository noteCategoryRepository) {
        this.accountRepository = accountRepository;
        this.workRepository = workRepository;
        this.chapterRepository = chapterRepository;
        this.noteCategoryRepository = noteCategoryRepository;
    }



    //note: work wide api calls


    private Object confirmAuth(String username, String password) {
        Account account;
        //ensure user account identified by cookies exists
        if (!Account.exists(username, accountRepository)) {
            account = Account.authenticate(username, password, accountRepository);
        } else {
            Log.create("Attempted to access account with an unrecognized username",
                    "WorkController.getAccount()", "info", null);
            return "unrecognized_username";
        }

        //ensure user authentication succeeds
        if (account == null) {
            Log.create("Password does not match username",
                    "WorkController.getWork()", "info", null);
            return "incorrect_password";
        }

        return account;
    }



    private Object getTargetWork(String target, Account account) {
        //retrieve work
        ArrayList<Work> works = account.getOwnedWorks(workRepository);
        for (Work work : works) {
            if (work.toResource().equals(target)) {
                //found the work
                return work;
            }
        }

        //if this point is reached, the work was not found in the user's list of works
        Log.create("Failed to find work " + target + " in account work list.",
                "WorkController.getWork()", "info", null);
        return "unrecognized_work";
    }



    public ArrayList<Object> getAccountAndWork(String username, String password, String target) {
        ArrayList<Object> output = new ArrayList<>();
        String error = "none";

        //confirm user credentials
        Account account;
        Object authResult = confirmAuth(username, password);
        if (authResult instanceof Account) {
            account = (Account) authResult;
        } else {
            error = "" + authResult;
            output.add(error);
            return output;
        }

        //retrieve work
        Work work;
        Object workResult = getTargetWork(target, account);
        if (workResult instanceof Work w) {
            work = w;
        } else {
            error = "" + workResult;
            output.add(error);
            return output;
        }
        output.add(account);
        output.add(work);
        output.add(error);
        return output;
    }



    @GetMapping("/api/works/work")
    public ResponseEntity<HashMap<String, Object>> getWork(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "username", defaultValue = "null") String username,
            @CookieValue(value = "password", defaultValue = "null") String password) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;

        //confirm user credentials
        Object authResult = confirmAuth(username, password);
        if (authResult instanceof Account) {
            account = (Account) authResult;
        } else {
            response.put("error", authResult);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        //retrieve work
        Object workResult = getTargetWork(target, account);
        if (workResult instanceof Work work) {
            response.put("error", "none");
            response.put("work", work); //todo test that this works correctly, sending the work object in json format to the client
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", workResult);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }



    @GetMapping("/api/works/chapters")
    public ResponseEntity<HashMap<String, Object>> getChapters(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "username", defaultValue = "null") String username,
            @CookieValue(value = "password", defaultValue = "null") String password) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;

        //confirm user credentials
        Object authResult = confirmAuth(username, password);
        if (authResult instanceof Account) {
            account = (Account) authResult;
        } else {
            response.put("error", authResult);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        //retrieve work
        Object workResult = getTargetWork(target, account);
        if (workResult instanceof Work work) {
            response.put("error", "none");
            response.put("chapters", work.getChapters(chapterRepository)); //todo test that this works correctly
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", workResult);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }



    @GetMapping("/api/works/notecategories")
    public ResponseEntity<HashMap<String, Object>> getNotes(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "username", defaultValue = "null") String username,
            @CookieValue(value = "password", defaultValue = "null") String password) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;

        //confirm user credentials
        Object authResult = confirmAuth(username, password);
        if (authResult instanceof Account) {
            account = (Account) authResult;
        } else {
            response.put("error", authResult);
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        //retrieve work
        Object workResult = getTargetWork(target, account);
        if (workResult instanceof Work work) {
            ArrayList<NoteCategory> noteCategories = new ArrayList<>();
            for (NoteCategory nc : noteCategoryRepository.findAll()) {
                if (nc.getWork().equals(work)) {
                    noteCategories.add(nc);
                }
            }

            //retrieve chapters and reply
            response.put("error", "none");
            response.put("notecategories", noteCategories); //todo test that this works correctly
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", workResult);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }




    @GetMapping("/api/works/rename")
    public ResponseEntity<HashMap<String, Object>> renameWork(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "username", defaultValue = "null") String username,
            @CookieValue(value = "password", defaultValue = "null") String password,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;
        ArrayList<Object> container = getAccountAndWork(username, password, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }

        //retrieve response data
        String newName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            newName = node.get("newName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "WorkController.renameWork()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        boolean result = work.changeName(newName, workRepository);
        if (result) {
            response.put("error", "none");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", "rename_failed");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }





    @GetMapping("/api/works/delete")
    public ResponseEntity<HashMap<String, Object>> deleteWork(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "username", defaultValue = "null") String username,
            @CookieValue(value = "password", defaultValue = "null") String password) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;
        ArrayList<Object> container = getAccountAndWork(username, password, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }

        boolean result = work.delete(workRepository);
        if (result) {
            response.put("error", "none");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("error", "delete_failed");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
