package dev.jkopecky.draftbook_backend.data.tables;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.draftbook_backend.DraftbookBackendApplication;
import dev.jkopecky.draftbook_backend.Log;
import dev.jkopecky.draftbook_backend.data.Util;
import jakarta.persistence.*;

import javax.naming.NameAlreadyBoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

@Entity
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String title;
    private String path;
    @ManyToOne
    private Account account;


    public static void createWork(Account owner, String title, WorkRepository workRepository) throws IOException {
        Work work = new Work();
        work.setTitle(title);
        work.setAccount(owner);
        work.buildPath();
        work.createWorkFile();
        workRepository.save(work);
    }



    public void buildPath() {
        //directory should be the user's default root, in a folder based on their name. Changing the username or work name should force a refactor after checking.
        path = DraftbookBackendApplication.retrieveRoot() + Util.toInternalResource(account.getUsername()) + "/works/" + Util.toInternalResource(title) + "/";
    }



    public void createWorkFile() throws IOException {
        try {
            //create initial file based on title, and save the work object for data redundancy.
            ObjectMapper mapper = new ObjectMapper();
            String fullPath = path + Util.toInternalResource(title) + ".json";
            Files.createDirectories(Paths.get(path));
            File file = new File(fullPath);
            mapper.writeValue(file, this);
        } catch (IOException e) {
            Log.create(e.getMessage(), "Work.createWorkFile()", "error", e);
            throw e;
        }
    }



    public void createChapter(String title, int index, ChapterRepository chapterRepository) throws IOException {
        //retrieve all chapters and determine the location to add the new one.
        ArrayList<Chapter> chapters = getChapters(chapterRepository);

        if (index > chapters.size()) { //if index is too large, clamp to adding a chapter to the end
            index = chapters.size() + 1;
        }

        //create initial chapter object
        Chapter chapter = new Chapter();
        chapter.setTitle(title);
        chapter.setWork(this);
        chapter.setNumber(index);
        chapter.buildPath();

        //check if chapter is unique
        for (Chapter c : chapters) {
            if (c.getPath().equals(chapter.getPath())) {
                //a chapter already exists under the same name.
                String message = "A chapter (" + c.getTitle() + ") already exists under the same internal name.";
                message += "\n\t - Work: " + this.getTitle();
                message += "\n\t - Account: " + this.getAccount().getUsername();
                Log.create(message, "Work.createChapter()", "info", null);
                throw new FileAlreadyExistsException(message);
            }
        }

        //create files for chapters
        ObjectMapper mapper = new ObjectMapper();

        //save chapter object file
        try {
            String path = chapter.getPath() + "chapter_" + Util.toInternalResource(chapter.getTitle()) + ".json";
            Files.createDirectories(Paths.get(chapter.getPath()));
            File file = new File(path);
            mapper.writeValue(file, chapter);
        } catch (IOException e) {
            String message = "Failed to create chapter data copy.";
            String source = "Work.createChapter()";
            Log.create(message, source, "warn", null);
            throw new IOException("Warn in " + source + ": " + message);
        }

        //create file for the chapter's body
        try {
            String path = chapter.getPath() + "chapter_" + Util.toInternalResource(chapter.getTitle()) + ".txt";
            Files.createDirectories(Paths.get(chapter.getPath()));
            File file = new File(path);
            file.createNewFile();
        } catch (IOException e) {
            String message = "Failed to create chapter content file.";
            String source = "Work.createChapter()";
            Log.create(message, source, "warn", null);
            throw new IOException("Warn in " + source + ": " + message);
        }

        //create file for chapter-specific notes
        try {
            String path = chapter.getPath() + "note_" + Util.toInternalResource(chapter.getTitle()) + ".txt";
            Files.createDirectories(Paths.get(chapter.getPath()));
            File file = new File(path);
            file.createNewFile();
        } catch (IOException e) {
            String message = "Failed to create chapter note file.";
            String source = "Work.createChapter()";
            Log.create(message, source, "warn", null);
            throw new IOException("Warn in " + source + ": " + message);
        }

        chapterRepository.save(chapter);

        //log success
        String chapterConfirmationLog = "Created new chapter: ";
        chapterConfirmationLog += "\n\t - Chapter: " + chapter.getTitle();
        chapterConfirmationLog += "\n\t - Work: " + this.getTitle();
        chapterConfirmationLog += "\n\t - Account: " + this.getAccount().getUsername();
        Log.create(chapterConfirmationLog, "Work.createChapter", "debug", null);
    }



    public ArrayList<Chapter> getChapters(ChapterRepository chapterRepository) {
        ArrayList<Chapter> output = new ArrayList<>();
        for (Chapter c : chapterRepository.findAll()) {
            if (this.getPath().equals(c.getWork().getPath())) { //if path matches, it is the same work.
                output.add(c);
            }
        }
        Collections.sort(output); //sort output in increasing order by number.
        return output;
    }



    public String toResource() {
        return Util.toInternalResource(title);
        //todo replace usages to rely on path preferably, or an internal name. Title can be changed.
    }



    public boolean changeName(String newName, WorkRepository workRepository) {
        //todo implement
        return false;
    }



    public boolean delete(WorkRepository workRepository) {
        //todo implement
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
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public Account getAccount() {
        return account;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
}
