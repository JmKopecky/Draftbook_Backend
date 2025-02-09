package dev.jkopecky.draftbook_backend.data.tables;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.ArrayList;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String username;
    private String password;


    public static Account getByUsername(String username, AccountRepository repository) {
        for (Account account : repository.findAll()) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }



    public static Account create(String username, String password, AccountRepository repo) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(password);
        repo.save(account);
        return account;
    }



    public static Account authenticate(String username, String password, AccountRepository repo) {
        for (Account account : repo.findAll()) {
            if (account.getUsername().equals(username) && account.getPassword().equals(password)) { //todo secure authentication
                return account;
            }
        }
        return null;
    }



    public static boolean exists(String username, AccountRepository repo) {
        for (Account account : repo.findAll()) {
            if (account.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }



    public ArrayList<Work> getOwnedWorks(WorkRepository repo) {
        ArrayList<Work> works = new ArrayList<>();
        for (Work work : repo.findAll()) {
            if (work.getAccount().equals(this)) {
                works.add(work);
            }
        }
        return works;
    }



    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
