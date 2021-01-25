package epic.lms;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Service
public class FirebaseInitializer {

    @PostConstruct
   public void initialize() {
       try {
           FileInputStream serviceAccount = new FileInputStream("./src/main/java/epic/lms/dbjson.json"); // retrieve database login details

           String basePath = new File("").getAbsolutePath();
           System.out.println(basePath);
           // set webpage directory and initialize it
           FirebaseOptions options = new FirebaseOptions.Builder()
                   .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                   .setDatabaseUrl("https://epic-learning.firebaseio.com")
                   .build();
           FirebaseApp.initializeApp(options);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }




}
