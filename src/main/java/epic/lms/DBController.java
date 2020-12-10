package epic.lms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class DBController {
int found=0;
    @Autowired
    FirebaseService firebaseService;

    @PostMapping("/login")
    public void login(HttpServletResponse response, @RequestParam("login") String username, @RequestParam("password") String password) throws InterruptedException, ExecutionException, IOException, ScriptException, ServletException {
        found=0;
        List<User> users = firebaseService.getUser();
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                found=1;
                break;
            } else{ found=0;}
        }
        if (found==1){
            response.sendRedirect("userAccountPage.html");

        }else{
            response.sendRedirect("errorPage.html");
        }


    }


}
