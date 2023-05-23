import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.extracto.R;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;


public class MainActivity extends AppCompatActivity {

    private Button btnUpload;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        tvResult = findViewById(R.id.tvResult);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    private void extractInformationFromPdf(Uri pdfUri) {
        try {
            PdfReader reader = new PdfReader(getContentResolver().openInputStream(pdfUri));
            PdfDocument document = new PdfDocument(reader);

            StringBuilder extractedText = new StringBuilder();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                TextExtractionStrategy strategy = new LocationTextExtractionStrategy();
                extractedText.append(PdfTextExtractor.getTextFromPage(document.getPage(pageNumber), strategy));
                extractedText.append("\n");
            }

            document.close();

            // Regular expressions to match the desired information
            String rollNumberRegex = "Roll Number: (\\d+)";
            String nameRegex = "Name: ([A-Za-z]+)";
            String subjectNameRegex = "Subject: ([A-Za-z ]+)";
            String marksRegex = "Marks: (\\d+)";

            // Extract the relevant information
            String extractedTextString = extractedText.toString();
            String rollNumber = extractInfoFromText(rollNumberRegex, extractedTextString);
            String name = extractInfoFromText(nameRegex, extractedTextString);
            String subjectName = extractInfoFromText(subjectNameRegex, extractedTextString);
            String marks = extractInfoFromText(marksRegex, extractedTextString);

            // Set the extracted information to TextView
            String result = "Roll Number: " + rollNumber + "\n"
                    + "Name: " + name + "\n"
                    + "Subject Name: " + subjectName + "\n"
                    + "Marks: " + marks;

            tvResult.setText(result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractInfoFromText(String regex, String text) {
        String extractedInfo = "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            extractedInfo = matcher.group(1);
        }
        return extractedInfo;
    }

    private static final int PICK_PDF_REQUEST = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            extractInformationFromPdf(pdfUri);
        }
    }
}
