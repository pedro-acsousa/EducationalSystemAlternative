package epic.lms;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {
    public User getUser(String name) throws ExecutionException, InterruptedException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        List<User> userArrayList = new ArrayList<User>();
        DocumentReference documentReference = db.collection("Users").document("6rFpYIFzu5REnTqQkUph");
        ApiFuture<DocumentSnapshot> future = documentReference.get();
       DocumentSnapshot document = future.get();

       User user = null;

       if (document.exists()){
           user= document.toObject(User.class);
                   return user;
       } else {return null;}

    }

}
