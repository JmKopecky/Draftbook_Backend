package dev.jkopecky.draftbook_backend.data.tables;

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
        path = DraftbookBackendApplication.retrieveRoot() + Util.toInternalResource(work.getAccount().getUsername()) + "/works/" + Util.toInternalResource(work.getTitle()) + "/chapters/";
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
        //todo implement;
        return false;
    }



    public boolean rename(String name, ChapterRepository chapterRepository) {
        //todo implement;
        return false;
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
