package dev.jkopecky.draftbook_backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jkopecky.writerwebtools.data.Util;
import dev.jkopecky.writerwebtools.data.tables.Account;
import dev.jkopecky.writerwebtools.data.tables.AccountRepository;
import dev.jkopecky.writerwebtools.data.tables.Work;
import dev.jkopecky.writerwebtools.data.tables.WorkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Controller
public class DashboardController {

    public AccountRepository accountRepository;
    public WorkRepository workRepository;
    public DashboardController(AccountRepository accountRepository, WorkRepository workRepository) {
        this.accountRepository = accountRepository;
        this.workRepository = workRepository;
    }


    @GetMapping("/dashboard")
    public String dashboard(Model model, @RequestParam(name = "section", required = false) String section, @CookieValue(value = "account", defaultValue = "null") String account) {

        if (!Account.exists(account, accountRepository)) { //todo improve signon
            return "redirect:/signon?mode=signin";
        }

        //add content
        if (section == null) {
            section = "dashboard";
        }
        model.addAttribute("section", section);
        model.addAttribute("accountname", account);

        Account currentAccount = Account.get(account, accountRepository);

        if (section.equals("works")) {
            ArrayList<Work> works = currentAccount.getWorks(workRepository);
            model.addAttribute("works", works);
        }

        return "dashboard";
    }




    @PostMapping("/dashboard")
    public ResponseEntity<String> dashboardPost(Model model, @RequestBody String data, @CookieValue(value = "account", defaultValue = "null") String account) {

        if (!Account.exists(account, accountRepository)) {
            return new ResponseEntity<>("Account not found", HttpStatus.NOT_FOUND);
        }

        String title = "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(data);
            String mode = node.get("mode").asText();
            if (mode.equals("create")) {
                title = node.get("title").asText();
                Account owner = Account.get(account, accountRepository); //todo authentication
                Work.createWork(owner, title, workRepository);
            }

        } catch (Exception e) {
            System.out.println(e);
            return new ResponseEntity<>("creation failure", HttpStatus.OK);
        }


        return new ResponseEntity<>(Util.toInternalResource(title), HttpStatus.OK);
    }
}
