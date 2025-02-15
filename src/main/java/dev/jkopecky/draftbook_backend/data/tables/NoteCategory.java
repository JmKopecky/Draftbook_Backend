package dev.jkopecky.draftbook_backend.data.tables;


import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.Log;
import jakarta.persistence.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Entity
public class NoteCategory {
    //  One noteCategory serves as a container for multiple individual notes
    //  Example: noteCategory 'Character Sheets' could contain individual notes:
    //          - Harry Potter
    //          - Hermione Granger
    //          - Ron Weasley
    //          - Neville Longbottom
    //  Each of these is represented by its own file, containing the actual content.

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @ElementCollection
    public List<String> notes;
    private String categoryName;
    @ManyToOne
    private Work work;





    public static NoteCategory create(NoteCategoryRepository noteCategoryRepository, Work work, String categoryName) throws IOException {

        //create category
        NoteCategory category = new NoteCategory();
        category.setWork(work);
        category.setNotes(new ArrayList<>());
        category.setCategoryName(categoryName);

        //create file of category data copy
        ObjectMapper mapper = new ObjectMapper();
        String path = category.findPath() + ".json";
        File rootFile = new File(path);

        if (rootFile.isFile()) { //file already exists, read data from it instead of creating a new one

            try { //attempt to retrieve preexisting data copy
                category = mapper.readValue(rootFile, NoteCategory.class);
            } catch (Exception e) { //failed to fully read data from file.
                try { //in this case, override the preexisting file with the default data.
                    Files.createDirectories(Paths.get(path));
                    mapper.writeValue(rootFile, category);
                } catch (IOException io) {
                    Log.create("Failed to create noteCategory file for data copy.", "NoteCategory.create()", "warn", e);
                    throw io;
                }
            }

            try { //since the category already exists, attempt to retrieve existing notes as well.
                refreshNotes(category);
            } catch (IOException e) {
                Log.create(e.getMessage(), "NoteCategory.create() {unspecified log 1}", "error", e);
                throw e;
            }

        } else { //file does not already exist, create a new one with the default data
            try {
                path = category.findPath();
                Files.createDirectories(Paths.get(path));
                path += ".json";
                rootFile = new File(path);
                mapper.writeValue(rootFile, category);
            } catch (IOException e) {
                Log.create(e.getMessage(), "NoteCategory.create() {unspecified log 2}", "error", e);
                throw e;
            }
        }

        noteCategoryRepository.save(category);
        return category;
    }



    public String findPath() {
        return work.getPath() + "notes/" + categoryName;
    }



    public static ArrayList<NoteCategory> getWorkNoteCategories(Work work, NoteCategoryRepository noteCategoryRepository) {
        ArrayList<NoteCategory> categories = new ArrayList<>();
        for (NoteCategory noteCategory : noteCategoryRepository.findAll()) {
            if (noteCategory.work.equals(work)) {
                categories.add(noteCategory);
            }
        }
        return categories;
    }



    public static void refreshNotes(NoteCategory category) throws IOException {
        //access existing notes
        String path = category.findPath() + "/";
        File noteDir = new File(path);
        if (noteDir.exists() && noteDir.isDirectory()) {
            //read all, replacing preexisting notes with new ones
            File[] files = noteDir.listFiles();
            if (files != null && files.length > 0) {
                category.getNotes().clear();
                for (File file : files) {
                    category.getNotes().add(file.getName().substring(0, file.getName().lastIndexOf(".")));
                }
            } else {
                //no notes exist, clear.
                category.notes.clear();
            }
        } else {
            //directory has not been created, create it.
            Files.createDirectories(Paths.get(path));
        }
    }



    public void renameNote(String oldNote, String newNote, NoteCategoryRepository noteCategoryRepository) throws FileAlreadyExistsException, FileNotFoundException {
        if (!this.getNotes().contains(oldNote)) { //make sure that it exists first.
            Log.create("Attempted to rename note that does not exist", "NoteCategory.renameNote()", "debug", null);
            throw new FileNotFoundException("Attempted to rename note that does not exist");
        }

        File oldFile = new File(this.findPath() + "/" + oldNote + ".txt");
        File newFile = new File(this.findPath() + "/" + newNote + ".txt");
        if (newFile.exists()) {
            String message = "file " + newNote + " already exists";
            Log.create(message, "NoteCategory.renameNote()", "debug", null);
            throw new FileAlreadyExistsException(message);
        }

        try {
            oldFile.renameTo(newFile);
        } catch (Exception e) {
            Log.create(e.getMessage(), "NoteCategory.renameNote()", "error", e);
            throw e;
        }

        this.getNotes().set(this.getNotes().indexOf(oldNote), newNote);
        noteCategoryRepository.save(this);
    }



    public void deleteNote(String target, NoteCategoryRepository noteCategoryRepository) throws IOException {
        if (!this.getNotes().contains(target)) { //make sure that it exists first
            String message = "Note " + target + " does not exist.";
            Log.create(message, "NoteCategory.deleteNote()", "debug", null);
            throw new FileNotFoundException(message);
        }

        File file = new File(findPath() + "/" + target + ".txt");
        if (!file.exists()) { //does not exist, remove it from notes list and do nothing.
            this.getNotes().remove(target);
            noteCategoryRepository.save(this);
            String message = "Note " + target + " does not exist.";
            Log.create(message, "NoteCategory.deleteNote()", "debug", null);
            throw new FileNotFoundException(message);
        }

        try {
            file.delete();
        } catch (Exception e) {
            Log.create(e.getMessage(), "NoteCategory.deleteNote() {unspecified log 1}", "error", null);
            throw e;
        }

        this.getNotes().remove(target);
        noteCategoryRepository.save(this);
    }



    public void addNote(String noteTitle, String content, NoteCategoryRepository repository) throws IOException {
        File noteFile = new File(this.findPath() + "/" + noteTitle + ".txt"); //define file target

        try {
            FileWriter fileWriter = new FileWriter(noteFile);
            fileWriter.write(content);
            fileWriter.close();
            notes.add(noteTitle);
            repository.save(this);
        } catch (IOException e) {
            Log.create(e.getMessage(), "NoteCategory.addNote()", "error", e);
            throw e;
        }
    }



    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }
}
