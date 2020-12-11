package epic.lms;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FirebaseService {
    public List<User> getUser() throws ExecutionException, InterruptedException, IOException {

        Firestore db = FirestoreClient.getFirestore();
        List<User> userArrayList = new ArrayList<User>();

        CollectionReference collectionReference = db.collection("Users");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            userArrayList.add(doc.toObject(User.class));
        }
        return userArrayList;
    }

    public User addUser(User userAdded) throws ExecutionException, InterruptedException, IOException {

        Firestore db = FirestoreClient.getFirestore();


        Map<String,String> newUser = new HashMap<String,String>();
        newUser.put("username", userAdded.getUsername());
        newUser.put("password", userAdded.getPassword());
        newUser.put("firstname", userAdded.getFirstname());
        newUser.put("lastname", userAdded.getLastname());
        newUser.put("email", userAdded.getEmail());
        newUser.put("role", userAdded.getRole());

        db.collection("Users").document(userAdded.getUsername()).create(newUser);

        return userAdded;

    }

}
