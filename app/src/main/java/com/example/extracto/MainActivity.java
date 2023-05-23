package com.example.extracto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final int STORAGE_PERMISSION_REQUEST = 2;

    private Button btnUpload;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUpload = findViewById(R.id.btnUpload);
        tvResult = findViewById(R.id.tvResult);

        btnUpload.setOnClickListener(v -> checkStoragePermission());
    }

    private void checkStoragePermission() {
        // Check if storage permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_REQUEST);
        }
    }

    @SuppressWarnings("deprecation")
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_PDF_REQUEST);
    }

    @SuppressWarnings("deprecation")
    private void extractInformationFromPdf(Uri pdfUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            PdfReader reader = new PdfReader(inputStream);

            StringBuilder extractedText = new StringBuilder();
            int numPages = reader.getNumberOfPages();
            for (int i = 1; i <= numPages; i++) {
                extractedText.append(extractDataFromPage(reader, i));
            }

            reader.close();

            // Set the extracted text to the TextView
            tvResult.setText(extractedText.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private String extractDataFromPage(PdfReader reader, int pageNum) {
        StringBuilder pageData = new StringBuilder();

        try {
            // Extract text from the specified page
            String pageText = PdfTextExtractor.getTextFromPage(reader, pageNum);

            // Process the extracted text
            String[] lines = pageText.split("\n");

            String name = "";
            String rollNumber = "";
            String marks = "";

            for (String line : lines) {
                // Extract the name and roll number
                if (line.contains("Name:")) {
                    name = line.substring(line.indexOf("Name:") + 5).trim();
                } else if (line.contains("Roll Number:")) {
                    rollNumber = line.substring(line.indexOf("Roll Number:") + 12).trim();
                } else if (line.contains("Subject:") && line.contains("Marks:")) {
                    // Extract subject-wise marks
                    String subject = line.substring(line.indexOf("Subject:") + 8, line.indexOf("Marks:")).trim();
                    String mark = line.substring(line.indexOf("Marks:") + 6).trim();
                    marks += subject + ": " + mark + "\n";
                }
            }

            // Append the extracted data to the pageData StringBuilder
            pageData.append("Page ").append(pageNum).append(":\n");
            pageData.append("Name: ").append(name).append("\n");
            pageData.append("Roll Number: ").append(rollNumber).append("\n");
            pageData.append("Marks:\n").append(marks).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred while extracting data from page " + pageNum, Toast.LENGTH_SHORT).show();
        }

        return pageData.toString();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri pdfUri = data.getData();
            extractInformationFromPdf(pdfUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}