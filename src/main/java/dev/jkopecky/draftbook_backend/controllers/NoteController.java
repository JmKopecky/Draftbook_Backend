package dev.jkopecky.draftbook_backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.Log;
import dev.jkopecky.draftbook_backend.data.Util;
import dev.jkopecky.draftbook_backend.data.tables.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
@CrossOrigin
public class NoteController {



    //all the database repositories load here, add a new entry if another table is needed for this controller
    AccountRepository accountRepository;
    WorkRepository workRepository;
    ChapterRepository chapterRepository;
    NoteCategoryRepository noteCategoryRepository;
    public NoteController(AccountRepository accountRepository, WorkRepository workRepository, ChapterRepository chapterRepository, NoteCategoryRepository noteCategoryRepository) {
        this.accountRepository = accountRepository;
        this.workRepository = workRepository;
        this.chapterRepository = chapterRepository;
        this.noteCategoryRepository = noteCategoryRepository;
    }




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




    //note: inputs {noteCategoryName}
    @PostMapping("/api/works/notecategories/create")
    public ResponseEntity<HashMap<String, Object>> createCategory(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();

        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.createCategory()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            NoteCategory.create(noteCategoryRepository, work, noteCategoryName);
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.createCategory()", "error", e);
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName}
    @PostMapping("/api/works/notecategories/rename")
    public ResponseEntity<HashMap<String, Object>> renameCategory(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();

        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.renameCategory()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        //todo implement

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName}
    @PostMapping("/api/works/notecategories/delete")
    public ResponseEntity<HashMap<String, Object>> deleteCategory(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();

        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.deleteCategory()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        //find the noteCategory and delete it
        for (NoteCategory cat : NoteCategory.getWorkNoteCategories(work, noteCategoryRepository)) {
            if (cat.getCategoryName().equals(noteCategoryName)) {
                //delete
                for (String s : cat.getNotes()) {
                    try {
                        cat.deleteNote(s, noteCategoryRepository);
                    } catch (IOException e) {
                        Log.create("Failed to delete note: " + s, "NoteController.deleteCategory()", "error", e);
                        response.put("error", e.getMessage());
                        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                }
                File noteDir = new File(cat.findPath() + "/");
                noteDir.delete();
                File catRoot = new File(cat.findPath() + ".json");
                catRoot.delete();
                noteCategoryRepository.delete(cat);
            }
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }





    // notes



    //note: inputs {noteCategoryName, noteName}
    @PostMapping("/api/works/notes/create")
    public ResponseEntity<HashMap<String, Object>> createNote(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        String noteName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();
            noteName = node.get("noteName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.createNote()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        for (NoteCategory cat : NoteCategory.getWorkNoteCategories(work, noteCategoryRepository)) {
            if (cat.getCategoryName().equals(noteCategoryName)) {
                try {
                    cat.addNote(noteName, "", noteCategoryRepository);
                    noteCategoryRepository.save(cat);
                } catch (IOException e) {
                    Log.create(e.getMessage(), "NoteController.createNote()", "error", e);
                    response.put("error", e.getMessage());
                    return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                }

            }
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName, noteName}
    @PostMapping("/api/works/notes/select")
    public ResponseEntity<HashMap<String, Object>> selectNote(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        String noteName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();
            noteName = node.get("noteName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.selectNote()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        //todo implement

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName, noteName, newNoteName}
    @PostMapping("/api/works/notes/rename")
    public ResponseEntity<HashMap<String, Object>> renameNote(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        String noteName;
        String newNoteName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();
            noteName = node.get("noteName").asText();
            newNoteName = node.get("newNoteName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.renameNote()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        for (NoteCategory cat : NoteCategory.getWorkNoteCategories(work, noteCategoryRepository)) {
            if (cat.getCategoryName().equals(noteCategoryName)) {
                if (cat.getNotes().contains(noteName)) {
                    try {
                        cat.renameNote(noteName, newNoteName, noteCategoryRepository);
                    } catch (FileAlreadyExistsException e) {
                        response.put("error", "file_already_exists");
                        Log.create("Attempted to edit note name, but the filename already is being used.", "NoteController.renameNote()", "info", null);
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    } catch (FileNotFoundException e) {
                        response.put("error", "unrecognized_note");
                        Log.create("Attempted to rename note that does not exist.", "NoteController.renameNote()", "info", null);
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName, noteName}
    @PostMapping("/api/works/notes/save")
    public ResponseEntity<HashMap<String, Object>> saveNote(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        String noteName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();
            noteName = node.get("noteName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.saveNote()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        //todo implement

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    //note: inputs {noteCategoryName, noteName}
    @PostMapping("/api/works/notes/delete")
    public ResponseEntity<HashMap<String, Object>> deleteNote(
            @RequestParam(name = "target") String target,
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
        String noteCategoryName;
        String noteName;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            noteCategoryName = node.get("noteCategoryName").asText();
            noteName = node.get("noteName").asText();
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteController.deleteNote()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        for (NoteCategory cat : NoteCategory.getWorkNoteCategories(work, noteCategoryRepository)) {
            if (cat.getCategoryName().equals(noteCategoryName)) {
                if (cat.getNotes().contains(noteName)) {
                    try {
                        cat.deleteNote(noteName, noteCategoryRepository);
                    } catch (FileNotFoundException e) {
                        Log.create("Attempted to delete note " + noteName + ", but it does not exist", "NoteController.deleteNote()", "info", null);
                        response.put("error", "note_does_not_exist");
                        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                    } catch (Exception e) {
                        Log.create(e.getMessage(), "NoteController.deleteNote()", "error", e);
                        response.put("error", e.getMessage());
                        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            }
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
