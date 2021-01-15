package epic.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firestore.v1.Document;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.io.DataInput;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

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

    public User addUser(User userAdded){

        Firestore db = FirestoreClient.getFirestore();


        Map<String,String> newUser = new HashMap<String,String>();
        newUser.put("username", userAdded.getUsername());
        newUser.put("password", userAdded.getPassword());
        newUser.put("firstname", userAdded.getFirstname());
        newUser.put("lastname", userAdded.getSurname());
        newUser.put("email", userAdded.getEmail());
        newUser.put("role", userAdded.getRole());

        db.collection("Users").document(userAdded.getUsername()).create(newUser);

        return userAdded;

    }

    public List<Notifications> getAllNotifications(HttpSession session) throws ExecutionException, InterruptedException {

        Firestore db = FirestoreClient.getFirestore();

        List<Notifications> notifications = new ArrayList<>();
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (doc.get("recipient").equals(session.getAttribute("userid")) && (Boolean) doc.get("read")==false){
                notifications.add(doc.toObject(Notifications.class));
            }

        }

        return  notifications;
    }

    public List<Notifications> deleteNotification(List<Notifications> allNotifications, int id, HttpSession session) throws ExecutionException, InterruptedException {

        for (Notifications notification : allNotifications) {
            if (notification.getId() == id) {
                Firestore db = FirestoreClient.getFirestore();
                notification.setRead(true);
                db.collection("Notifications").document("" + notification.getId() + " " + notification.getRecipient()).update("read", notification.isRead());
                allNotifications.remove(notification);
                break;
            }
        }
    return allNotifications;
    }

    public String sendNotificationAll(String text, HttpSession session) throws ExecutionException, InterruptedException, IOException {

        List<User> users = getUser();
        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for(User user : users){
            id=id+1;
            Notifications newNotification = new Notifications();
            newNotification.setContent(text);
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
            String s = SDF.format(date);
            newNotification.setDate(s);



            newNotification.setRecipient(user.getUsername());
            newNotification.setSender(String.valueOf(session.getAttribute("userid")));
            newNotification.setId(id);
            newNotification.setRead(false);
            db.collection("Notifications").document("" + id + " " + user.getUsername()).create(newNotification);
        }
        return "Sent Successfully";
    }

    public String sendNotificationModule(String text, HttpSession session, String module) throws ExecutionException, InterruptedException, IOException {
        List<String> studentsModule;
        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        studentsModule=null;
        //Check Students in a module
        CollectionReference collectionReference1 = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot1 = collectionReference1.get();
        for(DocumentSnapshot doc1 : querySnapshot1.get().getDocuments()){
            if(doc1.getId().equals(module)){
               studentsModule= (List<String>) doc1.get("students");
            }
        }
        for (String student : studentsModule){
            id=id+1;
            Notifications newNotification = new Notifications();
            newNotification.setContent(text);
            Date date = Calendar.getInstance().getTime();
            SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
            String s = SDF.format(date);
            newNotification.setDate(s);
            newNotification.setRecipient(student);
            newNotification.setSender(String.valueOf(session.getAttribute("userid")));
            newNotification.setId(id);
            newNotification.setRead(false);
            db.collection("Notifications").document("" + id + " " + student).create(newNotification);


        }

    return "Sent Successfully";
    }

    public String sendNotificationStudent(String text, HttpSession session, String studentId) throws InterruptedException, ExecutionException, IOException {


        Firestore db = FirestoreClient.getFirestore();

        //increment id so id is unique
        CollectionReference collectionReference = db.collection("Notifications");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
        int id = 0;
        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()){
            if (Integer.parseInt(String.valueOf(doc.get("id"))) >= id){
                id = Integer.parseInt(String.valueOf(doc.get("id")));
            }
        }

        for (User student : getUser()){
            if (student.getUsername().equals(studentId)) {
                id = id + 1;
                Notifications newNotification = new Notifications();
                newNotification.setContent(text);
                Date date = Calendar.getInstance().getTime();
                SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");
                String s = SDF.format(date);
                newNotification.setDate(s);
                newNotification.setRecipient(studentId);
                newNotification.setSender(String.valueOf(session.getAttribute("userid")));
                newNotification.setId(id);
                newNotification.setRead(false);
                db.collection("Notifications").document("" + id + " " + studentId).create(newNotification);
            }

        }

        return "Sent Successfully";
    }



    public List<Modules> getModules() throws ExecutionException, InterruptedException {
        List<Modules> moduleList = new ArrayList<Modules>();

        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Modules");
        ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

        for(DocumentSnapshot doc : querySnapshot.get().getDocuments()) {
            moduleList.add(doc.toObject(Modules.class));
        }
        return  moduleList;
    }

    public Map<String,Map<String,Assessment>> getAssessments(Modules module) throws ExecutionException, InterruptedException, JSONException, IOException {
        List<Assessment> assessmentList = new ArrayList<Assessment>();
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference collectionReference = db.collection("Assignments");
        Map<String,Assessment> assessmentMap = new HashMap<>();
        Map<String,Map<String,Assessment>> studentModuleAssessmentMap = new HashMap<>();


        CollectionReference collection = db.collection("Assignments");
        for (DocumentReference document : collection.listDocuments()) {



            for (String student : module.getStudents()) {
                if (document.getId().equals(module.getId() + " : " + student))  {

                    DocumentReference docRef = db.collection("Assignments").document(module.getId() + " : " + student);
                    ApiFuture<DocumentSnapshot> future = docRef.get();
                    DocumentSnapshot doc = future.get();

                    Map<String, Object> assessmentMapObject;

                    assessmentMapObject=doc.getData();
                    for (Map.Entry<String, Object> pair : assessmentMapObject.entrySet()) {
                        final ObjectMapper mapper = new ObjectMapper();
                        final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);
                        assessmentMap.put(pair.getKey(), pojo);
                    }
                    studentModuleAssessmentMap.put(module.getId() + " : " + student, assessmentMap);
                }
            }

        }

        /*if (document.exists()) {
            Map<String, Object> assessmentMapObject = document.getData();
            for (Map.Entry<String, Object> pair : assessmentMapObject.entrySet()) {
                final ObjectMapper mapper = new ObjectMapper();
                final Assessment pojo = mapper.convertValue(pair.getValue(), Assessment.class);
                assessmentMap.put(pair.getKey(), pojo);
            }
        }
        */
        return  studentModuleAssessmentMap;
    }

    public void createAssessment(HttpSession session,String assessmentName, String dueDate, String module, String assessmentType, String spec) throws InterruptedException, ExecutionException, IOException {


        Firestore db = FirestoreClient.getFirestore();

        for(Modules moduleiter: getModules()){
            List<String> studentsInModule= moduleiter.getStudents();
            if (module.equals(moduleiter.getId())){


                Assessment assessmentObject = new Assessment();
                assessmentObject.setCreator((String) session.getAttribute("userid"));
                assessmentObject.setDue(dueDate);
                assessmentObject.setSubmitted(false);
                assessmentObject.setSpec(spec);
                assessmentObject.setType(assessmentType);



                //FALTA ADICIONAR OS ESTUDANTES COMO ARRAY AO ASSESSMENTDETAIL


                //creates Inner map
                Map<String, Assessment> newAssessmentDetail = new HashMap<String, Assessment>();
                newAssessmentDetail.put(assessmentName, assessmentObject);




                //Creates document before writing if non existent
                CollectionReference collectionReference = db.collection("Assignments");
                ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();

                for(DocumentSnapshot doc : querySnapshot.get().getDocuments()) {

                        for(String user: studentsInModule){
                            if (!doc.getId().equals(module + " : "+user)) {
                                db.collection("Assignments").document(module + " : " + user).create(newAssessmentDetail);
                            }else{
                        db.collection("Assignments").document(module+ " : " + user).set(newAssessmentDetail,SetOptions.merge());
                        }
                    }
                }

            }
        }



    }

    public void getAssessmentsinModule(HttpSession session) throws ExecutionException, InterruptedException, JSONException, IOException {

        List<Modules> moduleList = (List<Modules>) session.getAttribute("moduleList");
        Map<String, Map<String,Assessment>> assessmentsInModule = new HashMap<>();

        Map<String , String> mapForMarking= new HashMap<>();

        for (Modules modules : moduleList) {
            //for(Map<String,Assessment> extractMap : assessmentsInModule.get(modules.getId())){

            Firestore db = FirestoreClient.getFirestore();
            CollectionReference collectionReference = db.collection("Assignments");
            ApiFuture<QuerySnapshot> querySnapshot = collectionReference.get();
            assessmentsInModule = getAssessments(modules);

            for (DocumentSnapshot doc : querySnapshot.get().getDocuments()) {

                if (doc.getId().equals(modules.getId())) {



                    for (Map<String, Assessment> value : assessmentsInModule) {

                        for (Map.Entry<String, Assessment> entry : value.entrySet()) {
                            mapForMarking.put(modules.getId(), entry.getKey());
                        }

                    }
                }
            }


        }
        session.setAttribute("AssignmentsInModule",mapForMarking);
        System.out.println(mapForMarking);
    }




}
