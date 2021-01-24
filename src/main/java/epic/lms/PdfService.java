package epic.lms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;



@Service
public class PdfService {

    @Autowired
    FirebaseService firebaseService;



        public InputStream createPdf(String module, String student, String assessment) throws Exception {

            Firestore db  = FirestoreClient.getFirestore();
            Assessment currentAssessment = new Assessment();


            Map<String,Map<String, Assessment>> assessmentStudent = new HashMap<>();
            assessmentStudent = firebaseService.getAssessments(module, student);

            //access the accessmentStudent here and add fields to the document and format doc
            for (Map.Entry<String, Map<String, Assessment>> outterMap : assessmentStudent.entrySet()) {
                for (Map.Entry<String, Assessment> assessmentinModule : outterMap.getValue().entrySet()) {
                    if (assessmentinModule.getKey().equals(assessment)){
                        currentAssessment = assessmentinModule.getValue();
                    }


                }
            }


            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();


            String imageFile ="https://firebasestorage.googleapis.com/v0/b/epic-learning.appspot.com/o/Epic.png?alt=media&token=35b02152-c301-4627-8283-f1e70e258f79";
            Image img = Image.getInstance(imageFile);

            img.setAlignment(1);
            img.scaleToFit(70,70);

            document.add(img);


            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            Paragraph p = new Paragraph("Student Assignment report ("+ dtf.format(now) +")" );
            p.setAlignment(Element.ALIGN_CENTER);

            document.add(p);



            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Student: " + student));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Assignment creator: " + currentAssessment.getCreator()));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Due: " + currentAssessment.getDue()));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph("Assessment type: " + currentAssessment.getType()));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));


            //SE O ASSESSMENT JA FOI SUBMETIDO ELSE ADICIONAR
            if("false".equals(String.valueOf(currentAssessment.isSubmitted()))){
                Font f=new Font(Font.FontFamily.HELVETICA,15.0f,Font.UNDERLINE+ Font.BOLD,BaseColor.RED);
                Paragraph p1 = new Paragraph("This assignment was not submitted yet!",f);
                p1.setAlignment(Element.ALIGN_CENTER);
                document.add(p1);
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));

            } else if("true".equals(String.valueOf(currentAssessment.isSubmitted()))) {
                Font f = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE + Font.BOLD, BaseColor.GREEN);
                Paragraph p1 = new Paragraph("This assignment was submitted. Submission details:", f);
                p1.setAlignment(Element.ALIGN_CENTER);
                document.add(p1);
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph("Date of submission: " + currentAssessment.getDatesubmitted()));
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph("Submission URL: " + currentAssessment.getFileUrl()));
                document.add(new Paragraph(Chunk.NEWLINE));
                document.add(new Paragraph(Chunk.NEWLINE));

                if(currentAssessment.getMark()==null) {
                    Font f1 = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE+ Font.BOLD, BaseColor.ORANGE);
                    Paragraph p2 = new Paragraph("--This assignment is awaiting grading--", f1);
                    p2.setAlignment(Element.ALIGN_CENTER);
                    document.add(p2);
                    document.add(new Paragraph(Chunk.NEWLINE));
                    document.add(new Paragraph(Chunk.NEWLINE));
                } else {
                    Font f2 = new Font(Font.FontFamily.HELVETICA, 15.0f, Font.UNDERLINE+ Font.BOLD, BaseColor.GREEN);
                    Paragraph p3 = new Paragraph("--This assignment has been graded--", f2);
                    p3.setAlignment(Element.ALIGN_CENTER);
                    document.add(p3);
                    document.add(new Paragraph(Chunk.NEWLINE));
                    document.add(new Paragraph(Chunk.NEWLINE));
                    document.add(new Paragraph("Grade attributed :" + currentAssessment.getMark()));
                    document.add(new Paragraph(Chunk.NEWLINE));
                    document.add(new Paragraph("Additional marking feedback: " + currentAssessment.getFeedback()));
                    document.add(new Paragraph(Chunk.NEWLINE));
                    document.add(new Paragraph(Chunk.NEWLINE));

                }
            }

            Paragraph p4 = new Paragraph("--End of Report--");
            p4.setAlignment(Element.ALIGN_CENTER);
            document.add(p4);
            document.close();

            return new ByteArrayInputStream(out.toByteArray());
        }



    @Controller
    @RequestMapping("/admin/generate_pdf")
    public class PdfController {

        @Autowired
        private PdfService pdfService;

        @GetMapping("/online")
        public void generatePdf(HttpServletResponse response, @RequestParam("Class") String module, @RequestParam("Student") String student, @RequestParam("Assessment") String assessment) throws Exception {
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"document.pdf\"");

            InputStream pdf = pdfService.createPdf(module, student, assessment);
            org.apache.commons.io.IOUtils.copy(pdf, response.getOutputStream());
            response.flushBuffer();
        }
    }

}