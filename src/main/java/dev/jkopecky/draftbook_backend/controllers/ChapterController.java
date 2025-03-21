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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
@CrossOrigin
public class ChapterController {


    //all the database repositories load here, add a new entry if another table is needed for this controller
    AccountRepository accountRepository;
    WorkRepository workRepository;
    ChapterRepository chapterRepository;
    NoteCategoryRepository noteCategoryRepository;
    AuthTokenRepository authTokenRepository;
    public ChapterController(AccountRepository accountRepository, WorkRepository workRepository, ChapterRepository chapterRepository, NoteCategoryRepository noteCategoryRepository, AuthTokenRepository authTokenRepository) {
        this.accountRepository = accountRepository;
        this.workRepository = workRepository;
        this.chapterRepository = chapterRepository;
        this.noteCategoryRepository = noteCategoryRepository;
        this.authTokenRepository = authTokenRepository;
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



    public ArrayList<Object> getAccountAndWork(String token, String target) {
        ArrayList<Object> output = new ArrayList<>();
        String error = "none";

        //confirm user credentials
        Account account;
        try {
            account = AuthenticationController.getByToken(token, authTokenRepository);
        } catch (Exception e) {
            //failed to retrieve account;
            error = "Failed to match auth token to account";
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







    //note: inputs {chaptername, chapternumber}
    @PostMapping("/api/works/chapters/create")
    public ResponseEntity<HashMap<String, Object>> createChapter(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "token", defaultValue = "null") String token,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account = null;
        Work work;

        //retrieve response data
        String chapterName;
        int chapterNumber;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            chapterName = node.get("chaptername").asText();
            chapterNumber = Integer.parseInt(node.get("chapternumber").asText());
            //token verification if included in request body
            if (node.has("token")) {
                token = node.get("token").asText();
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.createChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        //auth
        ArrayList<Object> container = getAccountAndWork(token, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1); //todo see if this can be adapted to the above get mappings to reduce boilerplate.
        }


        //create chapter
        try {
            work.createChapter(chapterName, chapterNumber, chapterRepository);
        } catch (IOException e) {
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("error", "none");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    //note: inputs {chaptername}
    @PostMapping("/api/works/chapters/select")
    public ResponseEntity<HashMap<String, Object>> selectChapter(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "token", defaultValue = "null") String token,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;

        //retrieve response data
        String chapterTarget;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            chapterTarget = node.get("chaptername").asText();
            //token verification if included in request body
            if (node.has("token")) {
                token = node.get("token").asText();
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.selectChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        //auth
        ArrayList<Object> container = getAccountAndWork(token, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }


        try {
            for (Chapter c : work.getChapters(chapterRepository)) {
                if (c.toResource().equals(chapterTarget)) {
                    response.put("content", c.retrieveAsHTML());
                    response.put("notes", c.readNotes());
                    response.put("title", c.getTitle());
                    response.put("error", "none");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                }
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.selectChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("error", "unrecognized_chapter");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    //note: inputs {chaptername}
    @PostMapping("/api/works/chapters/rename")
    public ResponseEntity<HashMap<String, Object>> renameChapter(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "token", defaultValue = "null") String token,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;

        //retrieve response data
        String chapterTarget;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            chapterTarget = node.get("chaptername").asText();
            //token verification if included in request body
            if (node.has("token")) {
                token = node.get("token").asText();
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.selectChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        //auth
        ArrayList<Object> container = getAccountAndWork(token, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }

        return null; //todo implement
    }



    //note: inputs {chaptername, content, notes}
    @PostMapping("/api/works/chapters/save")
    public ResponseEntity<HashMap<String, Object>> saveChapter(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "token", defaultValue = "null") String token,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;

        //retrieve response data
        String chapterTitle;
        String content;
        String notes;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            chapterTitle = node.get("chaptername").asText();
            content = node.get("content").asText();
            notes = node.get("notes").asText();
            //token verification if included in request body
            if (node.has("token")) {
                token = node.get("token").asText();
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.saveChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        //auth
        ArrayList<Object> container = getAccountAndWork(token, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }


        //save chapter
        try {
            Chapter chapter = null;
            for (Chapter c : work.getChapters(chapterRepository)) {
                if (c.getTitle().equals(chapterTitle)) {
                    chapter = c;
                }
            }
            if (chapter != null) {
                chapter.writeHTML(content);
                chapter.writeNotes(notes);
                response.put("error", "none");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                Log.create("Unrecognized chapter: " + chapterTitle, "ChapterController.saveChapter()", "info", null);
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.saveChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Log.create("Failed to save chapter: " + chapterTitle, "ChapterController.saveChapter()", "error", null);
        response.put("error", "Failed to save chapter");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    //note: inputs {chaptername}
    @PostMapping("/api/works/chapters/delete")
    public ResponseEntity<HashMap<String, Object>> deleteChapter(
            @RequestParam(name = "target", required = true) String target,
            @CookieValue(value = "token", defaultValue = "null") String token,
            @RequestBody String data) {

        HashMap<String, Object> response = new HashMap<>();

        Account account;
        Work work;

        //retrieve response data
        String chapterTarget;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            chapterTarget = node.get("chaptername").asText();
            //token verification if included in request body
            if (node.has("token")) {
                token = node.get("token").asText();
            }
        } catch (IOException e) {
            Log.create(e.getMessage(), "ChapterController.deleteChapter()", "error", e);
            response.put("error", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        //auth
        ArrayList<Object> container = getAccountAndWork(token, target);
        if (container.size() == 1) {
            response.put("error", container.get(0));
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            account = (Account) container.get(0);
            work = (Work) container.get(1);
        }


        return null; //todo implement
    }
}
