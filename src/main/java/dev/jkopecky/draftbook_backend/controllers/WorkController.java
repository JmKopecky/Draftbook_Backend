package dev.jkopecky.draftbook_backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.writerwebtools.data.Util;
import dev.jkopecky.writerwebtools.data.tables.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class WorkController {

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

    @GetMapping("/work")
    public String work(Model model, @RequestParam(name = "target", required = true) String target, @CookieValue(value = "account", defaultValue = "null") String account) {

        if (!Account.exists(account, accountRepository)) { //todo improve signon
            //return "redirect:/signon?mode=signin";
        }

        Work work = null;

        for (Work w : workRepository.findAll()) {
            if (Util.toInternalResource(w.getTitle()).equals(target)) {
                work = w;
            }
        }

        if (work == null) {
            return "redirect:/error";
        }

        ArrayList<NoteCategory> noteCategories = new ArrayList<>();
        for (NoteCategory nc : noteCategoryRepository.findAll()) {
            if (nc.getWork().equals(work)) {
                noteCategories.add(nc);
                System.out.println(nc.getNotes());
            }
        }

        model.addAttribute("work", work);
        model.addAttribute("chapters", Chapter.associatedWith(work, chapterRepository));
        model.addAttribute("notes", noteCategories);
        return "work";
    }


    @PostMapping("/work")
    public ResponseEntity<HashMap<String, Object>> work(Model model, @RequestParam(name = "target", required = true) String target, @CookieValue(value = "account", defaultValue = "null") String account, @RequestBody String data) {
        if (!Account.exists(account, accountRepository)) { //todo improve signon
            //return "redirect:/signon?mode=signin";
        }

        HashMap<String, Object> output = new HashMap<>();
        output.put("error", "none");

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            String mode = node.get("mode").asText();

            Work work = null;
            for (Work w : workRepository.findAll()) {
                if (Util.toInternalResource(w.getTitle()).equals(target)) {
                    work = w;
                }
            }
            if (mode.equals("create")) {
                String chname = node.get("chaptername").asText();
                String chnum = node.get("chapternumber").asText();
                work.createChapter(chname, Integer.parseInt(chnum), chapterRepository);
                return new ResponseEntity<>(output, HttpStatus.OK);
            }
            if (mode.equals("create_notecategory")) {
                String catname = node.get("name").asText();
                NoteCategory.create(noteCategoryRepository, work, catname);
                return new ResponseEntity<>(output, HttpStatus.OK);
            }
            if (mode.equals("create_note")) {
                String notename = node.get("name").asText();
                String category = node.get("category").asText();
                for (NoteCategory cat : NoteCategory.associatedWith(work, noteCategoryRepository)) {
                    if (cat.getCategoryName().equals(category)) {
                        cat.addNote(notename, "", noteCategoryRepository);
                        noteCategoryRepository.save(cat);
                    }
                }
                return new ResponseEntity<>(output, HttpStatus.OK);
            }
            if (mode.equals("edit_note_name")) {
                String newNote = node.get("new").asText();
                String oldNote = node.get("old").asText();
                String category = node.get("category").asText();
                for (NoteCategory cat : NoteCategory.associatedWith(work, noteCategoryRepository)) {
                    if (cat.getCategoryName().equals(category)) {
                        if (cat.getNotes().contains(oldNote)) {
                            try {
                                cat.renameNote(oldNote, newNote, noteCategoryRepository);
                            } catch (FileAlreadyExistsException e) {
                                output.put("error", "file_already_exists");
                            }
                            return new ResponseEntity<>(output, HttpStatus.OK);
                        }
                    }
                }
            }
            if (mode.equals("delete_note")) {
                String note = node.get("target").asText();
                String category = node.get("category").asText();
                for (NoteCategory cat : NoteCategory.associatedWith(work, noteCategoryRepository)) {
                    if (cat.getCategoryName().equals(category)) {
                        if (cat.getNotes().contains(note)) {
                            cat.deleteNote(note, noteCategoryRepository);
                            return new ResponseEntity<>(output, HttpStatus.OK);
                        }
                    }
                }
            }
            if (mode.equals("delete_category")) {
                String category = node.get("category").asText();
                for (NoteCategory cat : NoteCategory.associatedWith(work, noteCategoryRepository)) {
                    if (cat.getCategoryName().equals(category)) {
                        //delete

                        for (String s : cat.getNotes()) {
                            cat.deleteNote(s, noteCategoryRepository);
                        }
                        File noteDir = new File(cat.findPath() + "/");
                        noteDir.delete();
                        File catRoot = new File(cat.findPath() + ".json");
                        catRoot.delete();
                        noteCategoryRepository.delete(cat);
                    }
                }
            }
            if (mode.equals("select")) {
                for (Chapter c : Chapter.associatedWith(work, chapterRepository)) {
                    if (c.toResource().equals(node.get("target").asText())) {
                        output.put("content", c.retrieveHTML());
                        output.put("notes", c.readNotes());
                        output.put("title", c.getTitle());
                        return new ResponseEntity<>(output, HttpStatus.OK);
                    }
                }
                output.put("error", "unrecognized_chapter");
                return new ResponseEntity<>(output, HttpStatus.OK);
            }
            if (mode.equals("save_chapter")) {
                String chapterTitle = node.get("target").asText();
                String content = node.get("content").asText();
                String notes = node.get("notes").asText();
                Chapter chapter = null;
                for (Chapter c : Chapter.associatedWith(work, chapterRepository)) {
                    if (c.getTitle().equals(chapterTitle)) {
                        chapter = c;
                    }
                }
                if (chapter != null) {
                    chapter.writeHTML(content);
                    chapter.writeNotes(notes);
                    return new ResponseEntity<>(output, HttpStatus.OK);
                }
            }



        } catch (Exception e) {
            System.out.println(e);
            output.put("error", e.getMessage());
            return new ResponseEntity<>(output, HttpStatus.OK);
        }
        return new ResponseEntity<>(output, HttpStatus.OK);
    }
}
