package dev.jkopecky.draftbook_backend.data.tables;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.DraftbookBackendApplication;
import dev.jkopecky.draftbook_backend.Log;
import dev.jkopecky.draftbook_backend.data.Util;
import jakarta.persistence.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

@Entity
public class Chapter implements Comparable<Chapter> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String title;
    private Integer number;
    @ManyToOne
    private Work work;
    private String path;



    @Override
    public int compareTo(Chapter o) {
        return this.number.compareTo(o.number);
    }



    public void buildPath() {
        path = work.getPath() + "chapters/";
    }



    public String retrieveAsHTML() throws IOException {
        try {
            String fullPath = path + "chapter_" + Util.toInternalResource(getTitle()) + ".txt";
            File file = new File(fullPath);
            return Files.readString(file.toPath());
        } catch (IOException e) {
            Log.create(e.getMessage(), "Chapter.retrieveAsHTML()", "error", e);
            throw e;
        }
    }



    public void writeHTML(String html) throws IOException {
        try {
            String fullPath = path + "chapter_" + Util.toInternalResource(getTitle()) + ".txt";
            File file = new File(fullPath);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(html);
            fileWriter.close();
        } catch (IOException e) { //if file does not exist or cannot be created, or if file cannot be written to
            Log.create(e.getMessage(), "Chapter.writeHTML()", "error", e);
            throw e;
        }
    }



    public void writeNotes(String notes) throws IOException {
        try {
            String fullPath = path + "note_" + Util.toInternalResource(getTitle()) + ".txt";
            File file = new File(fullPath);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(notes);
            fileWriter.close();
        } catch (IOException e) { //if file does not exist or cannot be created, or if file cannot be written to
            Log.create(e.getMessage(), "Chapter.writeNotes()", "error", e);
            throw e;
        }
    }



    public String readNotes() throws IOException {
        try {
            String fullPath = path + "note_" + Util.toInternalResource(getTitle()) + ".txt";
            File file = new File(fullPath);
            return Files.readString(file.toPath());
        } catch (IOException e) {
            Log.create(e.getMessage(), "Chapter.readNotes()", "error", e);
            throw e;
        }
    }



    public String toResource() {
        return Util.toInternalResource(getTitle());
    }



    public boolean delete(ChapterRepository chapterRepository) {
        File backupFile = new File(path + "chapter_" + Util.toInternalResource(title) + ".json");
        File htmlFile = new File(path + "chapter_" + Util.toInternalResource(title) + ".txt");
        File noteFile = new File(path + "note_" + Util.toInternalResource(title) + ".txt");
        try {
            backupFile.delete();
            htmlFile.delete();
            noteFile.delete();
            chapterRepository.delete(this);
        } catch (Exception e) {
            Log.create(e.getMessage(), "Chapter.delete()", "error", e);
            return false;
        }
        return true;
    }



    public boolean rename(String name, ChapterRepository chapterRepository) {
        File newHtmlFile = new File(path + "chapter_" + Util.toInternalResource(name) + ".txt");
        File newNoteFile = new File(path + "note_" + Util.toInternalResource(name) + ".txt");

        //check if this name is already in use
        if (newHtmlFile.isFile() || newNoteFile.isFile() || name.equals(this.title)) {
            // title already in use by either this or another chapter, do not change.
            Log.create("Attempted to rename chapter " + this.title + " to a name " + name + " that already exists", "Chapter.rename()", "info", null);
            return false;
        }

        //transfer content to new files
        try {
            File oldHtmlFile = new File(path + "note_" + Util.toInternalResource(getTitle()) + ".txt");
            FileWriter fileWriter = new FileWriter(newHtmlFile);
            fileWriter.write(Files.readString(oldHtmlFile.toPath()));
            fileWriter.close();
        } catch (IOException e) {
            Log.create("Error while transferring HTML content to new file", "Chapter.rename()", "info", null);
            newHtmlFile.delete();
            return false;
        }

        try {
            File oldNotesFile = new File(path + "chapter_" + Util.toInternalResource(getTitle()) + ".txt");
            FileWriter fileWriter = new FileWriter(newNoteFile);
            fileWriter.write(Files.readString(oldNotesFile.toPath()));
            fileWriter.close();
        } catch (IOException e) {
            Log.create("Error while transferring note content to new file", "Chapter.rename()", "info", null);
            newHtmlFile.delete();
            newNoteFile.delete();
            return false;
        }

        String oldTitle = title;
        title = name;

        //write to data backup file
        ObjectMapper mapper = new ObjectMapper();
        File newDataBackup = new File(path + "chapter_" + Util.toInternalResource(name) + ".json");
        try {
            mapper.writeValue(newDataBackup, this);
            File oldDataBackup = new File(path + "chapter_" + Util.toInternalResource(oldTitle) + ".json");
            oldDataBackup.delete();
        } catch (IOException e) {
            Log.create("Error while transferring backup content to new file", "Chapter.rename()", "info", null);
            newHtmlFile.delete();
            newNoteFile.delete();
            newDataBackup.delete();
            title = oldTitle;
            return false;
        }

        chapterRepository.save(this);

        return true;
    }



    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Work getWork() {
        return work;
    }
    public void setWork(Work work) {
        this.work = work;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }


}
