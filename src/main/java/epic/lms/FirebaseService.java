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

}
